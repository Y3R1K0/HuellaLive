import { Body, Controller, Get, Post, Query, Res } from '@nestjs/common';
import type { Response } from 'express';
import { WalletService } from './wallet.service';

@Controller('payments/mercadopago')
export class MercadoPagoWebhookController {
  constructor(private readonly walletService: WalletService) {}

  @Post('webhook')
  webhook(@Query() query: any, @Body() body: any) {
    return this.walletService.handleMercadoPagoNotification(query, body);
  }

  @Get('oauth/callback')
  async oauthCallback(@Query() query: any, @Res() response: Response) {
    await this.walletService.handleMercadoPagoOAuthCallback(query);
    response
      .type('html')
      .send(`
        <!doctype html>
        <html lang="es">
          <head><meta charset="utf-8"><title>Mercado Pago conectado</title></head>
          <body style="font-family: sans-serif; padding: 32px;">
            <h1>Mercado Pago conectado</h1>
            <p>Ya puedes volver a HuellaLive. Las donaciones de este albergue usaran marketplace.</p>
          </body>
        </html>
      `);
  }
}
