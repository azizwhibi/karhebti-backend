import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import { Deadline, DeadlineDocument } from './schemas/deadline.schema';
import { CreateDeadlineDto } from './dto/create-deadline.dto';
import { UpdateDeadlineDto } from './dto/update-deadline.dto';
import { DocumentsService } from '../documents/documents.service';

@Injectable()
export class DeadlinesService {
  constructor(
    @InjectModel(Deadline.name) private deadlineModel: Model<DeadlineDocument>,
    private documentsService: DocumentsService,
  ) {}

  async create(createDto: CreateDeadlineDto, userId: string, userRole: string): Promise<Deadline> {
    await this.documentsService.findOne(createDto.document, userId, userRole);
    const created = new this.deadlineModel(createDto);
    return created.save();
  }

  async findAll(userId: string, userRole: string): Promise<Deadline[]> {
    return this.deadlineModel.find().populate('document').exec();
  }

  async findOne(id: string): Promise<Deadline> {
    const deadline = await this.deadlineModel.findById(id).populate('document').exec();
    if (!deadline) {
      throw new NotFoundException('Échéance non trouvée');
    }
    return deadline;
  }

  async update(id: string, updateDto: UpdateDeadlineDto): Promise<Deadline> {
    const updated = await this.deadlineModel.findByIdAndUpdate(id, updateDto, { new: true }).populate('document').exec();
    if (!updated) {
      throw new NotFoundException('Échéance non trouvée');
    }
    return updated;
  }

  async remove(id: string): Promise<void> {
    const result = await this.deadlineModel.findByIdAndDelete(id).exec();
    if (!result) {
      throw new NotFoundException('Échéance non trouvée');
    }
  }
}
