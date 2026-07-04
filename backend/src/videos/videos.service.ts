import { BadRequestException, ForbiddenException, Injectable, NotFoundException } from '@nestjs/common';
import { randomUUID } from 'crypto';
import { PrismaService } from '../prisma/prisma.service';
import { MediaService } from '../media/media.service';

@Injectable()
export class VideosService {
  constructor(
    private prisma: PrismaService,
    private mediaService: MediaService,
  ) {}

  async getAnimalVideos(animalId: string) {
    await this.ensureAnimalExists(animalId);
    const videos = await this.prisma.video.findMany({
      where: { animalId },
      orderBy: { createdAt: 'desc' },
      include: this.videoInclude(),
    });
    return videos.map((video) => this.normalizeVideo(video));
  }

  async createAnimalVideo(userId: string, animalId: string, dto: any) {
    const animal = await this.prisma.animal.findUnique({
      where: { id: animalId },
      include: { shelter: true },
    });
    if (!animal) throw new NotFoundException('Animal no encontrado');

    const isAdopted = !!animal.adoptedById || animal.status === 'ADOPTED';
    const isAdopter = animal.adoptedById === userId;
    const isShelterOwner = !isAdopted && animal.shelter?.userId === userId;
    if (!isAdopter && !isShelterOwner) throw new ForbiddenException('Sin permisos para subir videos');
    if (isShelterOwner && animal.shelter?.status !== 'APPROVED') {
      throw new ForbiddenException('Tu albergue aun no ha sido aprobado');
    }
    if (!dto.videoUrl) throw new ForbiddenException('videoUrl es requerido');
    const normalizedVideoUrl = this.mediaService.normalizedVideoUrl(dto.videoUrl);

    const video = await this.prisma.video.create({
      data: {
        animalId,
        uploadedById: userId,
        videoUrl: normalizedVideoUrl,
        thumbnailUrl: dto.thumbnailUrl || this.mediaService.videoThumbnailUrl(normalizedVideoUrl),
        description: dto.description ?? '',
      },
      include: this.videoInclude(),
    });
    return this.normalizeVideo(video);
  }

  async getShelterStories(shelterId: string) {
    const shelter = await this.prisma.shelterProfile.findUnique({ where: { id: shelterId } });
    if (!shelter) throw new NotFoundException('Albergue no encontrado');
    const stories = await this.prisma.shelterStory.findMany({
      where: { shelterId },
      orderBy: { createdAt: 'desc' },
      include: this.storyInclude(),
    });
    return stories.map((story) => this.normalizeStory(story));
  }

  async createShelterStory(userId: string, dto: any) {
    const shelter = await this.prisma.shelterProfile.findUnique({ where: { userId } });
    if (!shelter) throw new ForbiddenException('No tienes perfil de albergue');
    if (shelter.status !== 'APPROVED') throw new ForbiddenException('Tu albergue aun no ha sido aprobado');
    if (!dto.videoUrl) throw new ForbiddenException('videoUrl es requerido');
    const normalizedVideoUrl = this.mediaService.normalizedVideoUrl(dto.videoUrl);

    const story = await this.prisma.shelterStory.create({
      data: {
        shelterId: shelter.id,
        uploadedById: userId,
        videoUrl: normalizedVideoUrl,
        thumbnailUrl: dto.thumbnailUrl || this.mediaService.videoThumbnailUrl(normalizedVideoUrl),
        description: dto.description ?? '',
      },
      include: this.storyInclude(),
    });
    return this.normalizeStory(story);
  }

  async like(userId: string, videoId: string) {
    await this.ensureVideoExists(videoId);
    await this.prisma.like.upsert({
      where: { userId_videoId: { userId, videoId } },
      update: {},
      create: { userId, videoId },
    });
    return this.syncLikes(videoId, userId);
  }

  async unlike(userId: string, videoId: string) {
    await this.ensureVideoExists(videoId);
    await this.prisma.like.deleteMany({ where: { userId, videoId } });
    return this.syncLikes(videoId, userId);
  }

  async deleteVideo(userId: string, videoId: string) {
    const video = await this.prisma.video.findUnique({
      where: { id: videoId },
      include: {
        animal: { include: { shelter: true } },
      },
    });
    if (!video) throw new NotFoundException('Video no encontrado');

    const user = await this.prisma.user.findUnique({ where: { id: userId }, select: { role: true } });
    const isUploader = video.uploadedById === userId;
    const isShelterOwner = video.animal.shelter?.userId === userId;
    const isAdmin = user?.role === 'ADMIN';
    if (!isUploader && !isShelterOwner && !isAdmin) {
      throw new ForbiddenException('Sin permisos para borrar este video');
    }

    await this.mediaService.destroyByUrl(video.videoUrl, 'video');
    if (video.thumbnailUrl && !this.mediaService.isDerivedVideoThumbnail(video.videoUrl, video.thumbnailUrl)) {
      await this.mediaService.destroyByUrl(video.thumbnailUrl, 'image');
    }
    await this.prisma.like.deleteMany({ where: { videoId } });
    await this.prisma.videoView.deleteMany({ where: { videoId } });
    await this.prisma.video.delete({ where: { id: videoId } });
    return { deleted: true };
  }

