import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Schema as MongooseSchema } from 'mongoose';

export type ServiceDocument = Service & Document;

@Schema({ timestamps: true })
export class Service {
  @Prop({ required: true, enum: ['vidange', 'contrôle technique', 'réparation pneu'] })
  type: string;

  @Prop({ required: true })
  coutMoyen: number;

  @Prop({ required: true })
  dureeEstimee: number;

  @Prop({ type: MongooseSchema.Types.ObjectId, ref: 'Garage', required: true })
  garage: MongooseSchema.Types.ObjectId;

  @Prop({ type: MongooseSchema.Types.ObjectId, ref: 'Car', required: true }) // <-- Add this!
  car: MongooseSchema.Types.ObjectId;
}


export const ServiceSchema = SchemaFactory.createForClass(Service);

