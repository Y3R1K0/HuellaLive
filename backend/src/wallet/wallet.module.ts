import { Module } from '@nestjs/common';
import { PrismaModule } from '../prisma/prisma.module';
import { MercadoPagoWebhookController } from './mercadopago-webhook.controller';
import { WalletController } from './wallet.controller';
import { WalletService } from './wallet.service';

@Module({
  imports: [PrismaModule],
  controllers: [WalletController, MercadoPagoWebhookController],
  providers: [WalletService],
})
export class WalletModule {}
