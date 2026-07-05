import { Injectable } from '@nestjs/common';
import { Prisma } from '@prisma/client';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class ExploreService {
  constructor(private prisma: PrismaService) {}

  searchCities() {
    return this.prisma.searchCity.findMany({
      where: { isActive: true },
      orderBy: [{ sortOrder: 'asc' }, { name: 'asc' }],
      select: { id: true, name: true, region: true },
    });
  }

  searchSpecies() {
    return this.prisma.searchSpecies.findMany({
      where: { isActive: true },
      orderBy: [{ sortOrder: 'asc' }, { name: 'asc' }],
      select: { id: true, name: true },
    });
  }

  async search(query: any) {
    const page = Math.max(parseInt(query.page ?? '1', 10), 1);
    const take = Math.min(Math.max(parseInt(query.limit ?? '20', 10), 1), 50);
    const skip = (page - 1) * take;
    const type = query.type ?? 'animals';
    const latitude = this.optionalNumber(query.lat);
    const longitude = this.optionalNumber(query.lng);

    if (type === 'shelters') {
      const poolTake = Math.min(Math.max(take * 10, 100), 500);
      const shelters = await this.prisma.shelterProfile.findMany({
        where: {
          status: 'APPROVED',
          location: query.city ? { contains: query.city, mode: 'insensitive' } : undefined,
        },
        take: poolTake,
        orderBy: { createdAt: 'asc' },
        include: {
          user: { select: { id: true, name: true, avatarUrl: true } },
          _count: { select: { animals: true } },
        },
      });
      return (await this.rankSheltersByNeed(shelters, latitude, longitude)).slice(skip, skip + take);
    }

    const poolTake = Math.min(Math.max(take * 10, 100), 500);
    const animals = await this.prisma.animal.findMany({
      where: {
        species: query.species ? { equals: query.species, mode: 'insensitive' } : undefined,
        status: query.status && query.status !== 'ADOPTED' ? query.status : { not: 'ADOPTED' },
        shelterId: { not: null },
        adoptedById: null,
        shelter: {
          status: 'APPROVED',
          location: query.city ? { contains: query.city, mode: 'insensitive' } : undefined,
        },
      },
      take: poolTake,
      orderBy: { createdAt: 'asc' },
      include: {
        shelter: { include: { user: { select: { id: true, name: true, avatarUrl: true } } } },
        card: true,
        _count: { select: { videos: true } },
      },
    });

    const publicAnimals = animals.map(({ credentialUsername, credentialPassword, ...animal }) => animal);
    const rankedAnimals = await this.rankAnimalsByNeed(publicAnimals, latitude, longitude);
    return this.balanceAnimalsByShelter(rankedAnimals, take);
  }

  private async rankAnimalsByNeed<T extends {
    id: string;
    shelterId: string | null;
    shelter?: { latitude?: number | null; longitude?: number | null } | null;
  }>(animals: T[], latitude?: number, longitude?: number) {
    if (animals.length === 0) return animals;
    const animalIds = animals.map((animal) => animal.id);
    const shelterIds = Array.from(new Set(animals.map((animal) => animal.shelterId).filter(Boolean))) as string[];

    const [videoViewTotals, shelterDonationTotals] = await Promise.all([
      this.videoViewsByAnimal(animalIds),
      this.prisma.donation.groupBy({
        by: ['shelterId'],
        where: { shelterId: { in: shelterIds }, status: 'COMPLETED' },
        _sum: { amount: true },
      }),
    ]);

    const viewsByAnimal = videoViewTotals;
    const donationsByShelter = new Map<string, number>(
      shelterDonationTotals.map((row) => [row.shelterId, row._sum.amount ?? 0]),
    );

    return [...animals].sort((a, b) => {
      const aNeed = this.animalNeedScore(a, viewsByAnimal, donationsByShelter, latitude, longitude);
      const bNeed = this.animalNeedScore(b, viewsByAnimal, donationsByShelter, latitude, longitude);
      return bNeed - aNeed || this.stableTie(a.id) - this.stableTie(b.id);
    });
  }

  private async videoViewsByAnimal(animalIds: string[]) {
    if (animalIds.length === 0) return new Map<string, number>();
    const rows = await this.prisma.$queryRaw<Array<{ animalId: string; views: number | bigint }>>(Prisma.sql`
      select v."animalId" as "animalId", count(vv.id)::int as views
      from "video_views" vv
      join "videos" v on v.id = vv."videoId"
      where v."animalId" in (${Prisma.join(animalIds)})
      group by v."animalId"
    `);
    const viewsByAnimal = new Map<string, number>();
    for (const row of rows) {
      viewsByAnimal.set(row.animalId, Number(row.views));
    }
    return viewsByAnimal;
  }

  private animalNeedScore(
    animal: { id: string; shelterId: string | null; shelter?: { latitude?: number | null; longitude?: number | null } | null },
    viewsByAnimal: Map<string, number>,
    donationsByShelter: Map<string, number>,
    latitude?: number,
    longitude?: number,
  ) {
    const shelterSupport = animal.shelterId ? donationsByShelter.get(animal.shelterId) ?? 0 : 0;
    const views = viewsByAnimal.get(animal.id) ?? 0;
    const distance = this.distanceFromShelter(animal.shelter, latitude, longitude);
    return this.needScore({
      support: shelterSupport,
      exposure: views,
      distance,
    });
  }

  private async rankSheltersByNeed<T extends { id: string; latitude?: number | null; longitude?: number | null }>(
    shelters: T[],
    latitude?: number,
    longitude?: number,
  ) {
    if (shelters.length === 0) return shelters;
    const shelterIds = shelters.map((shelter) => shelter.id);
    const [donationTotals, videoViews] = await Promise.all([
      this.prisma.donation.groupBy({
        by: ['shelterId'],
        where: { shelterId: { in: shelterIds }, status: 'COMPLETED' },
        _sum: { amount: true },
      }),
      this.videoViewsByShelter(shelterIds),
    ]);

    const viewsByShelter = videoViews;
    const donationsByShelter = new Map<string, number>(
      donationTotals.map((row) => [row.shelterId, row._sum.amount ?? 0]),
    );
    return shelters
      .map((shelter) => ({
        ...shelter,
        distanceKm: this.distanceFromShelter(shelter, latitude, longitude),
      }))
      .sort((a, b) => {
        const aNeed = this.needScore({
          support: donationsByShelter.get(a.id) ?? 0,
          exposure: viewsByShelter.get(a.id) ?? 0,
          distance: a.distanceKm,
        });
        const bNeed = this.needScore({
          support: donationsByShelter.get(b.id) ?? 0,
          exposure: viewsByShelter.get(b.id) ?? 0,
          distance: b.distanceKm,
        });
        return bNeed - aNeed || this.stableTie(a.id) - this.stableTie(b.id);
      });
  }

  private async videoViewsByShelter(shelterIds: string[]) {
    if (shelterIds.length === 0) return new Map<string, number>();
    const rows = await this.prisma.$queryRaw<Array<{ shelterId: string; views: number | bigint }>>(Prisma.sql`
      select coalesce(a."shelterId", a."originalShelterId") as "shelterId", count(vv.id)::int as views
      from "video_views" vv
      join "videos" v on v.id = vv."videoId"
      join "animals" a on a.id = v."animalId"
      where coalesce(a."shelterId", a."originalShelterId") in (${Prisma.join(shelterIds)})
      group by coalesce(a."shelterId", a."originalShelterId")
    `);
    const viewsByShelter = new Map<string, number>();
    for (const row of rows) {
      viewsByShelter.set(row.shelterId, Number(row.views));
    }
    return viewsByShelter;
  }

  private needScore(input: { support: number; exposure: number; distance?: number }) {
    const lowSupport = 1 / (1 + input.support);
    const lowExposure = 1 / (1 + input.exposure);
    const proximity = input.distance === undefined ? 0.5 : 1 / (1 + input.distance / 8);
    return lowSupport * 0.55 + lowExposure * 0.3 + proximity * 0.15;
  }

  private distanceFromShelter(
    shelter?: { latitude?: number | null; longitude?: number | null } | null,
    latitude?: number,
    longitude?: number,
  ) {
    if (
      latitude === undefined ||
      longitude === undefined ||
      shelter?.latitude === null ||
      shelter?.latitude === undefined ||
      shelter?.longitude === null ||
      shelter?.longitude === undefined
    ) {
      return undefined;
    }
    return this.distanceKm(latitude, longitude, shelter.latitude, shelter.longitude);
  }

  private distanceKm(lat1: number, lng1: number, lat2: number, lng2: number) {
    const earthRadiusKm = 6371;
    const dLat = this.toRadians(lat2 - lat1);
    const dLng = this.toRadians(lng2 - lng1);
    const a =
      Math.sin(dLat / 2) ** 2 +
      Math.cos(this.toRadians(lat1)) *
        Math.cos(this.toRadians(lat2)) *
        Math.sin(dLng / 2) ** 2;
    return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  }

  private toRadians(value: number) {
    return (value * Math.PI) / 180;
  }

  private optionalNumber(value: unknown) {
    if (value === undefined || value === null || value === '') return undefined;
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : undefined;
  }

  private balanceAnimalsByShelter<T extends { id: string; shelterId: string | null }>(animals: T[], take: number) {
    const byShelter = new Map<string, T[]>();
    const withoutShelter: T[] = [];

    for (const animal of animals) {
      if (!animal.shelterId) {
        withoutShelter.push(animal);
        continue;
      }
      const shelterAnimals = byShelter.get(animal.shelterId) ?? [];
      shelterAnimals.push(animal);
      byShelter.set(animal.shelterId, shelterAnimals);
    }

    const firstFromEachShelter: T[] = [];
    const remaining: T[] = [...withoutShelter];

    for (const shelterAnimals of Array.from(byShelter.values())) {
      const [first, ...rest] = shelterAnimals;
      if (first) firstFromEachShelter.push(first);
      remaining.push(...rest);
    }

    return [...firstFromEachShelter, ...remaining].slice(0, take);
  }

  private stableTie(value: string) {
    let hash = 0;
    for (let index = 0; index < value.length; index += 1) {
      hash = (hash * 31 + value.charCodeAt(index)) >>> 0;
    }
    return hash;
  }

  async weeklyRanking() {
    const start = this.startOfWeek(new Date());
    const donations = await this.prisma.donation.groupBy({
      by: ['humanId'],
      where: { status: 'COMPLETED', createdAt: { gte: start } },
      _sum: { amount: true },
    });
    const helpedShelters = await this.prisma.donation.groupBy({
      by: ['humanId', 'shelterId'],
      where: { status: 'COMPLETED', createdAt: { gte: start } },
    });
    const adoptedAnimals = await this.prisma.animal.groupBy({
      by: ['adoptedById'],
      where: { status: 'ADOPTED', adoptedById: { not: null } },
      _count: { _all: true },
    });

    const totals = new Map<
      string,
      { totalDonated: number; sheltersHelped: number; animalsAdopted: number }
    >();
    const emptyTotal = () => ({
      totalDonated: 0,
      sheltersHelped: 0,
      animalsAdopted: 0,
    });
    for (const row of donations) {
      const current = totals.get(row.humanId) ?? emptyTotal();
      current.totalDonated = row._sum.amount ?? 0;
      totals.set(row.humanId, current);
    }
    for (const row of helpedShelters) {
      const current = totals.get(row.humanId) ?? emptyTotal();
      current.sheltersHelped += 1;
      totals.set(row.humanId, current);
    }
    for (const row of adoptedAnimals) {
      if (!row.adoptedById) continue;
      const current = totals.get(row.adoptedById) ?? emptyTotal();
      current.animalsAdopted = row._count._all;
      totals.set(row.adoptedById, current);
    }

    const ranked = Array.from(totals.entries())
      .sort((a, b) => b[1].totalDonated - a[1].totalDonated)
      .slice(0, 10);

    const rankingRows = await Promise.all(
      ranked.map(([userId, value], index) =>
        this.prisma.weeklyRanking.upsert({
          where: { userId_weekStart: { userId, weekStart: start } },
          update: { ...value, totalGalletas: 0, rank: index + 1 },
          create: { userId, weekStart: start, ...value, totalGalletas: 0, rank: index + 1 },
        }),
      ),
    );
    const users = await this.prisma.user.findMany({
      where: { id: { in: rankingRows.map((row) => row.userId) } },
      select: { id: true, name: true, avatarUrl: true },
    });
    const usersById = new Map(users.map((user) => [user.id, user]));

    return rankingRows.map((row) => ({
      ...row,
      userName: usersById.get(row.userId)?.name ?? 'Donador HuellaLive',
      avatarUrl: usersById.get(row.userId)?.avatarUrl ?? null,
    }));
  }

  private startOfWeek(date: Date) {
    const result = new Date(Date.UTC(date.getUTCFullYear(), date.getUTCMonth(), date.getUTCDate()));
    const day = result.getUTCDay() || 7;
    result.setUTCDate(result.getUTCDate() - day + 1);
    result.setUTCHours(0, 0, 0, 0);
    return result;
  }
}
