import { Controller, Get, Patch, Body, Param, UseGuards, Request } from '@nestjs/common';
import { SheltersService } from './shelters.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@Controller('shelters')
export class SheltersController {
  constructor(private readonly sheltersService: SheltersService) {}

  @Get('nearby')
  getNearby() {
    return this.sheltersService.getNearbyShelters();
  }

  @Get(':id')
  getShelter(@Param('id') id: string) {
    return this.sheltersService.getShelterById(id);
  }

  @UseGuards(JwtAuthGuard)
  @Get('me/profile')
  getMyProfile(@Request() req: any) {
    return this.sheltersService.getMyShelterProfile(req.user.id);
  }

  @UseGuards(JwtAuthGuard)
  @Patch('me/profile')
  updateMyProfile(@Request() req: any, @Body() dto: any) {
    return this.sheltersService.updateShelterProfile(req.user.id, dto);
  }
}