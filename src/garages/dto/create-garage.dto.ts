import { IsString, IsArray, IsNumber, Min, Max } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class CreateGarageDto {
  @ApiProperty({ example: 'Garage Central' })
  @IsString()
  nom: string;

  @ApiProperty({ example: '123 Rue de Paris, 75001 Paris' })
  @IsString()
  adresse: string;

  @ApiProperty({ example: ['vidange', 'réparation', 'contrôle technique'] })
  @IsArray()
  @IsString({ each: true })
  typeService: string[];

  @ApiProperty({ example: '0145678901' })
  @IsString()
  telephone: string;

  @ApiProperty({ example: 4.5, minimum: 0, maximum: 5 })
  @IsNumber()
  @Min(0)
  @Max(5)
  noteUtilisateur: number;
}
