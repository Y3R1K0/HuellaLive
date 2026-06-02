import { Controller, Get, Post, Patch, Put, Body, Param, UseGuards, Request } from '@nestjs/common';
import { AnimalsService } from './animals.service';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';

@Controller('animals')
export class AnimalsController {
  constructor(private readonly animalsService: AnimalsService) {}

  @Get(':id')
  getAnimal(@Param('id') id: string) {
    return this.animalsService.getAnimalById(id);
  }

  @UseGuards(JwtAuthGuard)
  @Get('shelter/mine')
  getMyAnimals(@Request() req: any) {
    return this.animalsService.getMyAnimals(req.user.id);
  }

  @UseGuards(JwtAuthGuard)
  @Get('human/adopted')
  getAdoptedAnimals(@Request() req: any) {
    return this.animalsService.getAdoptedAnimals(req.user.id);
  }

  @UseGuards(JwtAuthGuard)
  @Post()
  createAnimal(@Request() req: any, @Body() dto: any) {
    return this.animalsService.createAnimal(req.user.id, dto);
  }

  @UseGuards(JwtAuthGuard)
  @Patch(':id/status')
  updateStatus(@Request() req: any, @Param('id') id: string, @Body('status') status: string) {
    return this.animalsService.updateAnimalStatus(req.user.id, id, status);
  }

  @UseGuards(JwtAuthGuard)
  @Get(':id/credentials')
  getCredentials(@Request() req: any, @Param('id') id: string) {
    return this.animalsService.getAnimalCredentials(req.user.id, id);
  }

   @UseGuards(JwtAuthGuard)
   @Put(':id/card')
   updateCard(@Request() req: any, @Param('id') id: string, @Body() dto: any) {
     return this.animalsService.updateAnimalCard(req.user.id, id, dto);
   }

   @UseGuards(JwtAuthGuard)
   @Get('shelter/adopted')
   getShelterAdopted(@Request() req: any) {
     return this.animalsService.getShelterAdoptedAnimals(req.user.id);
   }
}
