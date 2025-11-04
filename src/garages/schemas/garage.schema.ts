import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document } from 'mongoose';

export type GarageDocument = Garage & Document;

@Schema({ timestamps: true })
export class Garage {
  @Prop({ required: true })
  nom: string;

  @Prop({ required: true })
  adresse: string;

  @Prop({ type: [String], required: true })
  typeService: string[];

  @Prop({ required: true })
  telephone: string;

  @Prop({ default: 0, min: 0, max: 5 })
  noteUtilisateur: number;
}

export const GarageSchema = SchemaFactory.createForClass(Garage);
