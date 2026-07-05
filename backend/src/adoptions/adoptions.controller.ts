import { Body, Controller, Get, Param, Patch, Post, Request, UseGuards } from '@nestjs/common';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { AdoptionsService } from './adoptions.service';

@Controller()
@UseGuards(JwtAuthGuard)
export class AdoptionsController {
  constructor(private readonly adoptionsService: AdoptionsService) {}

  @Post('adoption-requests/:animalId')
  request(@Request() req: any, @Param('animalId') animalId: string) {
    return this.adoptionsService.requestAdoption(req.user.id, animalId);
  }

  @Get('adoption-requests/animal/:animalId/state')
  stateForAnimal(@Request() req: any, @Param('animalId') animalId: string) {
    return this.adoptionsService.getRequestState(req.user.id, animalId);
  }

  @Get('adoption-requests')
  listForShelter(@Request() req: any) {
    return this.adoptionsService.listForShelter(req.user.id);
  }

  @Patch('adoption-requests/:id')
  update(@Request() req: any, @Param('id') id: string, @Body('status') status: string) {
    return this.adoptionsService.updateRequest(req.user.id, id, status);
  }

  @Post('adoption-requests/:id/transfer')
  transfer(@Request() req: any, @Param('id') id: string) {
    return this.adoptionsService.completeTransfer(req.user.id, id);
  }

  @Get('chats/:chatId/transferable-adoptions')
  transferableForChat(@Request() req: any, @Param('chatId') chatId: string) {
    return this.adoptionsService.listTransferableForChat(req.user.id, chatId);
  }

  @Post('chats/:chatId/transfer/:animalId')
  transferFromChat(
    @Request() req: any,
    @Param('chatId') chatId: string,
    @Param('animalId') animalId: string,
  ) {
    return this.adoptionsService.transferFromChat(req.user.id, chatId, animalId);
  }
}
