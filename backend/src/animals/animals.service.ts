import { BadRequestException, Injectable, NotFoundException, ForbiddenException } from '@nestjs/common';
import { randomUUID } from 'crypto';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class AnimalsService {
  constructor(private prisma: PrismaService) {}

  async getAnimalById(id: string) {
    const animal = await this.prisma.animal.findUnique({
      where: { id },
      include: {
        shelter: {
          include: { user: { select: { id: true, name: true, avatarUrl: true } } }
        },
        adoptedBy: { select: { id: true, name: true, avatarUrl: true } },
        card: true,
        videos: {
          orderBy: { createdAt: 'desc' },
          include: { uploadedBy: { select: { id: true, name: true, avatarUrl: true, role: true } } }
        },
        _count: { select: { videos: true } }
      }
    });
    if (!animal) throw new NotFoundException('Animal no encontrado');
    return animal;
  }

  async getMyAnimals(userId: string) {
    const shelter = await this.prisma.shelterProfile.findUnique({ where: { userId } });
    if (!shelter) throw new ForbiddenException('No tienes perfil de albergue');
    return this.prisma.animal.findMany({
      where: { shelterId: shelter.id },
      include: { card: true, _count: { select: { videos: true } } },
      orderBy: { createdAt: 'desc' }
    });
  }

  async getAdoptedAnimals(userId: string) {
    return this.prisma.animal.findMany({
      where: { adoptedById: userId },
      include: { card: true, _count: { select: { videos: true } } },
      orderBy: { createdAt: 'desc' }
    });
  }

  async createAnimal(userId: string, dto: any) {
    const shelter = await this.prisma.shelterProfile.findUnique({ where: { userId } });
    if (!shelter) throw new ForbiddenException('No tienes perfil de albergue');
    if (shelter.status !== 'APPROVED') throw new ForbiddenException('Tu albergue no está aprobado');

    const animalName = typeof dto.name === 'string' ? dto.name.trim() : '';
    if (!animalName) throw new BadRequestException('El nombre del animal es requerido');

    const allowedStatuses = ['AVAILABLE', 'RECOVERING', 'PREGNANT', 'ADOPTED', 'OTHER'];
    const status = typeof dto.status === 'string' && allowedStatuses.includes(dto.status)
      ? dto.status
      : 'AVAILABLE';

    const birthDate = dto.birthDate ? new Date(dto.birthDate) : new Date();
    if (Number.isNaN(birthDate.getTime())) {
      throw new BadRequestException('La fecha de nacimiento debe tener formato YYYY-MM-DD');
    }

    const credentialUsername = this.uniqueCredentialUsername(animalName);
    const credentialPassword = `${animalName}${birthDate.toISOString().split('T')[0]}`;
    const isOtherSpecies = this.slug(dto.species ?? '') === 'otro';
    const requestedSpeciesName = typeof dto.requestedSpeciesName === 'string'
      ? dto.requestedSpeciesName.trim()
      : '';

    if (isOtherSpecies && !requestedSpeciesName) {
      throw new BadRequestException('Escribe la nueva especie que deseas solicitar');
    }
    if (isOtherSpecies && requestedSpeciesName.length > 80) {
      throw new BadRequestException('El nombre de la especie es demasiado largo');
    }
    if (isOtherSpecies) {
      const requestedSlug = this.slug(requestedSpeciesName);
      if (!requestedSlug) throw new BadRequestException('Escribe un nombre de especie valido');
      const existingSpecies = await this.prisma.searchSpecies.findUnique({
        where: { slug: requestedSlug },
      });
      if (existingSpecies?.isActive) {
        throw new BadRequestException(`${existingSpecies.name} ya existe. Seleccionala en la lista`);
      }
    }

    let canonicalSpecies = 'Otro';
    if (!isOtherSpecies) {
      const species = await this.prisma.searchSpecies.findFirst({
        where: { slug: this.slug(dto.species ?? ''), isActive: true },
      });
      if (!species) throw new BadRequestException('Selecciona una especie valida');
      canonicalSpecies = species.name;
    }

    return this.prisma.$transaction(async (tx) => {
      const animal = await tx.animal.create({
        data: {
          name: animalName,
          species: canonicalSpecies,
          breed: dto.breed,
          age: dto.age,
          description: dto.description,
          photoUrl: dto.photoUrl,
          status,
          credentialUsername,
          credentialPassword,
          shelterId: shelter.id,
          originalShelterId: shelter.id,
          card: { create: { birthDate } }
        },
        include: { card: true }
      });

      if (isOtherSpecies) {
        await tx.speciesRequest.create({
          data: {
            requestedName: requestedSpeciesName,
            normalizedName: this.slug(requestedSpeciesName),
            animalId: animal.id,
            shelterId: shelter.id,
            requestedById: userId,
          },
        });
      }

      return animal;
    });
  }

  private slug(value: string) {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .trim()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-|-$/g, '');
  }

  private uniqueCredentialUsername(name: string) {
    const base = this.slug(name) || 'animal';
    return `${base}-${randomUUID().slice(0, 8)}`;
  }

  async updateAnimalStatus(userId: string, animalId: string, status: string) {
    const shelter = await this.prisma.shelterProfile.findUnique({ where: { userId } });
    const animal = await this.prisma.animal.findUnique({ where: { id: animalId } });
    if (!animal) throw new NotFoundException('Animal no encontrado');
    if (!shelter || animal.shelterId !== shelter.id) throw new ForbiddenException('Sin permisos');
    return this.prisma.animal.update({ where: { id: animalId }, data: { status: status as any } });
  }

  async updateAnimal(userId: string, animalId: string, dto: any) {
    const shelter = await this.prisma.shelterProfile.findUnique({ where: { userId } });
    const animal = await this.prisma.animal.findUnique({ where: { id: animalId } });
    if (!animal) throw new NotFoundException('Animal no encontrado');

    const isOwner = animal.adoptedById === userId;
    const isShelter = shelter && animal.shelterId === shelter.id;
    if (!isOwner && !isShelter) throw new ForbiddenException('Sin permisos');

    const data: any = {};
    if (typeof dto.name === 'string') data.name = dto.name;
    if (typeof dto.species === 'string') {
      if (this.slug(dto.species) === 'otro') {
        data.species = 'Otro';
      } else {
        const species = await this.prisma.searchSpecies.findFirst({
          where: { slug: this.slug(dto.species), isActive: true },
        });
        if (!species) throw new BadRequestException('Selecciona una especie valida');
        data.species = species.name;
      }
    }
    if (typeof dto.breed === 'string' || dto.breed === null) data.breed = dto.breed;
    if (typeof dto.age === 'number' || dto.age === null) data.age = dto.age;
    if (typeof dto.description === 'string' || dto.description === null) data.description = dto.description;
    if (typeof dto.photoUrl === 'string' || dto.photoUrl === null) data.photoUrl = dto.photoUrl;
    if (typeof dto.status === 'string') {
      if (isOwner && dto.status !== animal.status) {
        throw new ForbiddenException('El estado de adopcion no puede ser modificado por el adoptante');
      }
      data.status = dto.status as any;
    }

    return this.prisma.animal.update({
      where: { id: animalId },
      data,
      include: {
        shelter: {
          include: { user: { select: { id: true, name: true, avatarUrl: true } } }
        },
        adoptedBy: { select: { id: true, name: true, avatarUrl: true } },
        card: true,
        videos: {
          orderBy: { createdAt: 'desc' },
          include: { uploadedBy: { select: { id: true, name: true, avatarUrl: true, role: true } } }
        },
      }
    });
  }

  async getAnimalCredentials(userId: string, animalId: string) {
    const shelter = await this.prisma.shelterProfile.findUnique({ where: { userId } });
    const animal = await this.prisma.animal.findUnique({ where: { id: animalId } });
    if (!animal) throw new NotFoundException('Animal no encontrado');
    if (!shelter || animal.shelterId !== shelter.id) throw new ForbiddenException('Sin permisos');
    return {
      username: animal.credentialUsername,
      password: animal.credentialPassword
    };
  }

   async updateAnimalCard(userId: string, animalId: string, dto: any) {
     const shelter = await this.prisma.shelterProfile.findUnique({ where: { userId } });
     const animal = await this.prisma.animal.findUnique({ where: { id: animalId } });
     if (!animal) throw new NotFoundException('Animal no encontrado');
     const isOwner = animal.adoptedById === userId;
     const isShelter = shelter && animal.shelterId === shelter.id;
     if (!isOwner && !isShelter) throw new ForbiddenException('Sin permisos');

     const data = {
       sex: typeof dto.sex === 'string' && dto.sex.trim() ? dto.sex.trim() : null,
       birthDate: dto.birthDate ? new Date(dto.birthDate) : null,
       isSterilized: Boolean(dto.isSterilized),
       vaccines: Array.isArray(dto.vaccines) ? dto.vaccines : [],
       controls: Array.isArray(dto.controls) ? dto.controls : [],
       notes: typeof dto.notes === 'string' && dto.notes.trim() ? dto.notes.trim() : null,
     };
     if (data.birthDate && Number.isNaN(data.birthDate.getTime())) {
       throw new BadRequestException('La fecha de nacimiento no es valida');
     }

     return this.prisma.animalCard.upsert({
       where: { animalId },
       update: data,
       create: { animalId, ...data }
     });
   }

   async getShelterAdoptedAnimals(userId: string) {
     const shelter = await this.prisma.shelterProfile.findUnique({ where: { userId } });
     if (!shelter) throw new ForbiddenException('No tienes perfil de albergue');
     return this.prisma.animal.findMany({
       where: { originalShelterId: shelter.id, status: 'ADOPTED' },
       include: { adoptedBy: { select: { id: true, name: true, avatarUrl: true } } },
       orderBy: { createdAt: 'desc' }
     });
   }
}
