import { Body, Controller, Delete, Get, Param, Post, Request, UseGuards } from '@nestjs/common';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { VideosService } from './videos.service';

@Controller()
export class VideosController {
  constructor(private readonly videosService: VideosService) {}

  @Get('animals/:animalId/videos')
  getAnimalVideos(@Param('animalId') animalId: string) {
    return this.videosService.getAnimalVideos(animalId);
  }

  @UseGuards(JwtAuthGuard)
  @Post('animals/:animalId/videos')
  createAnimalVideo(@Request() req: any, @Param('animalId') animalId: string, @Body() dto: any) {
    return this.videosService.createAnimalVideo(req.user.id, animalId, dto);
  }

  @Get('shelters/:shelterId/stories')
  getShelterStories(@Param('shelterId') shelterId: string) {
    return this.videosService.getShelterStories(shelterId);
  }

  @UseGuards(JwtAuthGuard)
  @Post('shelters/me/stories')
  createShelterStory(@Request() req: any, @Body() dto: any) {
    return this.videosService.createShelterStory(req.user.id, dto);
  }

  @UseGuards(JwtAuthGuard)
  @Post('likes/:videoId')
  like(@Request() req: any, @Param('videoId') videoId: string) {
    return this.videosService.like(req.user.id, videoId);
  }

  @UseGuards(JwtAuthGuard)
  @Delete('likes/:videoId')
  unlike(@Request() req: any, @Param('videoId') videoId: string) {
    return this.videosService.unlike(req.user.id, videoId);
  }

  @UseGuards(JwtAuthGuard)
  @Delete('videos/:videoId')
  deleteVideo(@Request() req: any, @Param('videoId') videoId: string) {
    return this.videosService.deleteVideo(req.user.id, videoId);
  }

  @UseGuards(JwtAuthGuard)
  @Delete('shelter-stories/:storyId')
  deleteShelterStory(@Request() req: any, @Param('storyId') storyId: string) {
    return this.videosService.deleteShelterStory(req.user.id, storyId);
  }

  @UseGuards(JwtAuthGuard)
  @Post('reports/videos/:videoId')
  reportVideo(@Request() req: any, @Param('videoId') videoId: string, @Body() dto: any) {
    return this.videosService.reportVideo(req.user.id, videoId, dto);
  }

  @UseGuards(JwtAuthGuard)
  @Post('reports/animals/:animalId')
  reportAnimal(@Request() req: any, @Param('animalId') animalId: string, @Body() dto: any) {
    return this.videosService.reportAnimal(req.user.id, animalId, dto);
  }
}
