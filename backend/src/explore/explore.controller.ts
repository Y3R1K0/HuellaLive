import { Controller, Get, Query } from '@nestjs/common';
import { ExploreService } from './explore.service';

@Controller()
export class ExploreController {
  constructor(private readonly exploreService: ExploreService) {}

  @Get('search')
  search(@Query() query: any) {
    return this.exploreService.search(query);
  }

  @Get('search/cities')
  searchCities() {
    return this.exploreService.searchCities();
  }

  @Get('search/species')
  searchSpecies() {
    return this.exploreService.searchSpecies();
  }

  @Get('ranking/weekly')
  weeklyRanking() {
    return this.exploreService.weeklyRanking();
  }
}
