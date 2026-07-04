import { BadGatewayException, BadRequestException, Injectable, NotFoundException } from '@nestjs/common';
import { PrismaService } from '../prisma/prisma.service';

@Injectable()
export class SheltersService {
  private lastNominatimRequestAt = 0;
  private nominatimQueue: Promise<unknown> = Promise.resolve();

  constructor(private prisma: PrismaService) {}

  async searchLocation(query: string) {
    const cleanQuery = query?.trim();
    if (!cleanQuery || cleanQuery.length < 3) {
      throw new BadRequestException('Escribe al menos 3 caracteres de la direccion');
    }
    if (cleanQuery.length > 160) {
      throw new BadRequestException('La direccion es demasiado larga');
    }

    const cacheKey = `search:${this.normalizeCacheKey(cleanQuery)}`;
    const cached = await this.readGeocodingCache(cacheKey);
    if (cached) return cached;

    const params = new URLSearchParams({
      q: cleanQuery,
      format: 'jsonv2',
      addressdetails: '1',
      countrycodes: 'pe',
      limit: '5',
    });
    const response = await this.requestNominatim(
      `https://nominatim.openstreetmap.org/search?${params.toString()}`,
    );
    const results = (Array.isArray(response) ? response : []).map((item: any) => ({
      displayName: item.display_name,
      latitude: Number(item.lat),
      longitude: Number(item.lon),
      city: this.extractCity(item.address),
    }));
    await this.writeGeocodingCache(cacheKey, 'SEARCH', results, 30);
    return results;
  }

