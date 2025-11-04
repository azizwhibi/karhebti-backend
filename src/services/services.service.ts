import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Service, ServiceDocument } from './schemas/service.schema';
import { CreateServiceDto } from './dto/create-service.dto';
import { UpdateServiceDto } from './dto/update-service.dto';

@Injectable()
export class ServicesService {
  constructor(@InjectModel(Service.name) private serviceModel: Model<ServiceDocument>) {}

  async create(createDto: CreateServiceDto): Promise<Service> {
    const created = new this.serviceModel(createDto);
    return created.save();
  }

  async findAll(): Promise<Service[]> {
    return this.serviceModel.find().populate('garage').exec();
  }

  async findOne(id: string): Promise<Service> {
    const service = await this.serviceModel.findById(id).populate('garage').exec();
    if (!service) {
      throw new NotFoundException('Service non trouvé');
    }
    return service;
  }

  async update(id: string, updateDto: UpdateServiceDto): Promise<Service> {
    const updated = await this.serviceModel.findByIdAndUpdate(id, updateDto, { new: true }).populate('garage').exec();
    if (!updated) {
      throw new NotFoundException('Service non trouvé');
    }
    return updated;
  }

  async remove(id: string): Promise<void> {
    const result = await this.serviceModel.findByIdAndDelete(id).exec();
    if (!result) {
      throw new NotFoundException('Service non trouvé');
    }
  }

  async findByGarage(garageId: string): Promise<Service[]> {
    return this.serviceModel.find({ garage: garageId }).populate('garage').exec();
  }
}
