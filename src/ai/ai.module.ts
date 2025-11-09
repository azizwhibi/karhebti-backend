import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { AiService } from './ai.service';
import { AiController } from './ai.controller';
import { RoadIssue, RoadIssueSchema } from './schemas/road-issue.schema';
import { CarsModule } from '../cars/cars.module';
import { GaragesModule } from '../garages/garages.module';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: RoadIssue.name, schema: RoadIssueSchema }]),
    CarsModule,
    GaragesModule,
  ],
  controllers: [AiController],
  providers: [AiService],
})
export class AiModule {}
