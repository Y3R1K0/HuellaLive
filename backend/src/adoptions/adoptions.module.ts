import { Module } from '@nestjs/common';
import { PrismaModule } from '../prisma/prisma.module';
import { AdoptionsController } from './adoptions.controller';
import { AdoptionsService } from './adoptions.service';

@Module({
  imports: [PrismaModule],
  controllers: [AdoptionsController],
  providers: [AdoptionsService],
})
export class AdoptionsModule {}
