import {
  Injectable,
  UnauthorizedException,
  ConflictException,
  NotFoundException,
  ForbiddenException,
  BadRequestException
} from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { ConfigService } from '@nestjs/config';
import { PrismaService } from '../prisma/prisma.service';
import {
  LoginDto,
  RegisterHumanDto,
  RegisterShelterDto,
  LinkAnimalDto
} from './dto/auth.dto';
import * as bcrypt from 'bcryptjs';
import { FirebaseIdentityService } from './firebase-identity.service';

@Injectable()
export class AuthService {
  constructor(
    private prisma: PrismaService,
    private jwtService: JwtService,
    private config: ConfigService,
    private firebaseIdentity: FirebaseIdentityService
  ) {}

  async registerHuman(dto: RegisterHumanDto) {
    const email = this.normalizeEmail(dto.email);
    const existing = await this.prisma.user.findUnique({
      where: { email }
    });
    if (existing) throw new ConflictException('El email ya está registrado');
    const passwordHash = await bcrypt.hash(dto.password, 12);
    let user = await this.prisma.user.create({
      data: { name: dto.name, email, passwordHash, role: 'HUMAN' }
    });
    user = await this.syncFirebasePassword(user, dto.password);
    return this.generateTokens(user);
  }

  async registerShelter(dto: RegisterShelterDto) {
    const email = this.normalizeEmail(dto.email);
    await this.ensureValidUniqueShelterGmail(email);
    const existing = await this.prisma.user.findUnique({
      where: { email }
    });
    if (existing) throw new ConflictException('El email ya está registrado');
    const passwordHash = await bcrypt.hash(dto.password, 12);
    let user = await this.prisma.user.create({
      data: {
        name: dto.name,
        email,
        passwordHash,
        role: 'SHELTER',
        shelterProfile: {
          create: {
            description: dto.description,
            location: dto.location,
            phone: dto.phone,
            verificationDocs: dto.verificationDocs,
            status: 'PENDING'
          }
        }
      },
      include: { shelterProfile: true }
    });
    user = await this.syncFirebasePassword(user, dto.password);
    return this.generateTokens(user);
  }

  async login(dto: LoginDto) {
    const email = this.normalizeEmail(dto.email);
    const password = dto.password.trim();
    let user = await this.prisma.user.findUnique({
      where: { email },
      include: { shelterProfile: true }
    });
    if (!user || !user.passwordHash)
      throw new UnauthorizedException('Email o contraseña incorrectos');
    if (!user.isActive)
      throw new ForbiddenException('Cuenta suspendida');
    const isValid = await bcrypt.compare(password, user.passwordHash);
    if (!isValid)
      throw new UnauthorizedException('Email o contraseña incorrectos');
    user = await this.syncFirebasePassword(user, password);
    return this.generateTokens(user);
  }

  async loginWithFirebase(idToken: string) {
    const identity = await this.firebaseIdentity.verifyIdToken(idToken);
    const email = identity.email?.trim().toLowerCase();
    if (!email || identity.email_verified === false) {
      throw new UnauthorizedException('Google no proporciono un correo verificado');
    }

    let user = await this.prisma.user.findFirst({
      where: {
        OR: [
          { firebaseUid: identity.uid },
          { email },
        ],
      },
      include: { shelterProfile: true },
    });

    if (!user) {
      user = await this.prisma.user.create({
        data: {
          email,
          name: identity.name?.trim() || email.substring(0, email.indexOf('@')),
          avatarUrl: identity.picture ?? null,
          firebaseUid: identity.uid,
          googleId: identity.firebase?.sign_in_provider === 'google.com' ? identity.uid : null,
          role: 'HUMAN',
        },
        include: { shelterProfile: true },
      });
    } else {
      if (!user.isActive) throw new ForbiddenException('Cuenta suspendida');
      if (user.role === 'SHELTER') {
        throw new ForbiddenException('Los albergues deben ingresar con correo y contraseña');
      }
      user = await this.prisma.user.update({
        where: { id: user.id },
        data: {
          firebaseUid: identity.uid,
          googleId: identity.firebase?.sign_in_provider === 'google.com'
            ? identity.uid
            : user.googleId,
          avatarUrl: user.avatarUrl ?? identity.picture ?? null,
        },
        include: { shelterProfile: true },
      });
    }

    return this.generateTokens(user);
  }

