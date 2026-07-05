import { BadRequestException, ConflictException, Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class AdminService {
  constructor(private prisma: PrismaService) {}

  pendingShelters() {
    return this.prisma.shelterProfile.findMany({
      where: { status: 'PENDING' },
      orderBy: { createdAt: 'asc' },
      include: { user: { select: { id: true, name: true, email: true, avatarUrl: true } } },
    });
  }

  shelters() {
    return this.prisma.shelterProfile.findMany({
      orderBy: [{ status: 'asc' }, { createdAt: 'desc' }],
      include: {
        user: { select: { id: true, name: true, email: true, avatarUrl: true, isActive: true } },
        _count: { select: { animals: true } },
      },
    });
  }

  async updateShelter(id: string, dto: any) {
    const current = await this.prisma.shelterProfile.findUnique({
      where: { id },
      include: { user: true },
    });
    if (!current) throw new NotFoundException('Albergue no encontrado');

    const userData: any = {};
    if (dto.name !== undefined) userData.name = this.requiredText(dto.name, 'El nombre es obligatorio', 120);
    if (dto.avatarUrl !== undefined) userData.avatarUrl = this.optionalText(dto.avatarUrl);
    if (dto.isActive !== undefined) userData.isActive = Boolean(dto.isActive);
    if (dto.email !== undefined) {
      const email = this.requiredText(dto.email, 'El email es obligatorio', 160).toLowerCase();
      const duplicate = await this.prisma.user.findFirst({
        where: { email, id: { not: current.userId } },
        select: { id: true },
      });
      if (duplicate) throw new ConflictException('El email ya esta registrado');
      userData.email = email;
    }

    const shelterData: any = {};
    if (dto.description !== undefined) shelterData.description = this.optionalText(dto.description);
    if (dto.location !== undefined) shelterData.location = this.optionalText(dto.location);
    if (dto.phone !== undefined) shelterData.phone = this.optionalText(dto.phone);
    if (dto.coverUrl !== undefined) shelterData.coverUrl = this.optionalText(dto.coverUrl);
    if (dto.latitude !== undefined) shelterData.latitude = this.optionalNumber(dto.latitude, 'Latitud invalida');
    if (dto.longitude !== undefined) shelterData.longitude = this.optionalNumber(dto.longitude, 'Longitud invalida');
    if (dto.status !== undefined) shelterData.status = this.shelterStatus(dto.status);

    await this.prisma.$transaction(async (tx) => {
      if (Object.keys(userData).length > 0) {
        await tx.user.update({ where: { id: current.userId }, data: userData });
      }
      if (Object.keys(shelterData).length > 0) {
        await tx.shelterProfile.update({ where: { id }, data: shelterData });
      }
    });

    return this.prisma.shelterProfile.findUnique({
      where: { id },
      include: {
        user: { select: { id: true, name: true, email: true, avatarUrl: true, isActive: true } },
        _count: { select: { animals: true } },
      },
    });
  }

  async deleteShelter(id: string) {
    const shelter = await this.prisma.shelterProfile.findUnique({ where: { id } });
    if (!shelter) throw new NotFoundException('Albergue no encontrado');
    return this.updateShelter(id, { status: 'REJECTED', isActive: false });
  }

  async approveShelter(id: string) {
    const shelter = await this.prisma.shelterProfile.update({
      where: { id },
      data: { status: 'APPROVED' },
      include: { user: true },
    });
    await this.prisma.notification.create({
      data: {
        userId: shelter.userId,
        type: 'SHELTER_APPROVED',
        title: 'Albergue aprobado',
        body: 'Tu albergue ya puede subir videos y gestionar animales.',
        data: { shelterId: shelter.id },
      },
    });
    return shelter;
  }

  async rejectShelter(id: string) {
    const shelter = await this.prisma.shelterProfile.update({
      where: { id },
      data: { status: 'REJECTED' },
      include: { user: true },
    });
    await this.prisma.notification.create({
      data: {
        userId: shelter.userId,
        type: 'SHELTER_REJECTED',
        title: 'Verificacion rechazada',
        body: 'Revisa tus documentos y vuelve a intentarlo.',
        data: { shelterId: shelter.id },
      },
    });
    return shelter;
  }

  async assignBadge(adminId: string, userId: string, dto: any) {
    const user = await this.prisma.user.findUnique({ where: { id: userId } });
    if (!user) throw new NotFoundException('Usuario no encontrado');
    const badge = dto.badgeId
        ? await this.prisma.badge.findUnique({ where: { id: dto.badgeId } })
        : await this.prisma.badge.create({
          data: {
            key: dto.key ?? `ADMIN_${Date.now()}`,
            name: dto.name,
            description: dto.description,
            iconUrl: dto.iconUrl,
            tier: dto.tier ?? 'BRONZE',
            activity: dto.activity ?? 'SPECIAL',
            threshold: Number(dto.threshold ?? 0),
            isPublic: dto.isPublic ?? true,
          },
        });
    if (!badge) throw new NotFoundException('Insignia no encontrada');

    return this.prisma.userBadge.upsert({
      where: { userId_badgeId: { userId, badgeId: badge.id } },
      update: { assignedBy: adminId },
      create: { userId, badgeId: badge.id, assignedBy: adminId },
      include: { badge: true, user: { select: { id: true, name: true, avatarUrl: true } } },
    });
  }

  async stats() {
    const [users, sheltersPending, sheltersApproved, animals, adoptions, donations] = await Promise.all([
      this.prisma.user.count(),
      this.prisma.shelterProfile.count({ where: { status: 'PENDING' } }),
      this.prisma.shelterProfile.count({ where: { status: 'APPROVED' } }),
      this.prisma.animal.count(),
      this.prisma.animal.count({ where: { status: 'ADOPTED' } }),
      this.prisma.donation.aggregate({ where: { status: 'COMPLETED' }, _sum: { amount: true } }),
    ]);
    return {
      users,
      sheltersPending,
      sheltersApproved,
      animals,
      adoptions,
      completedDonationsTotal: donations._sum.amount ?? 0,
    };
  }

  searchCities() {
    return this.prisma.searchCity.findMany({
      orderBy: [{ sortOrder: 'asc' }, { name: 'asc' }],
    });
  }

  async createSearchCity(dto: any) {
    const name = this.requiredCityName(dto.name);
    const slug = this.citySlug(name);
    const existing = await this.prisma.searchCity.findUnique({ where: { slug } });
    if (existing) throw new ConflictException('La ciudad ya existe');

    return this.prisma.searchCity.create({
      data: {
        name,
        slug,
        region: this.optionalText(dto.region),
        isActive: dto.isActive ?? true,
        sortOrder: this.sortOrder(dto.sortOrder),
      },
    });
  }

  async updateSearchCity(id: string, dto: any) {
    const current = await this.prisma.searchCity.findUnique({ where: { id } });
    if (!current) throw new NotFoundException('Ciudad no encontrada');

    const name = dto.name === undefined ? current.name : this.requiredCityName(dto.name);
    const slug = this.citySlug(name);
    const duplicate = await this.prisma.searchCity.findFirst({
      where: { slug, id: { not: id } },
    });
    if (duplicate) throw new ConflictException('La ciudad ya existe');

    return this.prisma.searchCity.update({
      where: { id },
      data: {
        name,
        slug,
        region: dto.region === undefined ? current.region : this.optionalText(dto.region),
        isActive: dto.isActive === undefined ? current.isActive : Boolean(dto.isActive),
        sortOrder: dto.sortOrder === undefined ? current.sortOrder : this.sortOrder(dto.sortOrder),
      },
    });
  }

  async deleteSearchCity(id: string) {
    const city = await this.prisma.searchCity.findUnique({ where: { id } });
    if (!city) throw new NotFoundException('Ciudad no encontrada');
    await this.prisma.searchCity.delete({ where: { id } });
    return { deleted: true };
  }

  private requiredCityName(value: unknown) {
    const name = typeof value === 'string' ? value.trim() : '';
    if (!name) throw new BadRequestException('El nombre de la ciudad es obligatorio');
    if (name.length > 80) throw new BadRequestException('El nombre de la ciudad es demasiado largo');
    return name;
  }

  private optionalText(value: unknown) {
    const text = typeof value === 'string' ? value.trim() : '';
    return text || null;
  }

  private requiredText(value: unknown, message: string, maxLength: number) {
    const text = typeof value === 'string' ? value.trim() : '';
    if (!text) throw new BadRequestException(message);
    if (text.length > maxLength) throw new BadRequestException('El texto es demasiado largo');
    return text;
  }

  private optionalNumber(value: unknown, message: string) {
    if (value === null || value === '') return null;
    const parsed = Number(value);
    if (!Number.isFinite(parsed)) throw new BadRequestException(message);
    return parsed;
  }

  private shelterStatus(value: unknown) {
    const status = typeof value === 'string' ? value.trim().toUpperCase() : '';
    if (!['PENDING', 'APPROVED', 'REJECTED'].includes(status)) {
      throw new BadRequestException('Estado de albergue invalido');
    }
    return status;
  }

  private sortOrder(value: unknown) {
    const parsed = Number(value ?? 0);
    if (!Number.isInteger(parsed)) throw new BadRequestException('El orden debe ser un numero entero');
    return parsed;
  }

  private citySlug(value: string) {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-|-$/g, '');
  }

  searchSpecies() {
    return this.prisma.searchSpecies.findMany({
      orderBy: [{ sortOrder: 'asc' }, { name: 'asc' }],
    });
  }

  async createSearchSpecies(dto: any) {
    const name = this.requiredSpeciesName(dto.name);
    const slug = this.citySlug(name);
    const existing = await this.prisma.searchSpecies.findUnique({ where: { slug } });
    if (existing) throw new ConflictException('La especie ya existe');
    return this.prisma.searchSpecies.create({
      data: {
        name,
        slug,
        isActive: dto.isActive ?? true,
        sortOrder: this.sortOrder(dto.sortOrder),
      },
    });
  }

  async updateSearchSpecies(id: string, dto: any) {
    const current = await this.prisma.searchSpecies.findUnique({ where: { id } });
    if (!current) throw new NotFoundException('Especie no encontrada');
    const name = dto.name === undefined ? current.name : this.requiredSpeciesName(dto.name);
    const slug = this.citySlug(name);
    const duplicate = await this.prisma.searchSpecies.findFirst({
      where: { slug, id: { not: id } },
    });
    if (duplicate) throw new ConflictException('La especie ya existe');
    return this.prisma.searchSpecies.update({
      where: { id },
      data: {
        name,
        slug,
        isActive: dto.isActive === undefined ? current.isActive : Boolean(dto.isActive),
        sortOrder: dto.sortOrder === undefined ? current.sortOrder : this.sortOrder(dto.sortOrder),
      },
    });
  }

  async deleteSearchSpecies(id: string) {
    const species = await this.prisma.searchSpecies.findUnique({ where: { id } });
    if (!species) throw new NotFoundException('Especie no encontrada');
    const animalsUsingSpecies = await this.prisma.animal.count({
      where: { species: { equals: species.name, mode: 'insensitive' } },
    });
    if (animalsUsingSpecies > 0) {
      throw new BadRequestException('Esta especie tiene animales asociados. Desactivala en lugar de eliminarla');
    }
    await this.prisma.searchSpecies.delete({ where: { id } });
    return { deleted: true };
  }

  async speciesRequests() {
    const requests = await this.prisma.speciesRequest.findMany({
      where: { status: 'PENDING' },
      orderBy: { createdAt: 'asc' },
    });
    const [animals, shelters] = await Promise.all([
      this.prisma.animal.findMany({
        where: { id: { in: requests.map((item) => item.animalId) } },
        select: { id: true, name: true, species: true, photoUrl: true },
      }),
      this.prisma.shelterProfile.findMany({
        where: { id: { in: requests.map((item) => item.shelterId) } },
        select: { id: true, userId: true, user: { select: { name: true } } },
      }),
    ]);
    const animalsById = new Map(animals.map((animal) => [animal.id, animal]));
    const sheltersById = new Map(shelters.map((shelter) => [shelter.id, shelter]));
    return requests.map((request) => ({
      ...request,
      animal: animalsById.get(request.animalId) ?? null,
      shelter: sheltersById.get(request.shelterId) ?? null,
    }));
  }

  async approveSpeciesRequest(adminId: string, id: string) {
    const request = await this.prisma.speciesRequest.findUnique({ where: { id } });
    if (!request) throw new NotFoundException('Solicitud no encontrada');
    if (request.status !== 'PENDING') throw new BadRequestException('La solicitud ya fue revisada');

    const result = await this.prisma.$transaction(async (tx) => {
      const species = await tx.searchSpecies.upsert({
        where: { slug: request.normalizedName },
        update: { isActive: true },
        create: {
          name: request.requestedName,
          slug: request.normalizedName,
          isActive: true,
          sortOrder: 100,
        },
      });
      const updatedRequest = await tx.speciesRequest.update({
        where: { id },
        data: { status: 'APPROVED', reviewedById: adminId, reviewedAt: new Date() },
      });
      return { species, request: updatedRequest };
    });

    await this.notifySpeciesDecision(request, true, result.species.name);
    return result;
  }

  async rejectSpeciesRequest(adminId: string, id: string, reason?: string) {
    const request = await this.prisma.speciesRequest.findUnique({ where: { id } });
    if (!request) throw new NotFoundException('Solicitud no encontrada');
    if (request.status !== 'PENDING') throw new BadRequestException('La solicitud ya fue revisada');
    const updated = await this.prisma.speciesRequest.update({
      where: { id },
      data: {
        status: 'REJECTED',
        rejectionReason: this.optionalText(reason),
        reviewedById: adminId,
        reviewedAt: new Date(),
      },
    });
    await this.notifySpeciesDecision(request, false, request.requestedName, reason);
    return updated;
  }

  async reports() {
    return this.prisma.$queryRaw`
      select
        r.*,
        u.name as "reporterName",
        u.email as "reporterEmail",
        v."thumbnailUrl" as "videoThumbnailUrl",
        a.name as "animalName",
        a."photoUrl" as "animalPhotoUrl"
      from "reports" r
      join "users" u on u.id = r."reporterId"
      left join "videos" v on v.id = r."videoId"
      left join "animals" a on a.id = r."targetId" or a.id = v."animalId"
      order by
        case when r.status = 'PENDING' then 0 else 1 end,
        r."createdAt" desc
      limit 100
    `;
  }

  async updateReport(adminId: string, id: string, status: unknown) {
    const cleanStatus = typeof status === 'string' ? status.trim().toUpperCase() : '';
    const allowed = ['PENDING', 'REVIEWED', 'DISMISSED', 'ACTION_TAKEN'];
    if (!allowed.includes(cleanStatus)) {
      throw new BadRequestException('Estado de reporte invalido');
    }
    const rows = await this.prisma.$queryRaw<Array<{ id: string }>>`
      select id from "reports" where id = ${id} limit 1
    `;
    if (rows.length === 0) throw new NotFoundException('Reporte no encontrado');

    await this.prisma.$executeRaw`
      update "reports"
      set status = ${cleanStatus}, "reviewedAt" = now(), "reviewedBy" = ${adminId}
      where id = ${id}
    `;
    return { id, status: cleanStatus };
  }

  private async notifySpeciesDecision(
    request: { animalId: string; shelterId: string; requestedName: string },
    approved: boolean,
    speciesName: string,
    reason?: string,
  ) {
    const [shelter, animal] = await Promise.all([
      this.prisma.shelterProfile.findUnique({ where: { id: request.shelterId } }),
      this.prisma.animal.findUnique({ where: { id: request.animalId }, select: { name: true } }),
    ]);
    if (!shelter) return;
    await this.prisma.notification.create({
      data: {
        userId: shelter.userId,
        type: approved ? 'SPECIES_REQUEST_APPROVED' : 'SPECIES_REQUEST_REJECTED',
        title: approved ? 'Nueva especie aprobada' : 'Solicitud de especie rechazada',
        body: approved
          ? `${speciesName} ya esta disponible. Edita a ${animal?.name ?? 'tu animal'} y cambia su especie de Otro a ${speciesName}.`
          : `La solicitud ${speciesName} fue rechazada.${reason ? ` Motivo: ${reason}` : ''}`,
        data: { animalId: request.animalId, speciesName },
      },
    });
  }

  private requiredSpeciesName(value: unknown) {
    const name = typeof value === 'string' ? value.trim() : '';
    if (!name) throw new BadRequestException('El nombre de la especie es obligatorio');
    if (name.length > 80) throw new BadRequestException('El nombre de la especie es demasiado largo');
    return name;
  }
}
