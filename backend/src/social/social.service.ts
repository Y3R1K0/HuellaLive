import { Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class SocialService {
  constructor(private prisma: PrismaService) {}

  async followShelter(humanId: string, shelterId: string) {
    await this.ensureShelter(shelterId);
    return this.prisma.follow.upsert({
      where: { humanId_shelterId: { humanId, shelterId } },
      update: {},
      create: { humanId, shelterId },
    });
  }

  async unfollowShelter(humanId: string, shelterId: string) {
    await this.ensureShelter(shelterId);
    await this.prisma.follow.deleteMany({ where: { humanId, shelterId } });
    return { ok: true };
  }

  private async ensureShelter(shelterId: string) {
    const shelter = await this.prisma.shelterProfile.findUnique({ where: { id: shelterId } });
    if (!shelter) throw new NotFoundException('Albergue no encontrado');
  }
}
