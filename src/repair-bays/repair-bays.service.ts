import { Injectable, NotFoundException, BadRequestException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { RepairBay, RepairBayDocument } from './schemas/repair-bay.schema';
import { CreateRepairBayDto } from './dto/create-repair-bay.dto';
import { UpdateRepairBayDto } from './dto/update-repair-bay.dto';
import { NotificationsService } from '../notifications/notifications.service';
@Injectable()
export class RepairBaysService {
  constructor(
    @InjectModel(RepairBay.name) private repairBayModel: Model<RepairBayDocument>,
    @InjectModel('Reservation') private reservationModel: Model<any>,
    private readonly notificationsService: NotificationsService, 
  ) {}

  /**
   * Créer plusieurs créneaux pour un garage
   */
  async createMultipleBaysForGarage(
    garageId: string,
    numberOfBays: number,
    heureOuverture: string,
    heureFermeture: string
  ): Promise<RepairBay[]> {
    const bays: RepairBay[] = [];

    for (let i = 1; i <= numberOfBays; i++) {
      const bay = new this.repairBayModel({
        garageId: new Types.ObjectId(garageId),
        bayNumber: i,
        name: `Créneau ${i}`,
        heureOuverture,
        heureFermeture,
        isActive: true,
      });

      const saved = await bay.save();
      bays.push(saved);
    }

    return bays;
  }

  /**
   * Créer un seul créneau
   */
  async createRepairBay(
    garageId: string,
    bayNumber: number,
    name: string,
    heureOuverture: string,
    heureFermeture: string,
    isActive: boolean = true
  ): Promise<RepairBay> {
    if (!Types.ObjectId.isValid(garageId)) {
      throw new BadRequestException('ID garage invalide');
    }

    const bay = new this.repairBayModel({
      garageId: new Types.ObjectId(garageId),
      bayNumber,
      name,
      heureOuverture,
      heureFermeture,
      isActive,
    });

    return bay.save();
  }

  /**
   * Obtenir tous les créneaux d'un garage
   */
  async getBaysByGarage(garageId: string): Promise<RepairBay[]> {
    if (!Types.ObjectId.isValid(garageId)) {
      throw new BadRequestException('ID garage invalide');
    }

    return this.repairBayModel
      .find({ garageId: new Types.ObjectId(garageId) })
      .sort({ bayNumber: 1 })
      .exec();
  }

  /**
   * Obtenir les créneaux disponibles pour une date/heure
   */
  async getAvailableBays(
    garageId: string,
    date: Date,
    heureDebut: string,
    heureFin: string,
    reservationModel: Model<any>
  ): Promise<RepairBay[]> {
    if (!Types.ObjectId.isValid(garageId)) {
      throw new BadRequestException('ID garage invalide');
    }

    // Récupérer tous les créneaux actifs du garage
    const allBays = await this.repairBayModel
      .find({ garageId: new Types.ObjectId(garageId), isActive: true })
      .exec();

    // Normaliser la date (début de la journée)
    const startOfDay = new Date(date);
    startOfDay.setHours(0, 0, 0, 0);

    const endOfDay = new Date(date);
    endOfDay.setHours(23, 59, 59, 999);

    // ✅ CORRECTION IMPORTANTE: Seules les réservations CONFIRMÉES bloquent les créneaux
    // Les réservations en_attente ne bloquent PAS les créneaux (en attente de confirmation par propGarage)
    const reservedBayIds = await reservationModel
      .find({
        garageId: new Types.ObjectId(garageId),
        date: {
          $gte: startOfDay,
          $lte: endOfDay
        },
        status: {
          $in: ['confirmé', 'en_cours', 'terminé'] // ✅ RETIRÉ 'en_attente' - seules les confirmées bloquent
        },
        $or: [
          {
            heureDebut: { $lt: heureFin },
            heureFin: { $gt: heureDebut }
          }
        ]
      })
      .distinct('repairBayId')
      .exec();

    // Retourner les créneaux non réservés
    return allBays.filter(
      bay => !reservedBayIds.some(id => id.equals((bay as any)._id))
    );
  }

  /**
   * Obtenir un créneau par ID
   */
  async findOne(id: string): Promise<RepairBay> {
    if (!Types.ObjectId.isValid(id)) {
      throw new BadRequestException('ID invalide');
    }

    const bay = await this.repairBayModel.findById(id).exec();
    
    if (!bay) {
      throw new NotFoundException('Créneau non trouvé');
    }

    return bay;
  }

  /**
   * Mettre à jour un créneau
   */
  async updateBay(
    bayId: string,
    updateData: UpdateRepairBayDto
  ): Promise<RepairBay> {
    if (!Types.ObjectId.isValid(bayId)) {
      throw new BadRequestException('ID invalide');
    }

    const updated = await this.repairBayModel
      .findByIdAndUpdate(bayId, updateData, { new: true })
      .exec();

    if (!updated) {
      throw new NotFoundException('Créneau non trouvé');
    }

    return updated;
  }

  /**
   * Supprimer un créneau
   */
  async deleteBay(bayId: string): Promise<void> {
    if (!Types.ObjectId.isValid(bayId)) {
      throw new BadRequestException('ID invalide');
    }

    const result = await this.repairBayModel.findByIdAndDelete(bayId).exec();
    
    if (!result) {
      throw new NotFoundException('Créneau non trouvé');
    }
  }

  /**
   * Supprimer tous les créneaux d'un garage (cascade)
   */
  async deleteAllByGarage(garageId: string): Promise<void> {
    if (!Types.ObjectId.isValid(garageId)) {
      throw new BadRequestException('ID garage invalide');
    }

    const result = await this.repairBayModel.deleteMany({ 
      garageId: new Types.ObjectId(garageId) 
    }).exec();

    console.log(`Deleted ${result.deletedCount} repair bays for garage ${garageId}`);
  }

  /**
   * Compter le nombre de créneaux d'un garage
   */
  async countByGarage(garageId: string): Promise<number> {
    if (!Types.ObjectId.isValid(garageId)) {
      throw new BadRequestException('ID garage invalide');
    }

    return this.repairBayModel.countDocuments({
      garageId: new Types.ObjectId(garageId)
    }).exec();
  }

  /**
   * Activer/Désactiver un créneau
   */
  async toggleActive(bayId: string): Promise<RepairBay> {
    if (!Types.ObjectId.isValid(bayId)) {
      throw new BadRequestException('ID invalide');
    }

    const bay = await this.repairBayModel.findById(bayId).exec();
    
    if (!bay) {
      throw new NotFoundException('Créneau non trouvé');
    }

    bay.isActive = !bay.isActive;
    return bay.save();
  }

  // typescript
  async confirmReservation(reservationId: string): Promise<void> {
    const reservation = await this.reservationModel.findById(reservationId)
      .populate('userId', 'nom prenom email deviceToken')  // ✅ Populate userId avec deviceToken
      .populate('garageId', 'nom adresse')  // ✅ Populate garageId
      .exec();

    if (!reservation) {
      throw new NotFoundException('Réservation non trouvée');
    }

    if (reservation.status === 'confirmé') {
      throw new BadRequestException('Cette réservation est déjà confirmée');
    }

    if (reservation.status === 'annulé') {
      throw new BadRequestException('Impossible de confirmer une réservation annulée');
    }

    // Récupérer toutes les bays actives du garage
    const allBays = await this.repairBayModel.find({
      garageId: reservation.garageId._id || reservation.garageId,
      isActive: true
    }).exec();

    const totalBays = allBays.length;
    if (totalBays === 0) {
      throw new BadRequestException('Aucun créneau actif disponible dans ce garage');
    }

    // Normaliser la date pour la comparaison
    const reservationDate = new Date(reservation.date);
    const startOfDay = new Date(reservationDate);
    startOfDay.setHours(0, 0, 0, 0);
    const endOfDay = new Date(reservationDate);
    endOfDay.setHours(23, 59, 59, 999);

    // Obtenir les réservations CONFIRMÉES qui se chevauchent pour le même garage (toutes bays confondues)
    const overlappingConfirmedReservations = await this.reservationModel.find({
      _id: { $ne: reservationId },
      garageId: reservation.garageId._id || reservation.garageId,
      status: { $in: ['confirmé', 'en_cours'] },
      date: {
        $gte: startOfDay,
        $lte: endOfDay
      },
      $or: [
        {
          heureDebut: { $lt: reservation.heureFin },
          heureFin: { $gt: reservation.heureDebut }
        }
      ]
    }).exec();

    // Si la capacité est déjà remplie -> refus
    if (overlappingConfirmedReservations.length >= totalBays) {
      throw new BadRequestException('Capacité du garage atteinte pour cette période. Impossible de confirmer cette réservation.');
    }

    // Trouver les bayIds déjà occupées par des réservations confirmées
    const usedBayIds = new Set(overlappingConfirmedReservations.map(r => r.repairBayId?.toString()));

    // Choisir une bay libre (parmi les bays actives)
    const freeBay = allBays.find(b => !usedBayIds.has((b as any)._id.toString()));
    if (!freeBay) {
      throw new BadRequestException('Aucune bay disponible trouvée (incohérence)');
    }

    // ✅ ÉTAPE 1: D'abord, confirmer la réservation actuelle et assigner une bay
    reservation.repairBayId = (freeBay as any)._id;
    reservation.status = 'confirmé';
    await reservation.save();

    console.log(`✅ Réservation ${reservationId} confirmée et assignée à la bay ${freeBay.name}`);

    // ✅ ÉTAPE 2: Envoyer une notification de confirmation à l'utilisateur
    try {
      const garageName = reservation.garageId?.nom || 'le garage';

      await this.notificationsService.sendReservationConfirmedNotification(
        reservation.userId._id.toString(),
        reservation._id.toString(),
        garageName,
        reservation.date,
        reservation.heureDebut,
        reservation.heureFin,
      );
    } catch (notifError) {
      console.error(`❌ Erreur lors de l'envoi de notification de confirmation:`, notifError.message);
    }

    // ✅ ÉTAPE 3: APRÈS confirmation, vérifier si le garage est maintenant COMPLET
    const nowConfirmedCount = overlappingConfirmedReservations.length + 1; // +1 pour celle qu'on vient de confirmer

    // Si le garage est maintenant COMPLET, annuler TOUTES les réservations en_attente restantes
    if (nowConfirmedCount >= totalBays) {
      console.log(`⚠️ Garage COMPLET (${nowConfirmedCount}/${totalBays} bays occupées) - Annulation des réservations en_attente restantes...`);

      // Récupérer TOUTES les réservations EN_ATTENTE qui se chevauchent avec cette période
      const conflictingPendingReservations = await this.reservationModel.find({
        garageId: reservation.garageId._id || reservation.garageId,
        status: 'en_attente',
        date: {
          $gte: startOfDay,
          $lte: endOfDay
        },
        $or: [
          {
            heureDebut: { $lt: reservation.heureFin },
            heureFin: { $gt: reservation.heureDebut }
          }
        ]
      })
      .populate('userId', 'nom prenom email deviceToken')
      .populate('garageId', 'nom adresse')
      .exec();

      if (conflictingPendingReservations.length > 0) {
        // Annuler TOUTES les réservations en_attente (car le garage est plein)
        await this.reservationModel.updateMany(
          {
            _id: { $in: conflictingPendingReservations.map(r => r._id) }
          },
          {
            $set: {
              status: 'annulé',
              commentaires: `Annulée automatiquement - Capacité du garage atteinte (${totalBays} créneaux complets)`
            }
          }
        ).exec();

        console.log(`✅ ${conflictingPendingReservations.length} réservation(s) en attente annulée(s) automatiquement (garage complet)`);

        // ✅ Envoyer des notifications à TOUS les utilisateurs concernés
        for (const cancelledReservation of conflictingPendingReservations) {
          try {
            const garageName = cancelledReservation.garageId?.nom || 'le garage';

            await this.notificationsService.sendReservationCancelledNotification(
              cancelledReservation.userId._id.toString(),
              cancelledReservation._id.toString(),
              garageName,
              cancelledReservation.date,
              cancelledReservation.heureDebut,
              cancelledReservation.heureFin,
            );

            console.log(`✅ Notification d'annulation envoyée à l'utilisateur ${cancelledReservation.userId._id}`);
          } catch (notifError) {
            console.error(`❌ Erreur lors de l'envoi de notification pour la réservation ${cancelledReservation._id}:`, notifError.message);
          }
        }
      } else {
        console.log(`ℹ️ Garage complet mais aucune réservation en_attente à annuler`);
      }
    } else {
      console.log(`ℹ️ Garage pas encore complet (${nowConfirmedCount}/${totalBays} bays occupées) - Les réservations en_attente restent disponibles`);
    }
  }

  /**
 * ✅ NEW: Supprimer les créneaux dans une plage de bayNumber
 * Annule également les réservations associées
 */
async deleteBaysByNumberRange(
  garageId: string,
  minBayNumber: number,
  maxBayNumber: number
): Promise<void> {
  if (!Types.ObjectId.isValid(garageId)) {
    throw new BadRequestException('ID garage invalide');
  }

  console.log(`🔍 Searching for repair bays to delete (bayNumber ${minBayNumber}-${maxBayNumber}) for garage ${garageId}`);

  // Find all bays in the range
  const baysToDelete = await this.repairBayModel.find({
    garageId: new Types.ObjectId(garageId),
    bayNumber: { $gte: minBayNumber, $lte: maxBayNumber }
  }).exec();

  if (baysToDelete.length === 0) {
    console.log(`ℹ️ No repair bays found in range ${minBayNumber}-${maxBayNumber}`);
    return;
  }

  console.log(`🗑️ Found ${baysToDelete.length} repair bay(s) to delete`);

  const bayIds = baysToDelete.map(bay => (bay as any)._id);

  // ✅ Find all reservations using these bays
  const affectedReservations = await this.reservationModel.find({
    repairBayId: { $in: bayIds },
    status: { $in: ['en_attente', 'confirmé', 'en_cours'] }
  })
  .populate('userId', 'nom prenom email deviceToken')
  .populate('garageId', 'nom adresse')
  .exec();

  console.log(`📋 Found ${affectedReservations.length} active reservation(s) affected`);

  // ✅ Cancel all affected reservations
  if (affectedReservations.length > 0) {
    await this.reservationModel.updateMany(
      { repairBayId: { $in: bayIds } },
      {
        $set: {
          status: 'annulé',
          commentaires: `Annulée automatiquement - Créneau de réparation supprimé par le propriétaire du garage`
        }
      }
    ).exec();

    console.log(`✅ ${affectedReservations.length} reservation(s) cancelled`);

    // ✅ Send notifications to all affected users
    for (const reservation of affectedReservations) {
      try {
        const garageName = reservation.garageId?.nom || 'le garage';

        await this.notificationsService.sendReservationCancelledNotification(
          reservation.userId._id.toString(),
          reservation._id.toString(),
          garageName,
          reservation.date,
          reservation.heureDebut,
          reservation.heureFin,
        );

        console.log(`📲 Cancellation notification sent to user ${reservation.userId._id}`);
      } catch (notifError) {
        console.error(`❌ Error sending notification for reservation ${reservation._id}:`, notifError.message);
      }
    }
  }

  // ✅ Delete the repair bays
  const deleteResult = await this.repairBayModel.deleteMany({
    _id: { $in: bayIds }
  }).exec();

  console.log(`✅ Deleted ${deleteResult.deletedCount} repair bay(s) successfully`);
}


}
