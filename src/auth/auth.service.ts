import { Injectable, UnauthorizedException, ConflictException } from '@nestjs/common';
import { JwtService } from '@nestjs/jwt';
import { InjectModel } from '@nestjs/mongoose';
import { Model } from 'mongoose';
import * as bcrypt from 'bcrypt';
import * as admin from 'firebase-admin';
import { User, UserDocument } from '../users/schemas/user.schema';
import { LoginDto, SignupDto, ForgotPasswordDto, ResetPasswordDto } from './dto/auth.dto';

@Injectable()
export class AuthService {
  constructor(
    @InjectModel(User.name) private userModel: Model<UserDocument>,
    private jwtService: JwtService,
  ) {}

  async signup(signupDto: SignupDto) {
    const existingUser = await this.userModel.findOne({ email: signupDto.email });
    if (existingUser) {
      throw new ConflictException('Cet email est déjà utilisé');
    }

    const hashedPassword = await bcrypt.hash(signupDto.motDePasse, 10);
    const user = new this.userModel({
      ...signupDto,
      motDePasse: hashedPassword,
      role: 'utilisateur',
    });

    await user.save();

    const payload = { email: user.email, sub: user._id, role: user.role };
    return {
      access_token: this.jwtService.sign(payload),
      user: {
        id: user._id,
        email: user.email,
        nom: user.nom,
        prenom: user.prenom,
        role: user.role,
      },
    };
  }

  async login(loginDto: LoginDto) {
    const user = await this.userModel.findOne({ email: loginDto.email });
    if (!user) {
      throw new UnauthorizedException('Identifiants invalides');
    }

    const isPasswordValid = await bcrypt.compare(loginDto.motDePasse, user.motDePasse);
    if (!isPasswordValid) {
      throw new UnauthorizedException('Identifiants invalides');
    }

    const payload = { email: user.email, sub: user._id, role: user.role };
    return {
      access_token: this.jwtService.sign(payload),
      user: {
        id: user._id,
        email: user.email,
        nom: user.nom,
        prenom: user.prenom,
        role: user.role,
      },
    };
  }

  async forgotPassword(forgotPasswordDto: ForgotPasswordDto) {
    const user = await this.userModel.findOne({ email: forgotPasswordDto.email });
    if (!user) {
      // Ne pas révéler si l'email existe ou non pour des raisons de sécurité
      return { message: 'Si cet email existe, un lien de réinitialisation a été envoyé' };
    }

    // Génération d'un token de réinitialisation
    const resetToken = this.jwtService.sign(
      { email: user.email, sub: user._id },
      { expiresIn: '1h' },
    );

    // TODO: Envoyer un email avec le token
    // Pour l'instant, on retourne le token (à ne pas faire en production)
    return {
      message: 'Si cet email existe, un lien de réinitialisation a été envoyé',
      resetToken, // À retirer en production
    };
  }

  async resetPassword(resetPasswordDto: ResetPasswordDto) {
    try {
      const payload = this.jwtService.verify(resetPasswordDto.token);
      const user = await this.userModel.findById(payload.sub);

      if (!user) {
        throw new UnauthorizedException('Token invalide');
      }

      const hashedPassword = await bcrypt.hash(resetPasswordDto.nouveauMotDePasse, 10);
      user.motDePasse = hashedPassword;
      await user.save();

      return { message: 'Mot de passe réinitialisé avec succès' };
    } catch (error) {
      throw new UnauthorizedException('Token invalide ou expiré');
    }
  }

  async validateUser(payload: any): Promise<any> {
    const user = await this.userModel.findById(payload.sub);
    if (!user) {
      return null;
    }
    return {
      userId: (user._id as any).toString(),
      email: user.email,
      role: user.role,
    };
  }
}
