import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Schema as MongooseSchema } from 'mongoose';

export type DeadlineDocument = Deadline & Document;

@Schema({ timestamps: true })
export class Deadline {
  @Prop({ required: true })
  dateRappel: Date;

  @Prop({ required: true })
  typeNotification: string;

  @Prop({ required: true, enum: ['envoyé', 'reçu'], default: 'envoyé' })
  etat: string;

  @Prop({ type: MongooseSchema.Types.ObjectId, ref: 'DocumentEntity', required: true })
  document: MongooseSchema.Types.ObjectId;
}

export const DeadlineSchema = SchemaFactory.createForClass(Deadline);
