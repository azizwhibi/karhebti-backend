import { IsOptional, IsIn, Matches, IsArray, ArrayMinSize, ArrayMaxSize, IsString } from 'class-validator';
import { PartialType, ApiPropertyOptional } from '@nestjs/swagger';
import { CreateReservationDto } from './create-reservation.dto';

export const SERVICE_TYPES = [
  'vidange', 'contrôle technique', 'réparation pneu', 'changement pneu', 
  'freinage', 'batterie', 'climatisation', 'échappement', 
  'révision complète', 'diagnostic électronique', 'carrosserie', 
  'peinture', 'pare-brise', 'suspension', 'embrayage', 'transmission', 
  'injection', 'refroidissement', 'démarrage', 'lavage auto', 
  'équilibrage roues', 'parallélisme', 'système électrique', 
  'filtre à air', 'filtre à huile', 'plaquettes de frein'
];

export class UpdateReservationDto extends PartialType(CreateReservationDto) {
  @ApiPropertyOptional({ 
    description: 'Nouveau statut', 
    enum: ['en_attente', 'confirmé', 'annulé'] 
  })
  @IsOptional()
  @IsIn(['en_attente', 'confirmé', 'annulé'])
  status?: string;

  @ApiPropertyOptional({ 
    description: 'Services mis à jour', 
    example: ['vidange', 'réparation pneu'] 
  })
  @IsOptional()
  @IsArray()
  @ArrayMinSize(0)
  @ArrayMaxSize(5)
  @IsString({ each: true })
  @IsIn(SERVICE_TYPES, { each: true })
  services?: string[];

  @ApiPropertyOptional({ 
    description: 'Nouveaux commentaires' 
  })
  @IsOptional()
  @IsString()
  commentaires?: string;

  @ApiPropertyOptional({ 
    description: 'Heure de début (HH:mm)', 
    example: '10:00' 
  })
  @IsOptional()
  @Matches(/^(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]$/)
  heureDebut?: string;

  @ApiPropertyOptional({ 
    description: 'Heure de fin (HH:mm)', 
    example: '11:30' 
  })
  @IsOptional()
  @Matches(/^(0[0-9]|1[0-9]|2[0-3]):[0-5][0-9]$/)
  heureFin?: string;

  // Prevent changing core identifiers
  garageId?: never;
  userId?: never;
  email?: never;
  carId?: never; // 🔽 NEW: can't change car after booking
}
