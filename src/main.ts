import { NestFactory } from '@nestjs/core';
import { ValidationPipe, Logger } from '@nestjs/common';
import { SwaggerModule, DocumentBuilder } from '@nestjs/swagger';
import { NestExpressApplication } from '@nestjs/platform-express';
import { join } from 'path';
import { AppModule } from './app.module';

async function bootstrap() {
  const logger = new Logger('Bootstrap');
  
  try {
    const app = await NestFactory.create<NestExpressApplication>(AppModule, {
      logger: ['log', 'error', 'warn'],
    });

    // Serve static files (uploaded images)
    app.useStaticAssets(join(process.cwd(), 'uploads'), {
      prefix: '/uploads/',
    });

    // Validation globale des DTOs
    app.useGlobalPipes(
      new ValidationPipe({
        whitelist: true,
        forbidNonWhitelisted: true,
        transform: true,
      }),
    );

    // Configuration CORS - Allow all origins for development
    app.enableCors({
      origin: true,
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
      .addTag('Breakdowns', 'Gestion des pannes')
      .addTag('Notifications', 'Système de notifications')
      .addTag('Garages', 'Gestion des garages (Admin)')
      .addTag('Services', 'Services proposés par les garages (Admin)')
      .addTag('AI Features', 'Fonctionnalités IA (détection route, recommandations)')
      .addTag('Marketplace', 'Marketplace de véhicules (swipes, conversations)')
      .build();

    const document = SwaggerModule.createDocument(app, config);
    SwaggerModule.setup('api', app, document);

    const port = process.env.PORT || 3000;
    const host = process.env.HOST || '0.0.0.0';
    
    await app.listen(port, host);

    logger.log(`🚀 Application démarrée avec succès`);
    logger.log(`📡 Serveur en écoute sur http://localhost:${port}`);
    logger.log(`📚 Documentation Swagger disponible sur http://localhost:${port}/api`);
    logger.log(`🌍 Environnement: ${process.env.NODE_ENV || 'development'}`);
  } catch (error) {
    logger.error('❌ Erreur au démarrage du serveur', error.stack);
    process.exit(1);
  }
}

bootstrap().catch((error) => {
  const logger = new Logger('Bootstrap');
  logger.error('❌ Erreur fatale lors du démarrage', error.stack);
  process.exit(1);
});
