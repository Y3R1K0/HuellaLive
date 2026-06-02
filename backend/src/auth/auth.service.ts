import {
  Injectable,
  UnauthorizedException,
  ConflictException,
  NotFoundException,
  ForbiddenException
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

@Injectable()
export class AuthService {
  constructor(
    private prisma: PrismaService,
    private jwtService: JwtService,
    private config: ConfigService
  ) {}

  async registerHuman(dto: RegisterHumanDto) {
    const existing = await this.prisma.user.findUnique({
      where: { email: dto.email }
    });
    if (existing) throw new ConflictException('El email ya está registrado');
    const passwordHash = await bcrypt.hash(dto.password, 12);
    const user = await this.prisma.user.create({
      data: { name: dto.name, email: dto.email, passwordHash, role: 'HUMAN' }
    });
    return this.generateTokens(user);
  }

  async registerShelter(dto: RegisterShelterDto) {
    const existing = await this.prisma.user.findUnique({
      where: { email: dto.email }
    });
    if (existing) throw new ConflictException('El email ya está registrado');
    const passwordHash = await bcrypt.hash(dto.password, 12);
    const user = await this.prisma.user.create({
      data: {
        name: dto.name,
        email: dto.email,
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
    return this.generateTokens(user);
  }

  async login(dto: LoginDto) {
    const user = await this.prisma.user.findUnique({
      where: { email: dto.email },
      include: { shelterProfile: true }
    });
    if (!user || !user.passwordHash)
      throw new UnauthorizedException('Email o contraseña incorrectos');
    if (!user.isActive)
      throw new ForbiddenException('Cuenta suspendida');
    const isValid = await bcrypt.compare(dto.password, user.passwordHash);
    if (!isValid)
      throw new UnauthorizedException('Email o contraseña incorrectos');
    return this.generateTokens(user);
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
}