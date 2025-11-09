import { IsString, IsNumber, IsMongoId, IsEnum, IsDateString } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class CreateMaintenanceDto {
  @ApiProperty({ enum: ['vidange', 'révision', 'réparation'], example: 'vidange' })
  @IsEnum(['vidange', 'révision', 'réparation'])
  type: string;

  @ApiProperty({ example: '2024-01-15T10:00:00.000Z' })
  @IsDateString()
  date: Date;

  @ApiProperty({ example: 150.50 })
  @IsNumber()
  cout: number;

  @ApiProperty({ example: '507f1f77bcf86cd799439011' })
  @IsMongoId()
  garage: string;

  @ApiProperty({ example: '507f1f77bcf86cd799439012' })
  @IsMongoId()
  voiture: string;
}
