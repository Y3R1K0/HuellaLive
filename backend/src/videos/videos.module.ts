import { Module } from '@nestjs/common';
import { MediaModule } from '../media/media.module';
import { PrismaModule } from '../prisma/prisma.module';
import { VideosController } from './videos.controller';
import { VideosService } from './videos.service';

@Module({
  imports: [PrismaModule, MediaModule],
  controllers: [VideosController],
  providers: [VideosService],
})
export class VideosModule {}