  async reverseLocation(latitude: number, longitude: number) {
    if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) {
      throw new BadRequestException('Coordenadas invalidas');
    }
    if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
      throw new BadRequestException('Coordenadas fuera de rango');
    }

    const cacheKey = `reverse:${latitude.toFixed(5)}:${longitude.toFixed(5)}`;
    const cached = await this.readGeocodingCache(cacheKey);
    if (cached) return cached;

    const params = new URLSearchParams({
      lat: latitude.toString(),
      lon: longitude.toString(),
      format: 'jsonv2',
      addressdetails: '1',
      zoom: '18',
    });
    const response: any = await this.requestNominatim(
      `https://nominatim.openstreetmap.org/reverse?${params.toString()}`,
    );
    const result = {
      displayName: response.display_name ?? 'Ubicacion seleccionada',
      latitude,
      longitude,
      city: this.extractCity(response.address),
    };
    await this.writeGeocodingCache(cacheKey, 'REVERSE', result, 90);
    return result;
  }

  async getShelterById(id: string, viewerId?: string) {
    const shelter = await this.prisma.shelterProfile.findUnique({
      where: { id },
      include: {
        user: { select: { id: true, name: true, avatarUrl: true } },
        stories: {
          orderBy: { createdAt: 'desc' },
          take: 12,
          include: { uploadedBy: { select: { id: true, name: true, avatarUrl: true, role: true } } },
        },
        animals: {
          where: { status: { not: 'ADOPTED' } },
          orderBy: { createdAt: 'desc' },
          include: {
            videos: {
              orderBy: { createdAt: 'desc' },
              take: 6,
              include: { uploadedBy: { select: { id: true, name: true, avatarUrl: true, role: true } } },
            },
          },
        },
        _count: { select: { animals: true } }
      }
    });
    if (!shelter) throw new NotFoundException('Albergue no encontrado');
    const isFollowing = viewerId
      ? !!(await this.prisma.follow.findUnique({
          where: { humanId_shelterId: { humanId: viewerId, shelterId: id } },
          select: { id: true },
        }))
      : false;
    return { ...shelter, isFollowing };
  }

  async getMyShelterProfile(userId: string) {
    const shelter = await this.prisma.shelterProfile.findUnique({
      where: { userId },
      include: {
        user: { select: { id: true, name: true, avatarUrl: true } },
        stories: {
          orderBy: { createdAt: 'desc' },
          take: 12,
          include: { uploadedBy: { select: { id: true, name: true, avatarUrl: true, role: true } } },
        },
        animals: {
          orderBy: { createdAt: 'desc' },
          include: {
            videos: {
              orderBy: { createdAt: 'desc' },
              take: 6,
              include: { uploadedBy: { select: { id: true, name: true, avatarUrl: true, role: true } } },
            },
          },
        },
        _count: { select: { animals: true } }
      }
    });
    if (!shelter) throw new NotFoundException('Perfil no encontrado');
    return shelter;
  }

  async updateShelterProfile(userId: string, dto: any) {
    const data = { ...dto };
    if (dto.latitude !== undefined) {
      data.latitude = dto.latitude === '' || dto.latitude === null ? null : Number(dto.latitude);
    }
    if (dto.longitude !== undefined) {
      data.longitude = dto.longitude === '' || dto.longitude === null ? null : Number(dto.longitude);
    }

    return this.prisma.shelterProfile.update({
      where: { userId },
      data,
      include: {
        user: { select: { id: true, name: true, avatarUrl: true } },
        stories: {
          orderBy: { createdAt: 'desc' },
          take: 12,
          include: { uploadedBy: { select: { id: true, name: true, avatarUrl: true, role: true } } },
        },
        animals: {
          orderBy: { createdAt: 'desc' },
          include: {
            videos: {
              orderBy: { createdAt: 'desc' },
              take: 6,
              include: { uploadedBy: { select: { id: true, name: true, avatarUrl: true, role: true } } },
            },
          },
        },
        _count: { select: { animals: true } }
      }
    });
  }

  async getNearbyShelters(latitude?: number, longitude?: number, radiusKm: number = 100) {
    const shelters = await this.prisma.shelterProfile.findMany({
      where: {
        status: 'APPROVED',
        latitude: latitude === undefined ? undefined : { not: null },
        longitude: longitude === undefined ? undefined : { not: null },
      },
      include: {
        user: { select: { id: true, name: true, avatarUrl: true } },
        _count: { select: { animals: true } }
      },
      take: latitude === undefined || longitude === undefined ? 20 : 100,
    });

    if (latitude === undefined || longitude === undefined) return shelters;

    return shelters
      .map((shelter) => ({
        ...shelter,
        distanceKm: this.distanceKm(latitude, longitude, shelter.latitude!, shelter.longitude!),
      }))
      .filter((shelter) => shelter.distanceKm <= radiusKm)
      .sort((a, b) => a.distanceKm - b.distanceKm)
      .slice(0, 20);
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

  private requestNominatim(url: string): Promise<unknown> {
    const request = this.nominatimQueue.then(async () => {
      const elapsed = Date.now() - this.lastNominatimRequestAt;
      if (elapsed < 1100) {
        await new Promise((resolve) => setTimeout(resolve, 1100 - elapsed));
      }
      this.lastNominatimRequestAt = Date.now();
      const response = await fetch(url, {
        headers: {
          'User-Agent': 'HuellaLive/1.0 (contacto@huellalive.app)',
          'Accept-Language': 'es',
        },
      });
      if (!response.ok) {
        throw new BadGatewayException('No se pudo consultar OpenStreetMap');
      }
      return response.json();
    });
    this.nominatimQueue = request.catch(() => undefined);
    return request;
  }

  private async readGeocodingCache(cacheKey: string) {
    const cached = await this.prisma.geocodingCache.findUnique({ where: { cacheKey } });
    if (!cached || cached.expiresAt <= new Date()) return null;
    return cached.payload;
  }

  private async writeGeocodingCache(
    cacheKey: string,
    kind: string,
    payload: any,
    days: number,
  ) {
    const expiresAt = new Date(Date.now() + days * 24 * 60 * 60 * 1000);
    await this.prisma.geocodingCache.upsert({
      where: { cacheKey },
      update: { kind, payload, expiresAt },
      create: { cacheKey, kind, payload, expiresAt },
    });
  }

  private normalizeCacheKey(value: string) {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .replace(/\s+/g, ' ');
  }

  private extractCity(address: any) {
    if (!address) return null;
    return address.city ?? address.town ?? address.village ?? address.municipality ?? address.state ?? null;
  }
}
