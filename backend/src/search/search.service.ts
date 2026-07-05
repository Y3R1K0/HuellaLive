import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class SearchService {
  constructor(private prisma: PrismaService) {}

  async search(query: string, species?: string, city?: string, status?: string) {
    const animals = await this.prisma.animal.findMany({
      where: {
        AND: [
          query ? {
            OR: [
              { name: { contains: query, mode: 'insensitive' } },
              { species: { contains: query, mode: 'insensitive' } },
              { breed: { contains: query, mode: 'insensitive' } }
            ]
          } : {},
          species ? { species: { contains: species, mode: 'insensitive' } } : {},
          status ? { status: status as any } : {},
          city ? {
            shelter: { location: { contains: city, mode: 'insensitive' } }
          } : {},
          { status: { not: 'ADOPTED' } }
        ]
      },
      include: {
        shelter: {
          include: { user: { select: { id: true, name: true, avatarUrl: true } } }
        }
      },
      take: 30
    });

    const shelters = await this.prisma.shelterProfile.findMany({
      where: {
        AND: [
          { status: 'APPROVED' },
          query ? {
            OR: [
              { user: { name: { contains: query, mode: 'insensitive' } } },
              { description: { contains: query, mode: 'insensitive' } },
              { location: { contains: query, mode: 'insensitive' } }
            ]
          } : {},
          city ? { location: { contains: city, mode: 'insensitive' } } : {}
        ]
      },
      include: {
        user: { select: { id: true, name: true, avatarUrl: true } },
        _count: { select: { animals: true } }
      },
      take: 20
    });

    return { animals, shelters };
  }

  async getWeeklyRanking() {
    const weekStart = new Date();
    weekStart.setDate(weekStart.getDate() - weekStart.getDay());
    weekStart.setHours(0, 0, 0, 0);

    const donations = await this.prisma.donation.groupBy({
      by: ['humanId'],
      where: {
        createdAt: { gte: weekStart },
        status: 'COMPLETED'
      },
      _sum: { amount: true },
      orderBy: { _sum: { amount: 'desc' } },
      take: 10
    });

    const results = await Promise.all(
      donations.map(async (d, index) => {
        const user = await this.prisma.user.findUnique({
          where: { id: d.humanId },
          select: {
            id: true, name: true, avatarUrl: true,
            badges: { include: { badge: true } },
            _count: { select: { follows: true, adoptedAnimals: true } }
          }
        });
        const sheltersHelped = await this.prisma.donation.groupBy({
          by: ['shelterId'],
          where: { humanId: d.humanId, status: 'COMPLETED' }
        });
        return {
          rank: index + 1,
          user,
          totalDonated: d._sum.amount ?? 0,
          sheltersHelped: sheltersHelped.length,
          animalsAdopted: user?._count.adoptedAnimals ?? 0
        };
      })
    );

    return results;
  }
}