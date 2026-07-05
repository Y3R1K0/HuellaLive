import { Controller, Delete, Param, Post, Request, UseGuards } from '@nestjs/common';
import { JwtAuthGuard } from '../auth/jwt-auth.guard';
import { SocialService } from './social.service';

@Controller('follows')
@UseGuards(JwtAuthGuard)
export class SocialController {
  constructor(private readonly socialService: SocialService) {}

  @Post(':shelterId')
  follow(@Request() req: any, @Param('shelterId') shelterId: string) {
    return this.socialService.followShelter(req.user.id, shelterId);
  }

  @Delete(':shelterId')
  unfollow(@Request() req: any, @Param('shelterId') shelterId: string) {
    return this.socialService.unfollowShelter(req.user.id, shelterId);
  }
}
