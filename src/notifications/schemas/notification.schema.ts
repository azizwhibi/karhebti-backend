import { Prop, Schema, SchemaFactory } from '@nestjs/mongoose';
import { Document, Types } from 'mongoose';

export enum NotificationType {
  DOCUMENT_EXPIRATION = 'document_expiration',
  MAINTENANCE_REMINDER = 'maintenance_reminder',
  SERVICE_INFO = 'service_info',
  ALERT = 'alert',
  CUSTOM = 'custom',
}

export enum NotificationStatus {
  PENDING = 'pending',
  SENT = 'sent',
  FAILED = 'failed',
  READ = 'read',
}

@Schema({ timestamps: true })
export class Notification extends Document {
  @Prop({ required: true, type: Types.ObjectId, ref: 'User' })
  userId: Types.ObjectId;

  @Prop({ enum: NotificationType, required: true })
  type: NotificationType;

  @Prop({ required: true })
  titre: string;

  @Prop({ required: true })
  message: string;

  @Prop({ enum: NotificationStatus, default: NotificationStatus.PENDING })
  status: NotificationStatus;

  @Prop()
  deviceToken?: string;

  @Prop({ type: Types.ObjectId, ref: 'Document' })
  documentId?: Types.ObjectId;

  @Prop({ type: Types.ObjectId, ref: 'Maintenance' })
  maintenanceId?: Types.ObjectId;

  @Prop({ type: Object })
  data?: Record<string, any>;

  @Prop()
  sentAt?: Date;

  @Prop()
  readAt?: Date;

  @Prop()
  errorMessage?: string;

  createdAt: Date;
  updatedAt: Date;
}

export const NotificationSchema = SchemaFactory.createForClass(Notification);

