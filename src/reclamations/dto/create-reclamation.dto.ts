import { IsString, IsEnum, IsNotEmpty, IsOptional, IsMongoId } from 'class-validator';

export class CreateReclamationDto {
  @IsEnum(['service', 'garage'])
  @IsNotEmpty()
  type: string;

  @IsString()
  @IsNotEmpty()
  titre: string;

  @IsString()
  @IsNotEmpty()
  message: string;

  @IsMongoId()
  @IsOptional()
  garage?: string;

  @IsMongoId()
  @IsOptional()
  service?: string;
}
