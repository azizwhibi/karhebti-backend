import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Schema as MongooseSchema } from 'mongoose';

export type CarDocument = Car & Document;

@Schema({ timestamps: true })
export class Car {
  @Prop({ required: true })
  marque: string;
  //

  @Prop({ required: true })
  modele: string;

  @Prop({ required: true })
  annee: number;

  @Prop({ required: true })
  immatriculation: string;

  @Prop({ required: true })
  typeCarburant: string;

  @Prop({ type: MongooseSchema.Types.ObjectId, ref: 'User', required: true })
  user: MongooseSchema.Types.ObjectId;
}

export const CarSchema = SchemaFactory.createForClass(Car);
