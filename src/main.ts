import { NestFactory } from '@nestjs/core';
import { ValidationPipe } from '@nestjs/common';
import { SwaggerModule, DocumentBuilder } from '@nestjs/swagger';
import { AppModule } from './app.module';

async function bootstrap() {
  const app = await NestFactory.create(AppModule);

  // Validation globale des DTOs
  app.useGlobalPipes(
    new ValidationPipe({
      whitelist: true,
      forbidNonWhitelisted: true,
      transform: true,
    }),
  );

  // Configuration CORS
  app.enableCors({
    origin: process.env.FRONTEND_URL || 'http://localhost:3001',
    credentials: true,
  });

  // Configuration Swagger
  const config = new DocumentBuilder()
    .setTitle('Karhebti API')
    .setDescription('Backend REST complet pour la gestion automobile avec NestJS, MongoDB et IA')
    .setVersion('1.0')
    .addBearerAuth()
    .addTag('Authentication', 'Endpoints d\'authentification (signup, login, forgot/reset password)')
    .addTag('Users', 'Gestion des utilisateurs (CRUD, rôles)')
    .addTag('Cars', 'Gestion des voitures')
    .addTag('Maintenances', 'Gestion des entretiens')
    .addTag('Parts', 'Gestion des pièces')
    .addTag('Replacement History', 'Historique de remplacement des pièces')
    .addTag('Documents', 'Gestion des documents (assurance, carte grise, contrôle technique)')
    .addTag('Deadlines', 'Gestion des échéances et rappels')
    .addTag('Garages', 'Gestion des garages (Admin)')
    .addTag('Services', 'Services proposés par les garages (Admin)')
    .addTag('AI Features', 'Fonctionnalités IA (détection route, recommandations)')
    .build();

  const document = SwaggerModule.createDocument(app, config);
  SwaggerModule.setup('api', app, document);

  const port = process.env.PORT || 3000;
  await app.listen(port);

  console.log(`\n🚀 Application démarrée sur http://localhost:${port}`);
  console.log(`📚 Documentation Swagger: http://localhost:${port}/api\n`);
}
bootstrap();
