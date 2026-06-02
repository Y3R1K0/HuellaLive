import { Controller, Post, Body, HttpCode, UseGuards, Request } from '@nestjs/common';
import { AuthService } from './auth.service';
import { LoginDto, RegisterHumanDto, RegisterShelterDto, LinkAnimalDto } from './dto/auth.dto';
import { JwtAuthGuard } from './jwt-auth.guard';

@Controller('auth')
export class AuthController {
  constructor(private readonly authService: AuthService) {}

  @Post('register/human')
  registerHuman(@Body() dto: RegisterHumanDto) {
    return this.authService.registerHuman(dto);
  }

  @Post('register/shelter')
  registerShelter(@Body() dto: RegisterShelterDto) {
    return this.authService.registerShelter(dto);
  }

  @Post('login')
  @HttpCode(200)
  login(@Body() dto: LoginDto) {
    return this.authService.login(dto);
  }

  @Post('link-animal')
  @UseGuards(JwtAuthGuard)
  linkAnimal(@Request() req: any, @Body() dto: LinkAnimalDto) {
    return this.authService.linkAnimal(req.user.id, dto);
  }
}