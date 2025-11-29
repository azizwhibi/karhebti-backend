import { Injectable, NotFoundException, BadRequestException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { RepairBay, RepairBayDocument } from './schemas/repair-bay.schema';
import { CreateRepairBayDto } from './dto/create-repair-bay.dto';
import { UpdateRepairBayDto } from './dto/update-repair-bay.dto';

@Injectable()
export class RepairBaysService {
  constructor(
    @InjectModel(RepairBay.name) private repairBayModel: Model<RepairBayDocument>,
    @InjectModel('Reservation') private reservationModel: Model<any>,
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
  
    // ✅ NOUVEAU: Exclure les réservations annulées
    // Ne considérer que: en_attente, confirmé, en_cours, terminé
    const reservedBayIds = await reservationModel
      .find({
        garageId: new Types.ObjectId(garageId),
        date: {
          $gte: startOfDay,
          $lte: endOfDay
        },
        status: { 
          $in: ['en_attente', 'confirmé', 'en_cours', 'terminé'] // ✅ Exclure 'annulé'
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
}
