import { IsString, IsMongoId, IsEnum, IsDateString } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class CreateDeadlineDto {
  @ApiProperty({ example: '2024-12-15T09:00:00.000Z' })
  @IsDateString()
  dateRappel: Date;

  @ApiProperty({ example: 'email' })
  @IsString()
  typeNotification: string;

  @ApiProperty({ enum: ['envoyé', 'reçu'], example: 'envoyé' })
  @IsEnum(['envoyé', 'reçu'])
  etat: string;

  @ApiProperty({ example: '507f1f77bcf86cd799439014' })
  @IsMongoId()
  document: string;
}
