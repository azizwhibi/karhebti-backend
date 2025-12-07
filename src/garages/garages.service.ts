import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Garage, GarageDocument } from './schemas/garage.schema';
import { CreateGarageDto } from './dto/create-garage.dto';
import { UpdateGarageDto } from './dto/update-garage.dto';
import { Service, ServiceDocument } from '../services/schemas/service.schema';
import { OsmService } from './osm.service';
import { RepairBaysService } from '../repair-bays/repair-bays.service';
import { ReservationsService } from '../reservation/reservations.service';  // ✅ ADDED
import { NotificationsService } from '../notifications/notifications.service';  // ✅ ADDED

@Injectable()
export class GaragesService {
  constructor(
    @InjectModel(Garage.name) private garageModel: Model<GarageDocument>,
    @InjectModel(Service.name) private serviceModel: Model<ServiceDocument>,
    private readonly osmService: OsmService,
    private readonly repairBaysService: RepairBaysService,
    private readonly reservationsService: ReservationsService,  // ✅ ADDED
    private readonly notificationsService: NotificationsService,  // ✅ ADDED
  ) {}

  async create(
    createDto: CreateGarageDto,
    numberOfBays: number = 1
  ): Promise<{ garage: Garage; repairBays: any[] }> {
    if (!createDto.latitude || !createDto.longitude) {
      const results = await this.osmService.searchAddress(createDto.adresse);
  
      if (!results || results.length === 0) {
        throw new NotFoundException('Adresse introuvable via OpenStreetMap');
      }
  
      const bestMatch = results[0];
      createDto.latitude = parseFloat(bestMatch.lat);
      createDto.longitude = parseFloat(bestMatch.lon);
    }
  
    const garageData = {
      ...createDto,
      numberOfBays: numberOfBays
    };
  
    const created = new this.garageModel(garageData);
    const savedGarage = await created.save();
    const garageId = (savedGarage._id as any).toString();
  
    const repairBays = await this.repairBaysService.createMultipleBaysForGarage(
      garageId,
      numberOfBays,
      createDto.heureOuverture,
      createDto.heureFermeture
    );
  
    return { garage: savedGarage, repairBays };
  }

  async findAll(): Promise<Garage[]> {
    return this.garageModel.find().exec();
  }

  async findOne(id: string): Promise<Garage> {
    const garage = await this.garageModel.findById(id).exec();
    if (!garage) throw new NotFoundException('Garage non trouvé');
    return garage;
  }

  // ✅ UPDATED: Handle numberOfBays decrease
  async update(id: string, updateDto: UpdateGarageDto): Promise<Garage> {
    // Get current garage to compare numberOfBays
    const currentGarage = await this.garageModel.findById(id).exec();
    if (!currentGarage) throw new NotFoundException('Garage non trouvé');

    const currentNumberOfBays = currentGarage.numberOfBays || 1;
    const newNumberOfBays = updateDto.numberOfBays;

    // ✅ If numberOfBays is being decreased, delete excess bays
    if (newNumberOfBays !== undefined && newNumberOfBays < currentNumberOfBays) {
      console.log(`⚠️ Decreasing repair bays from ${currentNumberOfBays} to ${newNumberOfBays}`);
      
      const baysToDelete = currentNumberOfBays - newNumberOfBays;
      console.log(`🗑️ Deleting ${baysToDelete} repair bay(s) with bayNumber > ${newNumberOfBays}`);

      // Delete bays with bayNumber > newNumberOfBays
      await this.repairBaysService.deleteBaysByNumberRange(
        id, 
        newNumberOfBays + 1, 
        currentNumberOfBays
      );

      console.log(`✅ Excess repair bays deleted successfully`);
    }

    // ✅ If numberOfBays is being increased, create new bays
    if (newNumberOfBays !== undefined && newNumberOfBays > currentNumberOfBays) {
      console.log(`➕ Increasing repair bays from ${currentNumberOfBays} to ${newNumberOfBays}`);
      
      const baysToAdd = newNumberOfBays - currentNumberOfBays;
      console.log(`✅ Creating ${baysToAdd} new repair bay(s)`);

      // Create new bays starting from currentNumberOfBays + 1
      for (let i = currentNumberOfBays + 1; i <= newNumberOfBays; i++) {
        await this.repairBaysService.createRepairBay(
          id,
          i,
          `Créneau ${i}`,
          updateDto.heureOuverture || currentGarage.heureOuverture || '08:00',
          updateDto.heureFermeture || currentGarage.heureFermeture || '18:00',
          true
        );
      }

      console.log(`✅ New repair bays created successfully`);
    }

    // Update the garage
    const updated = await this.garageModel.findByIdAndUpdate(id, updateDto, { new: true }).exec();
    if (!updated) throw new NotFoundException('Garage non trouvé');
    
    return updated;
  }

  async remove(id: string): Promise<void> {
    const result = await this.garageModel.findByIdAndDelete(id).exec();
    if (!result) throw new NotFoundException('Garage non trouvé');
  }
}
