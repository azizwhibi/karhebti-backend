import { Injectable, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Notification, NotificationStatus, NotificationType } from './schemas/notification.schema';
import { SendNotificationDto, UpdateDeviceTokenDto } from './dto/notification.dto';
import { initializeFirebase, getFirebaseMessaging } from '../common/config/firebase.config';
import { UsersService } from '../users/users.service';

@Injectable()
export class NotificationsService {
  private readonly logger = new Logger(NotificationsService.name);
  private firebaseMessaging: any;

  constructor(
    @InjectModel(Notification.name)
    private notificationModel: Model<Notification>,
    private readonly usersService: UsersService,
  ) {
    this.initializeServices();
  }

  private initializeServices() {
    try {
      initializeFirebase();
      this.firebaseMessaging = getFirebaseMessaging();
      this.logger.log('✅ Firebase initialisé avec succès');
    } catch (error) {
      this.logger.warn('⚠️  Firebase non disponible:', error);
    }
  }

  /**
   * Envoyer une notification via Firebase Cloud Messaging
   */
  async sendFirebaseNotification(
    deviceToken: string,
    titre: string,
    message: string,
    data?: Record<string, any>,
  ): Promise<string> {
    if (!this.firebaseMessaging) {
      throw new Error('Firebase Cloud Messaging non disponible');
    }

    try {
      const response = await this.firebaseMessaging.send({
        token: deviceToken,
        notification: {
          title: titre,
          body: message,
        },
        data: data || {},
        android: {
          priority: 'high',
          notification: {
            clickAction: 'FLUTTER_NOTIFICATION_CLICK',
          },
        },
        apns: {
          payload: {
            aps: {
              alert: {
                title: titre,
                body: message,
              },
              badge: 1,
              sound: 'default',
            },
          },
        },
      });

      this.logger.log(`✅ Notification Firebase envoyée: ${response}`);
      return response;
    } catch (error) {
      this.logger.error(`❌ Erreur lors de l'envoi Firebase: ${error.message}`);
      throw error;
    }
  }

  /**
   * Envoyer une notification complète (enregistrement + envoi Firebase)
   */
  async sendNotification(
    sendNotificationDto: SendNotificationDto,
  ): Promise<Notification> {
    const {
      userId,
      titre,
      message,
      type,
      deviceToken,
      documentId,
      maintenanceId,
      data,
    } = sendNotificationDto;

    // Créer l'enregistrement de notification
    const notificationRecord = await this.notificationModel.create({
      userId: new Types.ObjectId(userId),
      type,
      titre,
      message,
      deviceToken,
      documentId: documentId ? new Types.ObjectId(documentId) : undefined,
      maintenanceId: maintenanceId ? new Types.ObjectId(maintenanceId) : undefined,
      data,
      status: NotificationStatus.PENDING,
    });

    try {
      // Envoyer via Firebase
      if (deviceToken) {
        try {
          const messageId = await this.sendFirebaseNotification(
            deviceToken,
            titre,
            message,
            data,
          );
          notificationRecord.status = NotificationStatus.SENT;
          notificationRecord.sentAt = new Date();
        } catch (error) {
          this.logger.error(`Firebase failed for notification: ${error.message}`);
          notificationRecord.errorMessage = error.message;
        }
      }

      await notificationRecord.save();
      return notificationRecord;
    } catch (error) {
      notificationRecord.status = NotificationStatus.FAILED;
      notificationRecord.errorMessage = error.message;
      await notificationRecord.save();
      throw error;
    }
  }

  /**
   * Mettre à jour le device token d'un utilisateur
   */
  async updateDeviceToken(
    userId: string,
    updateDeviceTokenDto: UpdateDeviceTokenDto,
  ): Promise<void> {
    const { deviceToken } = updateDeviceTokenDto;
    await this.usersService.updateDeviceToken(userId, deviceToken);
    this.logger.log(`Device token mis à jour pour l'utilisateur: ${userId}`);
  }

  /**
   * Récupérer les notifications d'un utilisateur
   */
  async getUserNotifications(
    userId: string,
    limit: number = 20,
    skip: number = 0,
  ): Promise<Notification[]> {
    return this.notificationModel
      .find({ userId: new Types.ObjectId(userId) })
      .sort({ createdAt: -1 })
      .limit(limit)
      .skip(skip)
      .exec();
  }

  /**
   * Marquer une notification comme lue
   */
  async markAsRead(notificationId: string): Promise<Notification | null> {
    return this.notificationModel.findByIdAndUpdate(
      new Types.ObjectId(notificationId),
      {
        status: NotificationStatus.READ,
        readAt: new Date(),
      },
      { new: true },
    );
  }

  /**
   * Marquer toutes les notifications d'un utilisateur comme lues
   */
  async markAllAsRead(userId: string): Promise<any> {
    return this.notificationModel.updateMany(
      {
        userId: new Types.ObjectId(userId),
        status: { $ne: NotificationStatus.READ },
      },
      {
        status: NotificationStatus.READ,
        readAt: new Date(),
      },
    );
  }

  /**
   * Récupérer les notifications non lues
   */
  async getUnreadNotifications(userId: string): Promise<Notification[]> {
    return this.notificationModel
      .find({
        userId: new Types.ObjectId(userId),
        status: { $ne: NotificationStatus.READ },
      })
      .sort({ createdAt: -1 })
      .exec();
  }

  /**
   * Compter les notifications non lues
   */
  async countUnreadNotifications(userId: string): Promise<number> {
    return this.notificationModel.countDocuments({
      userId: new Types.ObjectId(userId),
      status: { $ne: NotificationStatus.READ },
    });
  }

  /**
   * Supprimer une notification
   */
  async deleteNotification(notificationId: string): Promise<any> {
    return this.notificationModel.findByIdAndDelete(
      new Types.ObjectId(notificationId),
    );
  }

  /**
   * Supprimer toutes les notifications d'un utilisateur
   */
  async deleteAllUserNotifications(userId: string): Promise<any> {
    return this.notificationModel.deleteMany({
      userId: new Types.ObjectId(userId),
    });
  }
}
