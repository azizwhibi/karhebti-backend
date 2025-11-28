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
import { GaragesModule } from './garages/garages.module';
import { ServicesModule } from './services/services.module';
import { ReclamationsModule } from './reclamations/reclamations.module';
import { AiModule } from './ai/ai.module';
import { BreakdownsModule } from './breakdowns/breakdowns.module';
import { NotificationsModule } from './notifications/notifications.module';
import { FirebaseModule } from './firebase/firebase.module';

@Module({
  imports: [
    // Load environment variables from .env file
    ConfigModule.forRoot({
      isGlobal: true,
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

    // Firebase Configuration
    FirebaseModule,

    // Modules métier
    AuthModule,
    UsersModule,
    CarsModule,
    MaintenancesModule,
    PartsModule,
    ReplacementHistoryModule,
    DocumentsModule,
    GaragesModule,
    ServicesModule,
    ReclamationsModule,
    NotificationsModule,
    AiModule,
    BreakdownsModule,
  ],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
