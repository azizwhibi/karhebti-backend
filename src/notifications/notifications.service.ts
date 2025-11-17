import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Notification, NotificationDocument } from './schemas/notification.schema';
import { CreateNotificationDto } from './dto/create-notification.dto';
import { EventEmitter2 } from '@nestjs/event-emitter';

@Injectable()
export class NotificationsService {
  constructor(
    @InjectModel(Notification.name)
    private notificationModel: Model<NotificationDocument>,
    private eventEmitter: EventEmitter2,
  ) {}

  async createNotification(createNotificationDto: CreateNotificationDto) {
    const notification = await this.notificationModel.create(createNotificationDto);

    // Emit event for real-time notification via WebSocket
    this.eventEmitter.emit('notification.created', {
      userId: notification.userId.toString(),
      notification,
    });

    return notification;
  }

  async getUserNotifications(userId: string, unreadOnly = false) {
    const query = unreadOnly ? { userId, read: false } : { userId };
    return this.notificationModel
      .find(query)
      .populate('fromUserId', 'nom prenom email')
      .sort({ createdAt: -1 })
      .limit(50);
  }

  async markAsRead(notificationId: string, userId: string) {
    const notification = await this.notificationModel.findOne({
      _id: notificationId,
      userId,
    });

    if (!notification) {
      throw new Error('Notification not found');
    }

    notification.read = true;
    await notification.save();

    return notification;
  }

  async markAllAsRead(userId: string) {
    await this.notificationModel.updateMany(
      { userId, read: false },
      { read: true },
    );

    return { success: true };
  }

  async getUnreadCount(userId: string) {
    const count = await this.notificationModel.countDocuments({
      userId,
      read: false,
    });

    return { count };
  }
}
