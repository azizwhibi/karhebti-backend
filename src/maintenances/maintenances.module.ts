import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { MaintenancesService } from './maintenances.service';
import { MaintenancesController } from './maintenances.controller';
import { Maintenance, MaintenanceSchema } from './schemas/maintenance.schema';
import { CarsModule } from '../cars/cars.module';
import { forwardRef } from '@nestjs/common';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: Maintenance.name, schema: MaintenanceSchema }]),
    forwardRef(() => CarsModule), // 🔽 Use forwardRef if CarsModule imports something that imports MaintenancesModule
  ],
  controllers: [MaintenancesController],
  providers: [MaintenancesService],
  exports: [MaintenancesService], // 🔽 add this
})
export class MaintenancesModule {}
