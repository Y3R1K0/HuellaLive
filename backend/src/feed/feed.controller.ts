import { Controller, Get, Param, Post, Query, Request, UseGuards } from '@nestjs/common';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { OptionalJwtAuthGuard } from '../auth/optional-jwt-auth.guard';
import { FeedService } from './feed.service';

@Controller('feed')
export class FeedController {
  constructor(private readonly feedService: FeedService) {}

  @UseGuards(OptionalJwtAuthGuard)
  @Get()
  getFeed(
    @Request() req: any,
    @Query('page') page: string = '1',
    @Query('limit') limit: string = '10',
  ) {
    return this.feedService.getFeed(parseInt(page), parseInt(limit), req.user?.id);
  }

  @UseGuards(JwtAuthGuard)
  @Post('viewed/:videoId')
  markViewed(@Request() req: any, @Param('videoId') videoId: string) {
    return this.feedService.markViewed(req.user.id, videoId);
  }

  @UseGuards(JwtAuthGuard)
  @Get('history')
  history(
    @Request() req: any,
    @Query('page') page: string = '1',
    @Query('limit') limit: string = '40',
  ) {
    return this.feedService.getViewedHistory(req.user.id, parseInt(page), parseInt(limit));
  }
}
