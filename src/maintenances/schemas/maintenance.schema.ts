import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Schema as MongooseSchema } from 'mongoose';

export type MaintenanceDocument = Maintenance & Document;

@Schema({ timestamps: true })
export class Maintenance {
  @Prop({ required: true, enum: ['vidange', 'révision', 'réparation'] })
  type: string;

  @Prop({ required: true })
  date: Date;

  @Prop({ required: true })
  cout: number;

  @Prop({ type: MongooseSchema.Types.ObjectId, ref: 'Garage', required: true })
  garage: MongooseSchema.Types.ObjectId;

  @Prop({ type: MongooseSchema.Types.ObjectId, ref: 'Car', required: true })
  voiture: MongooseSchema.Types.ObjectId;
}

export const MaintenanceSchema = SchemaFactory.createForClass(Maintenance);
