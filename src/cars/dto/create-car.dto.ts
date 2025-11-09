import { IsString, IsNumber, IsMongoId } from 'class-validator';
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
}
