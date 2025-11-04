import { Injectable, NotFoundException, ForbiddenException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model, Types } from 'mongoose';
import { Car, CarDocument } from './schemas/car.schema';
import { CreateCarDto } from './dto/create-car.dto';
import { UpdateCarDto } from './dto/update-car.dto';

@Injectable()
export class CarsService {
  constructor(@InjectModel(Car.name) private carModel: Model<CarDocument>) {}

  async create(createCarDto: CreateCarDto, userId: string): Promise<Car> {
    const createdCar = new this.carModel({
      ...createCarDto,
      user: userId,
    });
    return createdCar.save();
  }

  async findAll(userId: string, userRole: string): Promise<Car[]> {
    // Admin peut voir toutes les voitures, utilisateur seulement les siennes
    const filter = userRole === 'admin' ? {} : { user: userId };
    return this.carModel.find(filter).populate('user', '-motDePasse').exec();
  }

  async findOne(id: string, userId: string, userRole: string): Promise<Car> {
    const car = await this.carModel.findById(id).populate('user', '-motDePasse').exec();
    if (!car) {
      throw new NotFoundException('Voiture non trouvée');
    }

    // Vérifier que l'utilisateur a accès à cette voiture
    if (userRole !== 'admin' && car.user.toString() !== userId) {
      throw new ForbiddenException('Accès non autorisé à cette voiture');
    }

    return car;
  }

  async update(id: string, updateCarDto: UpdateCarDto, userId: string, userRole: string): Promise<Car> {
    const car = await this.carModel.findById(id);
    if (!car) {
      throw new NotFoundException('Voiture non trouvée');
    }

    if (userRole !== 'admin' && car.user.toString() !== userId) {
      throw new ForbiddenException('Vous ne pouvez modifier que vos propres voitures');
    }

    const updatedCar = await this.carModel
      .findByIdAndUpdate(id, updateCarDto, { new: true })
      .populate('user', '-motDePasse')
      .exec();

    if (!updatedCar) {
      throw new NotFoundException('Voiture non trouvée');
    }

    return updatedCar;
  }

  async remove(id: string, userId: string, userRole: string): Promise<void> {
    const car = await this.carModel.findById(id);
    if (!car) {
      throw new NotFoundException('Voiture non trouvée');
    }

    if (userRole !== 'admin' && car.user.toString() !== userId) {
      throw new ForbiddenException('Vous ne pouvez supprimer que vos propres voitures');
    }

    await this.carModel.findByIdAndDelete(id).exec();
  }

  async findByUser(userId: string): Promise<Car[]> {
    return this.carModel.find({ user: userId }).exec();
  }
}
