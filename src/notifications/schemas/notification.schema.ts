import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Schema as MongooseSchema } from 'mongoose';

export type NotificationDocument = Notification & Document;

@Schema({ timestamps: true })
export class Notification {
  @Prop({ type: MongooseSchema.Types.ObjectId, ref: 'User', required: true })
  userId: MongooseSchema.Types.ObjectId;

  @Prop({ required: true })
  type: string; // 'swipe_right', 'swipe_accepted', 'swipe_declined', 'new_message'

  @Prop({ required: true })
  title: string;

  @Prop({ required: true })
  message: string;

  @Prop({ type: Object })
  data: any; // Additional data (e.g., carId, swipeId, conversationId)

  @Prop({ default: false })
  read: boolean;

  @Prop({ type: MongooseSchema.Types.ObjectId, ref: 'User' })
  fromUserId?: MongooseSchema.Types.ObjectId;
}

export const NotificationSchema = SchemaFactory.createForClass(Notification);
