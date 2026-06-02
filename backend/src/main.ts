import { NestFactory } from '@nestjs/core';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);
  app.enableCors({ origin: '*' });
  await app.listen(process.env.PORT ?? 3000);
  console.log(`🐾 HuellaLive API corriendo en http://localhost:3000`);
}
bootstrap();