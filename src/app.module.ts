import { Module } from '@nestjs/common';
import { ConfigModule } from '@nestjs/config';
import { MongooseModule } from '@nestjs/mongoose';
import { ThrottlerModule } from '@nestjs/throttler';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { AuthModule } from './auth/auth.module';
import { UsersModule } from './users/users.module';
import { CarsModule } from './cars/cars.module';
import { MaintenancesModule } from './maintenances/maintenances.module';
import { PartsModule } from './parts/parts.module';
import { ReplacementHistoryModule } from './replacement-history/replacement-history.module';
import { DocumentsModule } from './documents/documents.module';
import { DeadlinesModule } from './deadlines/deadlines.module';
import { GaragesModule } from './garages/garages.module';
import { ServicesModule } from './services/services.module';
import { AiModule } from './ai/ai.module';
import { TranslationModule } from './translation/translation.module';

@Module({
  imports: [
    // Configuration des variables d'environnement
    ConfigModule.forRoot({
      isGlobal: true, // Rend les variables d'environnement accessibles partout
      envFilePath: '.env',
    }),
    
    // Configuration MongoDB
    MongooseModule.forRoot(
      process.env.MONGODB_URI || 'mongodb://localhost:27017/karhebti',
    ),
    
    // Rate Limiting
    ThrottlerModule.forRoot([{
      ttl: 60000,
      limit: 100,
    }]),

    // Modules métier
    AuthModule,
    UsersModule,
    CarsModule,
    MaintenancesModule,
    PartsModule,
    ReplacementHistoryModule,
    DocumentsModule,
    DeadlinesModule,
    GaragesModule,
    ServicesModule,
    AiModule,
    TranslationModule,
  ],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
