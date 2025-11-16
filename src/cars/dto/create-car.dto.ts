
import { IsString, IsNumber, IsOptional } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class CreateCarDto {
  @ApiProperty({ example: 'Peugeot' })
  @IsString()
  marque: string;

  @ApiProperty({ example: '208' })
  @IsString()
  modele: string;

  @ApiProperty({ example: 2020 })
  @IsNumber()
  annee: number;

  @ApiProperty({ example: 'AB-123-CD' })
  @IsString()
  immatriculation: string;

  @ApiProperty({ example: 'Essence' })
  @IsString()
  typeCarburant: string;

  @ApiProperty({ example: 'http://example.com/image.jpg', required: false })
  @IsOptional()
  @IsString()
  imageUrl?: string; // URL or path to the uploaded car image
}