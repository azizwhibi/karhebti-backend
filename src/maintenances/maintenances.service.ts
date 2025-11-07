import { Injectable, NotFoundException, ForbiddenException, BadRequestException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Maintenance, MaintenanceDocument } from './schemas/maintenance.schema';
import { CreateMaintenanceDto } from './dto/create-maintenance.dto';
import { UpdateMaintenanceDto } from './dto/update-maintenance.dto';
import { CarsService } from '../cars/cars.service';

@Injectable()
export class MaintenancesService {
  constructor(
    @InjectModel(Maintenance.name) private maintenanceModel: Model<MaintenanceDocument>,
    private carsService: CarsService,
  ) {}

  async create(createMaintenanceDto: CreateMaintenanceDto, userId: string, userRole: string): Promise<Maintenance> {
    // Vérifier que la voiture appartient à l'utilisateur
    await this.carsService.findOne(createMaintenanceDto.voiture, userId, userRole);
    
    const createdMaintenance = new this.maintenanceModel(createMaintenanceDto);
    return createdMaintenance.save();
  }

  async findAll(userId: string, userRole: string): Promise<Maintenance[]> {
    if (userRole === 'admin') {
      return this.maintenanceModel.find().populate('garage voiture').exec();
    }

    // Récupérer les voitures de l'utilisateur
    const userCars = await this.carsService.findByUser(userId);
    const carIds = userCars.map(car => (car as any)._id);

    return this.maintenanceModel.find({ voiture: { $in: carIds } }).populate('garage voiture').exec();
  }

  async findOne(id: string, userId: string, userRole: string): Promise<Maintenance> {
    // Valider l'ID MongoDB
    if (!Types.ObjectId.isValid(id)) {
      throw new BadRequestException('ID d\'entretien invalide');
    }

    const maintenance = await this.maintenanceModel.findById(id).exec();
    if (!maintenance) {
      throw new NotFoundException('Entretien non trouvé');
    }

    if (userRole !== 'admin') {
      // Vérifier que la voiture appartient à l'utilisateur avant de peupler
      await this.carsService.findOne(maintenance.voiture.toString(), userId, userRole);
    }

    // Peupler après la vérification des permissions
    try {
      await maintenance.populate('garage voiture');
    } catch (error) {
      // Si le peuplement échoue (garage ou voiture introuvable), retourner sans peupler
      console.error('Erreur lors du peuplement:', error);
    }

    return maintenance;
  }

  async update(id: string, updateMaintenanceDto: UpdateMaintenanceDto, userId: string, userRole: string): Promise<Maintenance> {
    // Valider l'ID MongoDB
    if (!Types.ObjectId.isValid(id)) {
      throw new BadRequestException('ID d\'entretien invalide');
    }

    const maintenance = await this.maintenanceModel.findById(id).exec();
    if (!maintenance) {
      throw new NotFoundException('Entretien non trouvé');
    }

    if (userRole !== 'admin') {
      await this.carsService.findOne(maintenance.voiture.toString(), userId, userRole);
    }

    const updatedMaintenance = await this.maintenanceModel
      .findByIdAndUpdate(id, updateMaintenanceDto, { new: true })
      .exec();

    if (!updatedMaintenance) {
      throw new NotFoundException('Entretien non trouvé');
    }

    // Peupler après la mise à jour
    try {
      await updatedMaintenance.populate('garage voiture');
    } catch (error) {
      console.error('Erreur lors du peuplement:', error);
    }

    return updatedMaintenance;
  }

  async remove(id: string, userId: string, userRole: string): Promise<void> {
    // Valider l'ID MongoDB
    if (!Types.ObjectId.isValid(id)) {
      throw new BadRequestException('ID d\'entretien invalide');
    }

    const maintenance = await this.maintenanceModel.findById(id);
    if (!maintenance) {
      throw new NotFoundException('Entretien non trouvé');
    }

    if (userRole !== 'admin') {
      await this.carsService.findOne(maintenance.voiture.toString(), userId, userRole);
    }

    await this.maintenanceModel.findByIdAndDelete(id).exec();
  }
}
