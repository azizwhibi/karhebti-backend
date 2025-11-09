import { IsString, IsNumber, IsMongoId, IsEnum } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class CreateServiceDto {
  @ApiProperty({ enum: ['vidange', 'contrôle technique', 'réparation pneu'], example: 'vidange' })
  @IsEnum(['vidange', 'contrôle technique', 'réparation pneu'])
  type: string;

  @ApiProperty({ example: 75.50 })
  @IsNumber()
  coutMoyen: number;

  @ApiProperty({ example: 60, description: 'Durée en minutes' })
  @IsNumber()
  dureeEstimee: number;

  @ApiProperty({ example: '507f1f77bcf86cd799439011' })
  @IsMongoId()
  garage: string;
  
  @ApiProperty({ example: '507f1f77bcf86cd799439099', description: 'ID du véhicule concerné' })
  @IsMongoId()
  car: string;

}
