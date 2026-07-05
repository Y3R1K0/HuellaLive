import {
  BadRequestException,
  Controller,
  Param,
  Post,
  Request,
  UploadedFile,
  UseGuards,
  UseInterceptors,
} from '@nestjs/common';
import { FileInterceptor } from '@nestjs/platform-express';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { MediaService } from './media.service';

@Controller('media')
@UseGuards(JwtAuthGuard)
export class MediaController {
  constructor(private readonly mediaService: MediaService) {}

  @Post('human/avatar')
  @UseInterceptors(FileInterceptor('file'))
  async uploadHumanAvatar(@Request() req: any, @UploadedFile() file: Express.Multer.File) {
    this.validateFile(file, 'image/');
    const folder = await this.mediaService.humanAvatarFolder(req.user.id);
    const result = await this.mediaService.upload(file, 'image', folder);
    return this.toResponse(result);
  }

  @Post('shelter/avatar')
  @UseInterceptors(FileInterceptor('file'))
  async uploadShelterAvatar(@Request() req: any, @UploadedFile() file: Express.Multer.File) {
    this.validateFile(file, 'image/');
    const folder = await this.mediaService.shelterProfileFolder(req.user.id);
    const result = await this.mediaService.upload(file, 'image', `${folder}/avatar`);
    return this.toResponse(result);
  }

  @Post('shelter/cover')
  @UseInterceptors(FileInterceptor('file'))
  async uploadShelterCover(@Request() req: any, @UploadedFile() file: Express.Multer.File) {
    this.validateFile(file, 'image/');
    const folder = await this.mediaService.shelterProfileFolder(req.user.id);
    const result = await this.mediaService.upload(file, 'image', `${folder}/cover`);
    return this.toResponse(result);
  }

  @Post('shelter/story-video')
  @UseInterceptors(FileInterceptor('file'))
  async uploadShelterStoryVideo(@Request() req: any, @UploadedFile() file: Express.Multer.File) {
    this.validateFile(file, 'video/');
    const folder = await this.mediaService.shelterStoriesFolder(req.user.id);
    const result = await this.mediaService.upload(file, 'video', folder);
    return this.toResponse(result, this.mediaService.normalizedVideoUrl(result.secure_url));
  }

  @Post('animals/:animalId/photo')
  @UseInterceptors(FileInterceptor('file'))
  async uploadAnimalPhoto(
    @Request() req: any,
    @Param('animalId') animalId: string,
    @UploadedFile() file: Express.Multer.File,
  ) {
    this.validateFile(file, 'image/');
    const folder = await this.mediaService.animalFolder(req.user.id, animalId, 'photos');
    const result = await this.mediaService.upload(file, 'image', folder);
    return this.toResponse(result);
  }

  @Post('animals/:animalId/thumbnail')
  @UseInterceptors(FileInterceptor('file'))
  async uploadAnimalVideoThumbnail(
    @Request() req: any,
    @Param('animalId') animalId: string,
    @UploadedFile() file: Express.Multer.File,
  ) {
    this.validateFile(file, 'image/');
    const folder = await this.mediaService.animalFolder(req.user.id, animalId, 'thumbnails');
    const result = await this.mediaService.upload(file, 'image', folder);
    return this.toResponse(result);
  }

  @Post('animals/:animalId/video')
  @UseInterceptors(FileInterceptor('file'))
  async uploadAnimalVideo(
    @Request() req: any,
    @Param('animalId') animalId: string,
    @UploadedFile() file: Express.Multer.File,
  ) {
    this.validateFile(file, 'video/');
    const folder = await this.mediaService.animalFolder(req.user.id, animalId, 'videos');
    const result = await this.mediaService.upload(file, 'video', folder);
    return this.toResponse(result, this.mediaService.normalizedVideoUrl(result.secure_url));
  }

  @Post('chats/:chatId/image')
  @UseInterceptors(FileInterceptor('file'))
  async uploadChatImage(
    @Request() req: any,
    @Param('chatId') chatId: string,
    @UploadedFile() file: Express.Multer.File,
  ) {
    this.validateFile(file, 'image/');
    const folder = await this.mediaService.chatFolder(req.user.id, chatId);
    const result = await this.mediaService.upload(file, 'image', folder);
    return this.toResponse(result);
  }

  private validateFile(file: Express.Multer.File | undefined, mimePrefix: string) {
    if (!file) throw new BadRequestException('Archivo requerido');
    if (!file.mimetype.startsWith(mimePrefix)) {
      throw new BadRequestException(`El archivo debe ser ${mimePrefix === 'image/' ? 'una imagen' : 'un video'}`);
    }
  }

  private toResponse(result: any, url: string = result.secure_url) {
    return {
      url,
      publicId: result.public_id,
      resourceType: result.resource_type,
      format: result.format,
      width: result.width,
      height: result.height,
      duration: result.duration,
      bytes: result.bytes,
    };
  }
}
