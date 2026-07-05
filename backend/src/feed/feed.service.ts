import { Injectable, NotFoundException } from '@nestjs/common';
import { MediaService } from '../media/media.service';
import { PrismaService } from '../prisma/prisma.service';
import { randomUUID } from 'crypto';

@Injectable()
export class FeedService {
  constructor(
    private prisma: PrismaService,
    private mediaService: MediaService,
  ) {}

  async getFeed(page: number = 1, limit: number = 10, userId?: string | null) {
    const skip = (page - 1) * limit;
    const viewedIds = userId ? await this.viewedVideoIds(userId) : [];
    const fetchLimit = skip + limit;
    const [videos, stories] = await Promise.all([
      this.prisma.video.findMany({
      where: viewedIds.length ? { id: { notIn: viewedIds } } : undefined,
      take: fetchLimit,
      orderBy: { createdAt: 'desc' },
      include: this.videoInclude(userId),
      }),
      this.prisma.shelterStory.findMany({
        take: fetchLimit,
        orderBy: { createdAt: 'desc' },
        include: this.storyInclude(),
      }),
    ]);
    return [
      ...videos.map((video) => this.toVideoResponse(video, userId)),
      ...stories.map((story) => this.toStoryResponse(story)),
    ]
      .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
      .slice(skip, skip + limit);
  }

  async markViewed(userId: string, videoId: string) {
    const video = await this.prisma.video.findUnique({ where: { id: videoId } });
    if (!video) throw new NotFoundException('Video no encontrado');

    await this.prisma.$executeRaw`
      insert into "video_views" ("id", "userId", "videoId", "createdAt")
      values (${randomUUID()}, ${userId}, ${videoId}, now())
      on conflict ("userId", "videoId") do nothing
    `;
    return { viewed: true };
  }

  async getViewedHistory(userId: string, page: number = 1, limit: number = 40) {
    const skip = (page - 1) * limit;
    const rows = await this.prisma.$queryRaw<Array<{ videoId: string }>>`
      select "videoId"
      from "video_views"
      where "userId" = ${userId}
      order by "createdAt" desc
      limit ${limit}
      offset ${skip}
    `;
    const ids = rows.map((row) => row.videoId);
    if (ids.length === 0) return [];

    const videos = await this.prisma.video.findMany({
      where: { id: { in: ids } },
      include: this.videoInclude(userId),
    });
    const byId = new Map(videos.map((video) => [video.id, video]));
    return ids
      .map((id) => byId.get(id))
      .filter(Boolean)
      .map((video) => this.toVideoResponse(video, userId));
  }

  private videoInclude(userId?: string | null) {
    return {
      uploadedBy: {
        select: { id: true, name: true, avatarUrl: true, role: true }
      },
      animal: {
        select: {
          id: true, name: true, species: true,
          breed: true, status: true, photoUrl: true,
          shelter: {
            select: { id: true, user: { select: { name: true, avatarUrl: true } } }
          }
        }
      },
      likes: userId
        ? {
            where: { userId },
            select: { id: true },
          }
        : false,
    };
  }

  private storyInclude() {
    return {
      uploadedBy: {
        select: { id: true, name: true, avatarUrl: true, role: true }
      },
      shelter: {
        select: { id: true, user: { select: { name: true, avatarUrl: true } } }
      },
    };
  }

  private toVideoResponse(video: any, userId?: string | null) {
    const videoUrl = this.mediaService.normalizedVideoUrl(video.videoUrl);
    return {
      ...video,
      type: 'ANIMAL_VIDEO',
      videoUrl,
      thumbnailUrl: video.thumbnailUrl || this.mediaService.videoThumbnailUrl(videoUrl),
      isLiked: userId ? (video.likes?.length ?? 0) > 0 : false,
      likes: undefined,
    };
  }

  private toStoryResponse(story: any) {
    const videoUrl = this.mediaService.normalizedVideoUrl(story.videoUrl);
    return {
      ...story,
      type: 'SHELTER_STORY',
      videoUrl,
      thumbnailUrl: story.thumbnailUrl || this.mediaService.videoThumbnailUrl(videoUrl),
      likesCount: 0,
      isLiked: false,
      animal: null,
    };
  }

  private async viewedVideoIds(userId: string) {
    const rows = await this.prisma.$queryRaw<Array<{ videoId: string }>>`
      select "videoId"
      from "video_views"
      where "userId" = ${userId}
    `;
    return rows.map((row) => row.videoId);
  }
}
