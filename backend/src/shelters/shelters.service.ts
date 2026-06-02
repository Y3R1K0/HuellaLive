import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class SheltersService {
  constructor(private prisma: PrismaService) {}

  async getShelterById(id: string) {
    const shelter = await this.prisma.shelterProfile.findUnique({
      where: { id },
      include: {
        user: { select: { id: true, name: true, avatarUrl: true } },
        animals: {
          where: { status: { not: 'ADOPTED' } },
          orderBy: { createdAt: 'desc' }
        },
        _count: { select: { animals: true } }
      }
    });
    if (!shelter) throw new NotFoundException('Albergue no encontrado');
    return shelter;
  }

  async getMyShelterProfile(userId: string) {
    const shelter = await this.prisma.shelterProfile.findUnique({
      where: { userId },
      include: {
        animals: { orderBy: { createdAt: 'desc' } },
        _count: { select: { animals: true } }
      }
    });
    if (!shelter) throw new NotFoundException('Perfil no encontrado');
    return shelter;
  }

  async updateShelterProfile(userId: string, dto: any) {
    return this.prisma.shelterProfile.update({
      where: { userId },
      data: dto
    });
  }

  async getNearbyShelters() {
    return this.prisma.shelterProfile.findMany({
      where: { status: 'APPROVED' },
      include: {
        user: { select: { id: true, name: true, avatarUrl: true } },
        _count: { select: { animals: true } }
      },
      take: 20
    });
  }
}