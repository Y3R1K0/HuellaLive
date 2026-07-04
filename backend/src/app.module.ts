import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { PrismaModule } from './prisma/prisma.module';
import { AuthModule } from './auth/auth.module';
import { FeedModule } from './feed/feed.module';
import { AnimalsModule } from './animals/animals.module';
import { SheltersModule } from './shelters/shelters.module';
import { UsersModule } from './users/users.module';
import { VideosModule } from './videos/videos.module';
import { ExploreModule } from './explore/explore.module';
import { SocialModule } from './social/social.module';
import { ChatsModule } from './chats/chats.module';
import { AdoptionsModule } from './adoptions/adoptions.module';
import { WalletModule } from './wallet/wallet.module';
import { NotificationsModule } from './notifications/notifications.module';
import { AdminModule } from './admin/admin.module';
import { MediaModule } from './media/media.module';
import { HealthController } from './health.controller';

@Module({
  imports: [
    ConfigModule.forRoot({ isGlobal: true }),
    PrismaModule,
    AuthModule,
    FeedModule,
    AnimalsModule,
    SheltersModule,
    UsersModule,
    VideosModule,
    ExploreModule,
    SocialModule,
    ChatsModule,
    AdoptionsModule,
    WalletModule,
    NotificationsModule,
    AdminModule,
    MediaModule,
  ],
  controllers: [HealthController],
})
export class AppModule {}