  async previewFirebaseIdentity(idToken: string) {
    const identity = await this.firebaseIdentity.verifyIdToken(idToken);
    const email = identity.email?.trim().toLowerCase();
    if (!email || identity.email_verified === false) {
      throw new UnauthorizedException('Google no proporciono un correo verificado');
    }

    return {
      email,
      name: identity.name?.trim() || email.substring(0, email.indexOf('@')),
      avatarUrl: identity.picture ?? null,
    };
  }

  async linkAnimal(userId: string, dto: LinkAnimalDto) {
    // Buscar animal por credenciales
    const animal = await this.prisma.animal.findUnique({
      where: { credentialUsername: dto.credentialUsername }
    });
    if (!animal)
      throw new NotFoundException('Animal no encontrado');
    if (animal.credentialPassword !== dto.credentialPassword)
      throw new UnauthorizedException('Credenciales incorrectas');
    if (animal.adoptedById)
      throw new ConflictException('Este animal ya fue vinculado');

    // Eliminar videos anteriores
    await this.prisma.video.deleteMany({
      where: { animalId: animal.id }
    });

    // Transferir animal al humano
    const updated = await this.prisma.animal.update({
      where: { id: animal.id },
      data: {
        adoptedById: userId,
        shelterId: null,
        status: 'ADOPTED'
      }
    });

    return updated;
  }

  private generateTokens(user: any) {
    const payload = { sub: user.id, email: user.email, role: user.role };
    return {
      accessToken: this.jwtService.sign(payload, {
        secret: this.config.get<string>('JWT_SECRET') as string,
        expiresIn: this.config.get('JWT_EXPIRES_IN'),
      }),
      refreshToken: this.jwtService.sign(payload, {
        secret: this.config.get<string>('JWT_REFRESH_SECRET') as string,
        expiresIn: this.config.get('JWT_REFRESH_EXPIRES_IN'),
      }),
      user: {
        id: user.id,
        name: user.name,
        email: user.email,
        role: user.role,
        avatarUrl: user.avatarUrl,
        shelterStatus: user.shelterProfile?.status ?? null,
      }
    };
  }

  private async syncFirebasePassword(
    user: { id: string; email: string; name: string; firebaseUid?: string | null },
    password: string
  ): Promise<any> {
    const firebaseUid = await this.firebaseIdentity.syncPasswordUser(user, password);
    if (!firebaseUid || firebaseUid === user.firebaseUid) return user;

    return this.prisma.user.update({
      where: { id: user.id },
      data: { firebaseUid },
      include: { shelterProfile: true },
    });
  }

  private normalizeEmail(email: string) {
    return email.trim().toLowerCase();
  }

  private canonicalGmail(email: string) {
    const [localPart, domainPart] = email.split('@');
    const domain = domainPart?.toLowerCase();
    if (!localPart || !domain || !['gmail.com', 'googlemail.com'].includes(domain)) {
      return null;
    }

    const local = localPart
      .split('+')[0]
      .replace(/\./g, '')
      .toLowerCase();

    return local ? `${local}@gmail.com` : null;
  }

  private async ensureValidUniqueShelterGmail(email: string) {
    const canonicalEmail = this.canonicalGmail(email);
    if (!canonicalEmail) {
      throw new BadRequestException('Usa un correo Gmail valido para solicitar registro de albergue');
    }

    const shelterUsers = await this.prisma.user.findMany({
      where: {
        role: 'SHELTER',
        OR: [
          { email: { endsWith: '@gmail.com' } },
          { email: { endsWith: '@googlemail.com' } },
        ],
      },
      select: { email: true },
    });

    const alreadyRequested = shelterUsers.some(
      (user) => this.canonicalGmail(user.email) === canonicalEmail,
    );

    if (alreadyRequested) {
      throw new ConflictException('Ya existe una solicitud de albergue con este Gmail');
    }
  }
}
