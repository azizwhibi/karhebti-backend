import { Injectable, Logger } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { EventEmitter2 } from '@nestjs/event-emitter';
import { Notification, NotificationStatus, NotificationType } from './schemas/notification.schema';
import { SendNotificationDto, UpdateDeviceTokenDto } from './dto/notification.dto';
import { CreateNotificationDto } from './dto/create-notification.dto';
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
    private eventEmitter: EventEmitter2,
  ) {
    this.initializeServices();
  }

  private initializeServices() {
    try {
      initializeFirebase();
      this.firebaseMessaging = getFirebaseMessaging();
      this.logger.log('[translate:✅ Firebase initialisé avec succès]');
    } catch (error) {
      this.logger.warn('[translate:⚠️ Firebase non disponible:]', error);
    }
  }

  /**
   * [translate:Envoyer une notification via Firebase Cloud Messaging]
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

      this.logger.log(`[translate:✅ Notification Firebase envoyée:] ${response}`);
      return response;
    } catch (error) {
      this.logger.error(`[translate:❌ Erreur lors de l'envoi Firebase:] ${error.message}`);
      throw error;
    }
  }

  /**
   * [translate:Envoyer une notification complète (enregistrement + envoi Firebase)]
   * ✅ FIX: Automatically map titre → title to satisfy schema validation
   */
  async sendNotification(
    sendNotificationDto: SendNotificationDto,
  ): Promise<Notification> {
    const {
      userId,
      titre,
      title,
      message,
      type,
      deviceToken,
      documentId,
      maintenanceId,
      reservationId,
      data,
    } = sendNotificationDto;

    // ✅ FIX: Use title if provided, otherwise fallback to titre
    // This ensures the required 'title' field is always present
    const finalTitle = title || titre;

    if (!finalTitle) {
      throw new Error('[translate:Le champ title ou titre est requis]');
    }

    // [translate:Créer l'enregistrement de notification]
    const notificationRecord = await this.notificationModel.create({
      userId: new Types.ObjectId(userId),
      type,
      title: finalTitle,        // ✅ Required field
      titre: titre || finalTitle, // ✅ Optional French field
      message,
      deviceToken,
      documentId: documentId ? new Types.ObjectId(documentId) : undefined,
      maintenanceId: maintenanceId ? new Types.ObjectId(maintenanceId) : undefined,
      reservationId: reservationId ? new Types.ObjectId(reservationId) : undefined,
      data,
      status: NotificationStatus.PENDING,
    });

    try {
      // [translate:Envoyer via Firebase]
      if (deviceToken) {
        try {
          const messageId = await this.sendFirebaseNotification(
            deviceToken,
            finalTitle,
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
   * Create a notification (used by system/admin)
   */
  async createNotification(createNotificationDto: CreateNotificationDto) {
    const notification = await this.notificationModel.create(createNotificationDto);

    // Emit event for real-time notification via WebSocket
    this.eventEmitter.emit('notification.created', {
      userId: notification.userId.toString(),
      notification,
    });

    return notification;
  }

  /**
   * Get unread count for backward compatibility
   */
  async getUnreadCount(userId: string) {
    const count = await this.countUnreadNotifications(userId);
    return { count };
  }

  /**
   * [translate:Mettre à jour le device token d'un utilisateur]
   */
  async updateDeviceToken(
    userId: string,
    updateDeviceTokenDto: UpdateDeviceTokenDto,
  ): Promise<void> {
    const { deviceToken } = updateDeviceTokenDto;
    await this.usersService.updateDeviceToken(userId, deviceToken);
    this.logger.log(`[translate:Device token mis à jour pour l'utilisateur:] ${userId}`);
  }

  /**
   * [translate:Récupérer les notifications d'un utilisateur]
   */
  async getUserNotifications(
    userId: string,
    unreadOnly: boolean = false,
    limit: number = 20,
    skip: number = 0,
  ): Promise<Notification[]> {
    const query: any = { userId: new Types.ObjectId(userId) };
    if (unreadOnly) {
      query.status = { $ne: NotificationStatus.READ };
    }
    return this.notificationModel
      .find(query)
      .populate('fromUserId', 'nom prenom email')
      .sort({ createdAt: -1 })
      .limit(limit)
      .skip(skip)
      .exec();
  }

  /**
   * [translate:Marquer une notification comme lue]
   */
  async markAsRead(notificationId: string, userId?: string): Promise<Notification | null> {
    const query: any = { _id: new Types.ObjectId(notificationId) };
    if (userId) {
      query.userId = new Types.ObjectId(userId);
    }

    return this.notificationModel.findOneAndUpdate(
      query,
      {
        status: NotificationStatus.READ,
        read: true,
        readAt: new Date(),
      },
      { new: true },
    );
  }

  /**
   * [translate:Marquer toutes les notifications d'un utilisateur comme lues]
   */
  async markAllAsRead(userId: string): Promise<any> {
    return this.notificationModel.updateMany(
      {
        userId: new Types.ObjectId(userId),
        status: { $ne: NotificationStatus.READ },
      },
      {
        status: NotificationStatus.READ,
        read: true,
        readAt: new Date(),
      },
    );
  }

  /**
   * [translate:Récupérer les notifications non lues]
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
   * [translate:Compter les notifications non lues]
   */
  async countUnreadNotifications(userId: string): Promise<number> {
    return this.notificationModel.countDocuments({
      userId: new Types.ObjectId(userId),
      status: { $ne: NotificationStatus.READ },
    });
  }

  /**
   * [translate:Supprimer une notification]
   */
  async deleteNotification(notificationId: string): Promise<any> {
    return this.notificationModel.findByIdAndDelete(
      new Types.ObjectId(notificationId),
    );
  }

  /**
   * [translate:Supprimer toutes les notifications d'un utilisateur]
   */
  async deleteAllUserNotifications(userId: string): Promise<any> {
    return this.notificationModel.deleteMany({
      userId: new Types.ObjectId(userId),
    });
  }

  async sendReservationCancelledNotification(
    userId: string,
    reservationId: string,
    garageName: string,
    date: Date,
    heureDebut: string,
    heureFin: string,
  ): Promise<void> {
    try {
      const user = await this.usersService.findById(userId);
  
      if (!user) {
        this.logger.warn(`User ${userId} not found for notification`);
        return;
      }
  
      const formattedDate = new Date(date).toLocaleDateString('fr-FR', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric',
      });
  
      const titre = '🚫 Réservation annulée';
      const message =
        `Votre réservation chez ${garageName} le ${formattedDate} de ${heureDebut} à ${heureFin} ` +
        `a été annulée automatiquement, car aucun créneau de réparation n’était disponible à cette heure. ` +
        `Veuillez choisir une autre date ou un autre horaire.`;
  
      await this.sendNotification({
        userId,
        type: NotificationType.RESERVATION_CANCELLED,
        titre,
        title: titre,
        message,
        deviceToken: user.deviceToken,
        reservationId,
        data: {
          reservationId,
          garageName,
          date: date.toISOString(),
          heureDebut,
          heureFin,
          reason: 'no_repair_bay_available',
          action: 'reservation_cancelled',
        },
      });
  
      this.logger.log(`✅ Notification d'annulation envoyée à l'utilisateur ${userId}`);
    } catch (error) {
      this.logger.error(`❌ Erreur lors de l'envoi de notification d'annulation: ${error.message}`);
    }
  }
  

  /**
   * [translate:Envoyer une notification de réservation confirmée]
   */
  async sendReservationConfirmedNotification(
    userId: string,
    reservationId: string,
    garageName: string,
    date: Date,
    heureDebut: string,
    heureFin: string,
  ): Promise<void> {
    try {
      const user = await this.usersService.findById(userId);

      if (!user) {
        this.logger.warn(`User ${userId} not found for notification`);
        return;
      }

      const formattedDate = new Date(date).toLocaleDateString('fr-FR', {
        weekday: 'long',
        year: 'numeric',
        month: 'long',
        day: 'numeric'
      });

      const titre = '[translate:✅ Réservation confirmée]';
      const message = `[translate:Votre réservation chez] ${garageName} [translate:le] ${formattedDate} [translate:de] ${heureDebut} [translate:à] ${heureFin} [translate:a été confirmée !]`;

      await this.sendNotification({
        userId,
        type: NotificationType.RESERVATION_CONFIRMED,
        titre,
        title: titre, // ✅ Added for compatibility
        message,
        deviceToken: user.deviceToken,
        reservationId,
        data: {
          reservationId,
          garageName,
          date: date.toISOString(),
          heureDebut,
          heureFin,
          action: 'reservation_confirmed',
        },
      });

      this.logger.log(`[translate:✅ Notification de confirmation envoyée à l'utilisateur] ${userId}`);
    } catch (error) {
      this.logger.error(`[translate:❌ Erreur lors de l'envoi de notification de confirmation:] ${error.message}`);
    }
  }
}
