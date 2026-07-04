import { Controller, Get, Patch, Body, Param, Query, UseGuards, Request } from '@nestjs/common';
import { SheltersService } from './shelters.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { OptionalJwtAuthGuard } from '../auth/optional-jwt-auth.guard';

@Controller('shelters')
export class SheltersController {
  constructor(private readonly sheltersService: SheltersService) {}

  @Get('nearby')
  getNearby(@Query('lat') lat?: string, @Query('lng') lng?: string, @Query('radiusKm') radiusKm?: string) {
    return this.sheltersService.getNearbyShelters(
      lat ? Number(lat) : undefined,
      lng ? Number(lng) : undefined,
      radiusKm ? Number(radiusKm) : undefined,
    );
  }

  @Get('geocoding/search')
  searchLocation(@Query('q') query: string) {
    return this.sheltersService.searchLocation(query);
  }

  @Get('geocoding/reverse')
  reverseLocation(@Query('lat') lat: string, @Query('lng') lng: string) {
    return this.sheltersService.reverseLocation(Number(lat), Number(lng));
  }

  @UseGuards(OptionalJwtAuthGuard)
  @Get(':id')
  getShelter(@Param('id') id: string, @Request() req: any) {
    return this.sheltersService.getShelterById(id, req.user?.id);
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
