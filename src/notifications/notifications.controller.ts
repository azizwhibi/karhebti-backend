import {
  Controller,
  Post,
  Get,
  Patch,
  Delete,
  Body,
  Param,
  UseGuards,
  Request,
  Query,
  HttpException,
  HttpStatus,
} from '@nestjs/common';
import { NotificationsService } from './notifications.service';
import { SendNotificationDto, UpdateDeviceTokenDto } from './dto/notification.dto';
import { JwtAuthGuard } from '../common/guards/jwt-auth.guard';

@Controller('notifications')
@UseGuards(JwtAuthGuard)
export class NotificationsController {
  constructor(private readonly notificationsService: NotificationsService) {}

  /**
   * Envoyer une notification (Firebase uniquement)
   * POST /notifications/send
   */
  @Post('send')
  async sendNotification(
    @Body() sendNotificationDto: SendNotificationDto,
    @Request() req: any,
  ) {
    try {
      const notification = await this.notificationsService.sendNotification(
        sendNotificationDto,
      );
      return {
        success: true,
        message: 'Notification envoyée avec succès',
        data: notification,
      };
    } catch (error) {
      throw new HttpException(
        {
          success: false,
          message: error.message || 'Erreur lors de l\'envoi de la notification',
        },
        HttpStatus.BAD_REQUEST,
      );
    }
  }

  /**
   * Récupérer les notifications de l'utilisateur connecté
   * GET /notifications
   */
  @Get()
  async getUserNotifications(
    @Request() req: any,
    @Query('limit') limit: string = '20',
    @Query('skip') skip: string = '0',
  ) {
    try {
      const userId = req.user.userId;
      const notifications = await this.notificationsService.getUserNotifications(
        userId,
        parseInt(limit),
        parseInt(skip),
      );
      const unreadCount = await this.notificationsService.countUnreadNotifications(
        userId,
      );
      
      return {
        success: true,
        data: notifications,
        metadata: {
          total: notifications.length,
          unreadCount,
        },
      };
    } catch (error) {
      throw new HttpException(
        {
          success: false,
          message: error.message || 'Erreur lors de la récupération des notifications',
        },
        HttpStatus.BAD_REQUEST,
      );
    }
  }

  /**
   * Récupérer les notifications non lues
   * GET /notifications/unread
   */
  @Get('unread')
  async getUnreadNotifications(@Request() req: any) {
    try {
      const userId = req.user.userId;
      const notifications = await this.notificationsService.getUnreadNotifications(
        userId,
      );
      
      return {
        success: true,
        data: notifications,
        count: notifications.length,
      };
    } catch (error) {
      throw new HttpException(
        {
          success: false,
          message: error.message || 'Erreur lors de la récupération des notifications non lues',
        },
        HttpStatus.BAD_REQUEST,
      );
    }
  }

  /**
   * Compter les notifications non lues
   * GET /notifications/unread-count
   */
  @Get('unread-count')
  async countUnreadNotifications(@Request() req: any) {
    try {
      const userId = req.user.userId;
      const count = await this.notificationsService.countUnreadNotifications(
        userId,
      );
      
      return {
        success: true,
        count,
      };
    } catch (error) {
      throw new HttpException(
        {
          success: false,
          message: error.message,
        },
        HttpStatus.BAD_REQUEST,
      );
    }
  }

  /**
   * Marquer une notification comme lue
   * PATCH /notifications/:id/read
   */
  @Patch(':id/read')
  async markAsRead(@Param('id') notificationId: string) {
    try {
      const notification = await this.notificationsService.markAsRead(
        notificationId,
      );
      
      return {
        success: true,
        message: 'Notification marquée comme lue',
        data: notification,
      };
    } catch (error) {
      throw new HttpException(
        {
          success: false,
          message: error.message || 'Erreur lors de la mise à jour de la notification',
        },
        HttpStatus.BAD_REQUEST,
      );
    }
  }

  /**
   * Marquer toutes les notifications comme lues
   * PATCH /notifications/mark-all-read
   */
  @Patch('mark-all-read')
  async markAllAsRead(@Request() req: any) {
    try {
      const userId = req.user.userId;
      await this.notificationsService.markAllAsRead(userId);
      
      return {
        success: true,
        message: 'Toutes les notifications ont été marquées comme lues',
      };
    } catch (error) {
      throw new HttpException(
        {
          success: false,
          message: error.message,
        },
        HttpStatus.BAD_REQUEST,
      );
    }
  }

  /**
   * Mettre à jour le device token
   * POST /notifications/update-device-token
   */
  @Post('update-device-token')
  async updateDeviceToken(
    @Body() updateDeviceTokenDto: UpdateDeviceTokenDto,
    @Request() req: any,
  ) {
    try {
      const userId = req.user.userId;
      await this.notificationsService.updateDeviceToken(
        userId,
        updateDeviceTokenDto,
      );
      
      return {
        success: true,
        message: 'Device token mis à jour avec succès',
      };
    } catch (error) {
      throw new HttpException(
        {
          success: false,
          message: error.message,
        },
        HttpStatus.BAD_REQUEST,
      );
    }
  }

  /**
   * Supprimer une notification
   * DELETE /notifications/:id
   */
  @Delete(':id')
  async deleteNotification(@Param('id') notificationId: string) {
    try {
      await this.notificationsService.deleteNotification(notificationId);
      
      return {
        success: true,
        message: 'Notification supprimée avec succès',
      };
    } catch (error) {
      throw new HttpException(
        {
          success: false,
          message: error.message,
        },
        HttpStatus.BAD_REQUEST,
      );
    }
  }

  /**
   * Supprimer toutes les notifications
   * DELETE /notifications
   */
  @Delete()
  async deleteAllNotifications(@Request() req: any) {
    try {
      const userId = req.user.userId;
      await this.notificationsService.deleteAllUserNotifications(userId);
      
      return {
        success: true,
        message: 'Toutes les notifications ont été supprimées',
      };
    } catch (error) {
      throw new HttpException(
        {
          success: false,
          message: error.message,
        },
        HttpStatus.BAD_REQUEST,
      );
    }
  }
}
