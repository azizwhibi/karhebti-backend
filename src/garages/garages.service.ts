import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Garage, GarageDocument } from './schemas/garage.schema';
import { CreateGarageDto } from './dto/create-garage.dto';
import { UpdateGarageDto } from './dto/update-garage.dto';

@Injectable()
export class GaragesService {
  constructor(@InjectModel(Garage.name) private garageModel: Model<GarageDocument>) {}

  async create(createDto: CreateGarageDto): Promise<Garage> {
    const created = new this.garageModel(createDto);
    return created.save();
  }

  async findAll(): Promise<Garage[]> {
    return this.garageModel.find().exec();
  }

  async findOne(id: string): Promise<Garage> {
    const garage = await this.garageModel.findById(id).exec();
    if (!garage) {
      throw new NotFoundException('Garage non trouvé');
    }
    return garage;
  }

  async update(id: string, updateDto: UpdateGarageDto): Promise<Garage> {
    const updated = await this.garageModel.findByIdAndUpdate(id, updateDto, { new: true }).exec();
    if (!updated) {
      throw new NotFoundException('Garage non trouvé');
    }
    return updated;
  }

  async remove(id: string): Promise<void> {
    const result = await this.garageModel.findByIdAndDelete(id).exec();
    if (!result) {
      throw new NotFoundException('Garage non trouvé');
    }
  }

  async findByLocation(lat?: number, lng?: number, radius?: number): Promise<Garage[]> {
    // Simulation - en production, utiliser des requêtes géospatiales MongoDB
    return this.garageModel.find().exec();
  }

  async findByService(serviceType: string): Promise<Garage[]> {
    return this.garageModel.find({ typeService: serviceType }).exec();
  }
}
