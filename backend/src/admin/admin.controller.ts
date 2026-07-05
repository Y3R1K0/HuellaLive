import { Body, Controller, Delete, Get, Param, Patch, Post, Request, UseGuards } from '@nestjs/common';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { Roles } from '../auth/roles.decorator';
import { RolesGuard } from '../auth/roles.guard';
import { AdminService } from './admin.service';

@Controller('admin')
@UseGuards(JwtAuthGuard, RolesGuard)
@Roles('ADMIN')
export class AdminController {
  constructor(private readonly adminService: AdminService) {}

  @Get('shelters/pending')
  pendingShelters() {
    return this.adminService.pendingShelters();
  }

  @Get('shelters')
  shelters() {
    return this.adminService.shelters();
  }

  @Patch('shelters/:id')
  updateShelter(@Param('id') id: string, @Body() dto: any) {
    return this.adminService.updateShelter(id, dto);
  }

  @Delete('shelters/:id')
  deleteShelter(@Param('id') id: string) {
    return this.adminService.deleteShelter(id);
  }

  @Patch('shelters/:id/approve')
  approveShelter(@Param('id') id: string) {
    return this.adminService.approveShelter(id);
  }

  @Patch('shelters/:id/reject')
  rejectShelter(@Param('id') id: string) {
    return this.adminService.rejectShelter(id);
  }

  @Post('badges/:userId')
  assignBadge(@Request() req: any, @Param('userId') userId: string, @Body() dto: any) {
    return this.adminService.assignBadge(req.user.id, userId, dto);
  }

  @Get('stats')
  stats() {
    return this.adminService.stats();
  }

  @Get('search-cities')
  searchCities() {
    return this.adminService.searchCities();
  }

  @Post('search-cities')
  createSearchCity(@Body() dto: any) {
    return this.adminService.createSearchCity(dto);
  }

  @Patch('search-cities/:id')
  updateSearchCity(@Param('id') id: string, @Body() dto: any) {
    return this.adminService.updateSearchCity(id, dto);
  }

  @Delete('search-cities/:id')
  deleteSearchCity(@Param('id') id: string) {
    return this.adminService.deleteSearchCity(id);
  }

  @Get('search-species')
  searchSpecies() {
    return this.adminService.searchSpecies();
  }

  @Post('search-species')
  createSearchSpecies(@Body() dto: any) {
    return this.adminService.createSearchSpecies(dto);
  }

  @Patch('search-species/:id')
  updateSearchSpecies(@Param('id') id: string, @Body() dto: any) {
    return this.adminService.updateSearchSpecies(id, dto);
  }

  @Delete('search-species/:id')
  deleteSearchSpecies(@Param('id') id: string) {
    return this.adminService.deleteSearchSpecies(id);
  }

  @Get('species-requests')
  speciesRequests() {
    return this.adminService.speciesRequests();
  }

  @Patch('species-requests/:id/approve')
  approveSpeciesRequest(@Request() req: any, @Param('id') id: string) {
    return this.adminService.approveSpeciesRequest(req.user.id, id);
  }

  @Patch('species-requests/:id/reject')
  rejectSpeciesRequest(@Request() req: any, @Param('id') id: string, @Body() dto: any) {
    return this.adminService.rejectSpeciesRequest(req.user.id, id, dto.reason);
  }

  @Get('reports')
  reports() {
    return this.adminService.reports();
  }

  @Patch('reports/:id')
  updateReport(@Request() req: any, @Param('id') id: string, @Body() dto: any) {
    return this.adminService.updateReport(req.user.id, id, dto.status);
  }
}
