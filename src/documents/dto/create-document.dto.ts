import { IsString, IsMongoId, IsEnum, IsDateString } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class CreateDocumentDto {
  @ApiProperty({ enum: ['assurance', 'carte grise', 'contrôle technique'], example: 'assurance' })
  @IsEnum(['assurance', 'carte grise', 'contrôle technique'])
  type: string;

  @ApiProperty({ example: '2024-01-01T00:00:00.000Z' })
  @IsDateString()
  dateEmission: Date;

  @ApiProperty({ example: '2025-01-01T00:00:00.000Z' })
  @IsDateString()
  dateExpiration: Date;

  @ApiProperty({ example: 'https://storage.example.com/documents/assurance.pdf' })
  @IsString()
  fichier: string;

  @ApiProperty({ example: '507f1f77bcf86cd799439012' })
  @IsMongoId()
  voiture: string;
}
