import { ForbiddenException, Injectable, InternalServerErrorException, NotFoundException } from '@nestjs/common';
import { ConfigService } from '@nestjs/config';
import { v2 as cloudinary, UploadApiResponse } from 'cloudinary';
import { Readable } from 'stream';
import { PrismaService } from '../prisma/prisma.service';

type ResourceType = 'image' | 'video';

@Injectable()
export class MediaService {
  constructor(
    private readonly config: ConfigService,
    private readonly prisma: PrismaService,
  ) {
    cloudinary.config({
      cloud_name: this.config.get<string>('CLOUDINARY_CLOUD_NAME'),
      api_key: this.config.get<string>('CLOUDINARY_API_KEY'),
      api_secret: this.config.get<string>('CLOUDINARY_API_SECRET'),
      secure: true,
    });
  }

  upload(file: Express.Multer.File, resourceType: ResourceType, folder: string) {
    return new Promise<UploadApiResponse>((resolve, reject) => {
      const stream = cloudinary.uploader.upload_stream(
        {
          folder: `huellalive/${folder}`,
          resource_type: resourceType,
          overwrite: false,
        },
        (error, result) => {
          if (error || !result) {
            reject(error ?? new InternalServerErrorException('No se pudo subir el archivo'));
            return;
          }
          resolve(result);
        },
      );

      Readable.from(file.buffer).pipe(stream);
    });
  }

  normalizedVideoUrl(url: string) {
    return this.withCloudinaryVideoTransform(url);
  }

  videoThumbnailUrl(url: string) {
    try {
      const parsed = new URL(url);
      if (!parsed.hostname.includes('res.cloudinary.com')) return null;

      const parts = parsed.pathname.split('/').filter(Boolean);
      const uploadIndex = parts.indexOf('upload');
      if (uploadIndex === -1 || !parts.includes('video')) return null;

      const assetParts = parts.slice(uploadIndex + 1);
      while (assetParts.length > 0 && !assetParts[0].match(/^v\d+$/)) {
        assetParts.shift();
      }
      if (assetParts.length === 0) return null;

      const last = assetParts[assetParts.length - 1];
      assetParts[assetParts.length - 1] = last.replace(/\.[^.]+$/, '.jpg');
      parsed.pathname = `/${parts.slice(0, uploadIndex + 1).join('/')}/so_1,w_720,h_1280,c_fill,q_auto,f_jpg/${assetParts.join('/')}`;
      return parsed.toString();
    } catch {
      return null;
    }
  }

  isDerivedVideoThumbnail(videoUrl: string, thumbnailUrl: string) {
    return this.extractPublicId(videoUrl) === this.extractPublicId(thumbnailUrl);
  }

  async destroyByUrl(url: string, resourceType: ResourceType) {
    const publicId = this.extractPublicId(url);
    if (!publicId) return;

    try {
      await cloudinary.uploader.destroy(publicId, { resource_type: resourceType });
    } catch {
      // Keep database cleanup working even if the asset was already removed manually.
    }
  }

  async humanAvatarFolder(userId: string) {
    const user = await this.prisma.user.findUnique({ where: { id: userId } });
    if (!user) throw new NotFoundException('Usuario no encontrado');
    return `humans/${userId}/profile`;
  }

  async shelterProfileFolder(userId: string) {
    const shelter = await this.prisma.shelterProfile.findUnique({ where: { userId } });
    if (!shelter) throw new ForbiddenException('No tienes perfil de albergue');
    return `shelters/${shelter.id}/profile`;
  }

  async shelterStoriesFolder(userId: string) {
    const shelter = await this.prisma.shelterProfile.findUnique({ where: { userId } });
    if (!shelter) throw new ForbiddenException('No tienes perfil de albergue');
    if (shelter.status !== 'APPROVED') throw new ForbiddenException('Tu albergue aun no ha sido aprobado');
    return `shelters/${shelter.id}/stories`;
  }

  async animalFolder(userId: string, animalId: string, target: 'photos' | 'videos' | 'thumbnails') {
    const animal = await this.prisma.animal.findUnique({
      where: { id: animalId },
      include: { shelter: true },
    });
    if (!animal) throw new NotFoundException('Animal no encontrado');

    const isShelterOwner = animal.shelter?.userId === userId;
    const isAdopter = animal.adoptedById === userId;
    if (!isShelterOwner && !isAdopter) throw new ForbiddenException('Sin permisos sobre este animal');

    if (isShelterOwner) {
      return `shelters/${animal.shelterId}/animals/${animal.id}/${target}`;
    }
    return `humans/${userId}/adopted-animals/${animal.id}/${target}`;
  }

  async chatFolder(userId: string, chatId: string) {
    const chat = await this.prisma.chat.findUnique({
      where: { id: chatId },
      include: { shelter: true },
    });
    if (!chat) throw new NotFoundException('Chat no encontrado');
    if (chat.humanId !== userId && chat.shelter.userId !== userId) {
      throw new ForbiddenException('No perteneces a este chat');
    }
    return `chats/${chat.id}/images`;
  }

  private extractPublicId(url: string) {
    try {
      const { pathname } = new URL(url);
      const parts = pathname.split('/').filter(Boolean);
      const uploadIndex = parts.indexOf('upload');
      if (uploadIndex === -1) return null;

      const assetParts = parts.slice(uploadIndex + 1);
      const versionIndex = assetParts.findIndex((part) => part.match(/^v\d+$/));
      if (versionIndex >= 0) assetParts.splice(0, versionIndex + 1);
      if (assetParts.length === 0) return null;

      const last = assetParts[assetParts.length - 1];
      assetParts[assetParts.length - 1] = last.replace(/\.[^.]+$/, '');
      return decodeURIComponent(assetParts.join('/'));
    } catch {
      return null;
    }
  }

  private withCloudinaryVideoTransform(url: string) {
    try {
      const parsed = new URL(url);
      if (!parsed.hostname.includes('res.cloudinary.com')) return url;

      const parts = parsed.pathname.split('/').filter(Boolean);
      const uploadIndex = parts.indexOf('upload');
      if (uploadIndex === -1) return url;

      const next = parts[uploadIndex + 1];
      const alreadyTransformed = next != null && !next.match(/^v\d+$/);
      if (!alreadyTransformed) {
        parts.splice(uploadIndex + 1, 0, 'f_mp4,vc_h264,ac_aac,q_auto');
      }

      parsed.pathname = `/${parts.join('/')}`;
      return parsed.toString();
    } catch {
      return url;
    }
  }
}
