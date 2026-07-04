import { Module } from '@nestjs/common';
import { FeedService } from './feed.service';
import { FeedController } from './feed.controller';
import { MediaModule } from '../media/media.module';
import { PrismaModule } from '../prisma/prisma.module';

@Module({
  imports: [PrismaModule, MediaModule],
  controllers: [FeedController],
  providers: [FeedService],
})
export class FeedModule {}
