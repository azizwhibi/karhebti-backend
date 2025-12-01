
import { Injectable } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Breakdown, BreakdownType, BreakdownStatus } from './schemas/breakdown.schema';
import { NotificationType } from '../notifications/schemas/notification.schema';
import { CreateBreakdownDto } from './dto/create-breakdown.dto';
import { UpdateStatusDto } from './dto/update-status.dto';
import { NotificationsService } from '../notifications/notifications.service';
import { UsersService } from '../users/users.service';

/**
 * Service pour la gestion des pannes (MongoDB/Mongoose).
 */
@Injectable()
export class BreakdownsService {
  constructor(
    @InjectModel(Breakdown.name)
    private breakdownModel: Model<Breakdown>,
    private notificationsService: NotificationsService,
    private usersService: UsersService,
  ) {}

  /**
   * Liste paginée et filtrée des pannes (query params)
   */
  async findAll(params: { userId?: string; status?: string; limit?: number; offset?: number }): Promise<{ data: Breakdown[]; total: number; limit: number; offset: number }> {
    const { userId, status, limit = 20, offset = 0 } = params;
    const query: any = {};
    if (userId) query.userId = userId;
    if (status) query.status = status;
    const [data, total] = await Promise.all([
      this.breakdownModel.find(query).skip(Number(offset)).limit(Number(limit)).sort({ createdAt: -1 }).exec(),
      this.breakdownModel.countDocuments(query),
    ]);
    return { data, total, limit: Number(limit), offset: Number(offset) };
  }

  /**
   * Mise à jour partielle d'une panne (PATCH)
   */
  async patchBreakdown(id: string, dto: Partial<CreateBreakdownDto>): Promise<Breakdown | null> {
    await this.breakdownModel.findByIdAndUpdate(id, dto);
    return this.findById(id);
  }

  /**
   * Suppression/annulation d'une panne (DELETE)
   */
  async deleteBreakdown(id: string): Promise<{ message: string; id: string }> {
    await this.breakdownModel.findByIdAndDelete(id);
    return { message: 'Breakdown cancelled successfully', id };
  }

  /**
   * Crée une nouvelle panne.
   */
  async create(createDto: CreateBreakdownDto): Promise<Breakdown> {
    const created = new this.breakdownModel({ ...createDto, status: BreakdownStatus.PENDING });
    const saved = await created.save();
    // Récupérer le deviceToken de l'utilisateur
    const user = await this.usersService.findOne(createDto.userId);
    if (user && user.deviceToken) {
      await this.notificationsService.sendNotification({
        userId: user['userId']?.toString(),
        type: NotificationType.ALERT,
        titre: 'Demande SOS reçue',
        message: `Votre demande d'assistance (${saved.type}) a été enregistrée. Un technicien sera bientôt assigné.`,
        deviceToken: user.deviceToken,
        data: {
          type: 'sos_created',
          breakdownId: (saved.id ?? saved._id)?.toString(),
          status: saved.status,
          latitude: saved.latitude.toString(),
          longitude: saved.longitude.toString(),
        },
      });
    }
    return saved;
  }

  /**
   * Récupère une panne par son id.
   */
  async findById(id: string): Promise<Breakdown | null> {
    return this.breakdownModel.findById(id).exec();
  }

  /**
   * Récupère l'historique des pannes d'un utilisateur.
   */
  async findByUser(userId: string): Promise<Breakdown[]> {
    return this.breakdownModel.find({ userId }).sort({ createdAt: -1 }).exec();
  }

  /**
   * Met à jour le statut d'une panne.
   */
  async updateStatus(id: string, dto: UpdateStatusDto): Promise<Breakdown | null> {
    await this.breakdownModel.findByIdAndUpdate(id, { status: dto.status });
    const updated = await this.findById(id);
    if (updated) {
      let titre = '';
      let message = '';
      let deviceToken = '';
      let userId = '';
      // Selon le statut, on notifie l'utilisateur ou le garage
      if ([BreakdownStatus.ACCEPTED, BreakdownStatus.REFUSED].includes(dto.status)) {
        // Notifier l'utilisateur que le garage a accepté/refusé
        const user = await this.usersService.findOne(updated.userId);
        if (user && user.deviceToken) {
          deviceToken = user.deviceToken;
          userId = user['userId']?.toString();
          if (dto.status === BreakdownStatus.ACCEPTED) {
            titre = 'SOS accepté';
            message = 'Votre demande SOS a été acceptée. Vous pouvez contacter le garage.';
          } else if (dto.status === BreakdownStatus.REFUSED) {
            titre = 'SOS refusé';
            message = 'Votre demande SOS a été refusée par le garage.';
          }
        }
      } else {
        // Notifier l'utilisateur pour les autres statuts
        const user = await this.usersService.findOne(updated.userId);
        if (user && user.deviceToken) {
          deviceToken = user.deviceToken;
          userId = user['userId']?.toString();
          switch (dto.status) {
            case BreakdownStatus.IN_PROGRESS:
              titre = 'Assistance en route';
              message = 'Un technicien a été assigné et se dirige vers votre position.';
              break;
            case BreakdownStatus.COMPLETED:
              titre = 'Assistance terminée';
              message = "L'assistance a été effectuée avec succès. Merci d'avoir utilisé notre service.";
              break;
            case BreakdownStatus.CANCELLED:
              titre = 'Demande annulée';
              message = "Votre demande d'assistance a été annulée.";
              break;
          }
        }
      }
      if (titre && message && deviceToken) {
        await this.notificationsService.sendNotification({
          userId,
          type: NotificationType.ALERT,
          titre,
          message,
          deviceToken,
          data: {
            type: 'sos_status_updated',
            breakdownId: (updated.id ?? updated._id)?.toString(),
            status: dto.status,
          },
        });
      }
    }
    return updated;
  }

  /**
   * Assigne un agent à une panne.
   */
  async assignAgent(id: string, agentId: string): Promise<Breakdown | null> {
    await this.breakdownModel.findByIdAndUpdate(id, { assignedTo: agentId, status: 'ASSIGNED' });
    return this.findById(id);
  }
}