  async deleteShelterStory(userId: string, storyId: string) {
    const story = await this.prisma.shelterStory.findUnique({
      where: { id: storyId },
      include: { shelter: true },
    });
    if (!story) throw new NotFoundException('Historia no encontrada');

    const user = await this.prisma.user.findUnique({ where: { id: userId }, select: { role: true } });
    const isUploader = story.uploadedById === userId;
    const isShelterOwner = story.shelter?.userId === userId;
    const isAdmin = user?.role === 'ADMIN';
    if (!isUploader && !isShelterOwner && !isAdmin) {
      throw new ForbiddenException('Sin permisos para borrar esta historia');
    }

    await this.mediaService.destroyByUrl(story.videoUrl, 'video');
    if (story.thumbnailUrl && !this.mediaService.isDerivedVideoThumbnail(story.videoUrl, story.thumbnailUrl)) {
      await this.mediaService.destroyByUrl(story.thumbnailUrl, 'image');
    }
    await this.prisma.shelterStory.delete({ where: { id: storyId } });
    return { deleted: true };
  }

  async reportVideo(userId: string, videoId: string, dto: any) {
    await this.ensureVideoExists(videoId);
    return this.createReport(userId, 'VIDEO', videoId, dto?.reason, dto?.details, videoId);
  }

  async reportAnimal(userId: string, animalId: string, dto: any) {
    await this.ensureAnimalExists(animalId);
    return this.createReport(userId, 'ANIMAL', animalId, dto?.reason, dto?.details, null);
  }

  private async syncLikes(videoId: string, userId?: string) {
    const likesCount = await this.prisma.like.count({ where: { videoId } });
    const video = await this.prisma.video.update({
      where: { id: videoId },
      data: { likesCount },
      include: this.videoInclude(),
    });
    return {
      ...this.normalizeVideo(video),
      isLiked: userId
        ? await this.prisma.like.count({ where: { videoId, userId } }).then((count) => count > 0)
        : false,
    };
  }

  private async ensureAnimalExists(animalId: string) {
    const animal = await this.prisma.animal.findUnique({ where: { id: animalId } });
    if (!animal) throw new NotFoundException('Animal no encontrado');
  }

  private async ensureVideoExists(videoId: string) {
    const video = await this.prisma.video.findUnique({ where: { id: videoId } });
    if (!video) throw new NotFoundException('Video no encontrado');
  }

  private async createReport(
    userId: string,
    targetType: 'VIDEO' | 'ANIMAL',
    targetId: string,
    reason: unknown,
    details: unknown,
    videoId: string | null,
  ) {
    const cleanReason = typeof reason === 'string' ? reason.trim() : '';
    const cleanDetails = typeof details === 'string' ? details.trim() : '';
    if (!cleanReason) throw new BadRequestException('El motivo del reporte es obligatorio');
    if (cleanReason.length > 120) throw new BadRequestException('El motivo es demasiado largo');
    if (cleanDetails.length > 500) throw new BadRequestException('El detalle es demasiado largo');

    const existing = await this.prisma.$queryRaw<Array<{ id: string }>>`
      select id from "reports"
      where "reporterId" = ${userId}
        and "targetType" = ${targetType}
        and "targetId" = ${targetId}
        and status = 'PENDING'
      limit 1
    `;
    if (existing.length > 0) {
      return { reported: true, duplicate: true };
    }

    const id = randomUUID();
    await this.prisma.$executeRaw`
      insert into "reports" ("id", "reporterId", "targetType", "targetId", "reason", "details", "status", "videoId", "createdAt")
      values (${id}, ${userId}, ${targetType}, ${targetId}, ${cleanReason}, ${cleanDetails || null}, 'PENDING', ${videoId}, now())
    `;
    return { reported: true, id };
  }

  private videoInclude() {
    return {
      uploadedBy: { select: { id: true, name: true, avatarUrl: true, role: true } },
      animal: {
        select: {
          id: true,
          name: true,
          species: true,
          breed: true,
          photoUrl: true,
          status: true,
          shelter: { select: { id: true, user: { select: { name: true, avatarUrl: true } } } },
        },
      },
    };
  }

  private storyInclude() {
    return {
      uploadedBy: { select: { id: true, name: true, avatarUrl: true, role: true } },
      shelter: { select: { id: true, user: { select: { name: true, avatarUrl: true } } } },
    };
  }

  private normalizeVideo<T extends { videoUrl: string; thumbnailUrl?: string | null }>(video: T): T {
    const videoUrl = this.mediaService.normalizedVideoUrl(video.videoUrl);
    return {
      ...video,
      videoUrl,
      thumbnailUrl: video.thumbnailUrl || this.mediaService.videoThumbnailUrl(videoUrl),
    };
  }

  private normalizeStory<T extends { videoUrl: string; thumbnailUrl?: string | null }>(story: T) {
    const videoUrl = this.mediaService.normalizedVideoUrl(story.videoUrl);
    return {
      ...story,
      videoUrl,
      thumbnailUrl: story.thumbnailUrl || this.mediaService.videoThumbnailUrl(videoUrl),
      likesCount: 0,
      isLiked: false,
      animal: null,
    };
  }
}
