import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { DeadlinesService } from './deadlines.service';
import { DeadlinesController } from './deadlines.controller';
import { Deadline, DeadlineSchema } from './schemas/deadline.schema';
import { DocumentsModule } from '../documents/documents.module';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: Deadline.name, schema: DeadlineSchema }]),
    DocumentsModule,
  ],
  controllers: [DeadlinesController],
  providers: [DeadlinesService],
})
export class DeadlinesModule {}
