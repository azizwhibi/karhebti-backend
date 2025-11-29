import { IsEmail, IsString, MinLength } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class LoginDto {
  @ApiProperty({ example: 'rayen@esprit.tn' })
  @IsEmail()
  email: string;

  @ApiProperty({ example: 'rayen123' })
  @IsString()
  @MinLength(6)
  motDePasse: string;
}

export class SignupDto {
  @ApiProperty({ example: 'Dupont' })
  @IsString()
  nom: string;

  @ApiProperty({ example: 'Jean' })
  @IsString()
  prenom: string;

  @ApiProperty({ example: 'rayen@esprit.tn' })
  @IsEmail()
  email: string;

  @ApiProperty({ example: 'rayen123', minLength: 6 })
  @IsString()
  @MinLength(6)
  motDePasse: string;

  @ApiProperty({ example: '0612345678' })
  @IsString()
  telephone: string;
}

export class ForgotPasswordDto {
  @ApiProperty({ example: 'rayen@esprit.tn' })
  @IsEmail()
  email: string;
}

export class ResetPasswordDto {
  @ApiProperty({ example: 'reset-token-here' })
  @IsString()
  token: string;

  @ApiProperty({ example: 'NewPassword123!' })
  @IsString()
  @MinLength(6)
  nouveauMotDePasse: string;
}