import { Controller, Post, Body, HttpCode, UseGuards, Request } from '@nestjs/common';
import { AuthService } from './auth.service';
import { FirebaseLoginDto, LoginDto, RegisterHumanDto, RegisterShelterDto, LinkAnimalDto } from './dto/auth.dto';
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

  @Post('firebase')
  @HttpCode(200)
  loginWithFirebase(@Body() dto: FirebaseLoginDto) {
    return this.authService.loginWithFirebase(dto.idToken);
  }

  @Post('firebase/preview')
  @HttpCode(200)
  previewFirebaseIdentity(@Body() dto: FirebaseLoginDto) {
    return this.authService.previewFirebaseIdentity(dto.idToken);
  }

  @Post('link-animal')
  @UseGuards(JwtAuthGuard)
  linkAnimal(@Request() req: any, @Body() dto: LinkAnimalDto) {
    return this.authService.linkAnimal(req.user.id, dto);
  }
}
