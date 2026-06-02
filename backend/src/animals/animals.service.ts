import { Injectable, NotFoundException, ForbiddenException } from '@nestjs/common';
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

    const birthDate = dto.birthDate ? new Date(dto.birthDate) : new Date();
    const credentialUsername = dto.name.toLowerCase().replace(/\s+/g, '_');
    const credentialPassword = `${dto.name}${birthDate.toISOString().split('T')[0]}`;

    return this.prisma.animal.create({
      data: {
        name: dto.name,
        species: dto.species,
        breed: dto.breed,
        age: dto.age,
        description: dto.description,
        photoUrl: dto.photoUrl,
        status: dto.status ?? 'AVAILABLE',
        credentialUsername,
        credentialPassword,
        shelterId: shelter.id,
        originalShelterId: shelter.id,
        card: { create: { birthDate } }
      },
      include: { card: true }
    });
  }

  async updateAnimalStatus(userId: string, animalId: string, status: string) {
    const shelter = await this.prisma.shelterProfile.findUnique({ where: { userId } });
    const animal = await this.prisma.animal.findUnique({ where: { id: animalId } });
    if (!animal) throw new NotFoundException('Animal no encontrado');
    if (!shelter || animal.shelterId !== shelter.id) throw new ForbiddenException('Sin permisos');
    return this.prisma.animal.update({ where: { id: animalId }, data: { status: status as any } });
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
     return this.prisma.animalCard.upsert({
       where: { animalId },
       update: dto,
       create: { animalId, ...dto }
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