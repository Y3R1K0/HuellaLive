import { Body, Controller, Get, Param, Post, Request, UseGuards } from '@nestjs/common';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { WalletService } from './wallet.service';

@Controller()
@UseGuards(JwtAuthGuard)
export class WalletController {
  constructor(private readonly walletService: WalletService) {}

  @Post('donations/:shelterId')
  donate(@Request() req: any, @Param('shelterId') shelterId: string, @Body() dto: any) {
    return this.walletService.donate(req.user.id, shelterId, dto);
  }

  @Get('wallet/shelter/dashboard')
  shelterDashboard(@Request() req: any) {
    return this.walletService.getShelterDashboard(req.user.id);
  }

  @Post('wallet/shelter/withdrawals')
  requestWithdrawal(@Request() req: any, @Body() dto: any) {
    return this.walletService.requestWithdrawal(req.user.id, dto);
  }

  @Get('wallet/shelter/mercadopago/connect')
  connectMercadoPago(@Request() req: any) {
    return this.walletService.createMercadoPagoConnectUrl(req.user.id);
  }
}
