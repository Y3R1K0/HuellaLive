import { ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class UsersService {
  constructor(private prisma: PrismaService) {}

  async getMe(userId: string) {
    await this.syncActivityBadges(userId);
    const user = await this.prisma.user.findUnique({
      where: { id: userId },
      include: {
        shelterProfile: true,
        badges: { include: { badge: true } },
        adoptedAnimals: { include: { card: true } },
        _count: { select: { follows: true, donations: true } },
      },
    });
    if (!user) throw new NotFoundException('Usuario no encontrado');
    return user;
  }

  async updateMe(userId: string, dto: any) {
    const { selectedBadgeId: _ignored, badges: _badges, ...safeDto } = dto;
    return this.prisma.user.update({
      where: { id: userId },
      data: safeDto,
      select: { id: true, name: true, email: true, avatarUrl: true, role: true, selectedBadgeId: true },
    });
  }

  async selectBadge(userId: string, badgeId: string | null) {
    if (badgeId) {
      const owned = await this.prisma.userBadge.findUnique({
        where: { userId_badgeId: { userId, badgeId } },
      });
      if (!owned) throw new ForbiddenException('Solo puedes usar insignias desbloqueadas');
    }
    await this.prisma.user.update({ where: { id: userId }, data: { selectedBadgeId: badgeId } });
    return this.getMe(userId);
  }

  private async syncActivityBadges(userId: string) {
    const definitions = [
      ['FOLLOW_BRONZE', 'Rastro atento', 'Seguiste tu primer albergue.', 'BRONZE', 'FOLLOWS', 1],
      ['FOLLOW_SILVER', 'Explorador fiel', 'Sigues cinco albergues y ayudas a darles visibilidad.', 'SILVER', 'FOLLOWS', 5],
      ['FOLLOW_GOLD', 'Radar solidario', 'Sigues quince albergues activos.', 'GOLD', 'FOLLOWS', 15],
      ['FOLLOW_PLATINUM', 'Red de huellas', 'Sigues treinta albergues y mantienes viva la red.', 'PLATINUM', 'FOLLOWS', 30],
      ['DONATE_BRONZE', 'Primer impulso', 'Realizaste tu primera donacion.', 'BRONZE', 'DONATIONS', 1],
      ['DONATE_SILVER', 'Mano amiga', 'Realizaste tres donaciones.', 'SILVER', 'DONATIONS', 3],
      ['DONATE_GOLD', 'Corazon que sostiene', 'Realizaste diez donaciones.', 'GOLD', 'DONATIONS', 10],
      ['DONATE_DIAMOND', 'Motor de finales felices', 'Realizaste veinticinco donaciones.', 'DIAMOND', 'DONATIONS', 25],
      ['ADOPT_BRONZE', 'Final feliz', 'Completaste una adopcion.', 'BRONZE', 'ADOPTIONS', 1],
      ['ADOPT_SILVER', 'Familia que crece', 'Dos animales encontraron hogar contigo.', 'SILVER', 'ADOPTIONS', 2],
      ['ADOPT_GOLD', 'Hogar protector', 'Tres animales forman parte de tu familia.', 'GOLD', 'ADOPTIONS', 3],
      ['ADOPT_DIAMOND', 'Guardian de huellas', 'Cinco o mas animales encontraron hogar contigo.', 'DIAMOND', 'ADOPTIONS', 5],
    ] as const;

    await this.prisma.badge.createMany({
      data: definitions.map(([key, name, description, tier, activity, threshold]) => ({
        key,
        name,
        description,
        tier,
        activity,
        threshold,
        isPublic: true,
      })),
      skipDuplicates: true,
    });

    const [follows, donations, adoptions] = await Promise.all([
      this.prisma.follow.count({ where: { humanId: userId } }),
      this.prisma.donation.count({ where: { humanId: userId, status: 'APPROVED' } }),
      this.prisma.animal.count({ where: { adoptedById: userId, status: 'ADOPTED' } }),
    ]);
    const progress: Record<string, number> = {
      FOLLOWS: follows,
      DONATIONS: donations,
      ADOPTIONS: adoptions,
    };
    const badges = await this.prisma.badge.findMany({
      where: { key: { in: definitions.map((item) => item[0]) } },
    });
    const unlocked = badges.filter((badge) => (progress[badge.activity] ?? 0) >= badge.threshold);
    if (unlocked.length) {
      await this.prisma.userBadge.createMany({
        data: unlocked.map((badge) => ({ userId, badgeId: badge.id })),
        skipDuplicates: true,
      });
    }
  }
}
