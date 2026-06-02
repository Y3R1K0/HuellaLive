import { Injectable } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class FeedService {
  constructor(private prisma: PrismaService) {}

  async getFeed(page: number = 1, limit: number = 10) {
    const skip = (page - 1) * limit;
    return this.prisma.video.findMany({
      skip,
      take: limit,
      orderBy: { createdAt: 'desc' },
      include: {
        uploadedBy: {
          select: { id: true, name: true, avatarUrl: true, role: true }
        },
        animal: {
          select: {
            id: true, name: true, species: true,
            breed: true, status: true,
            shelter: {
              select: { id: true, user: { select: { name: true } } }
            }
          }
        },
      },
    });
  }
}