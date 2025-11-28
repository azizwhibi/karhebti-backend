import { Injectable, Logger } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy } from 'passport-jwt';
import { AuthService } from './auth.service';

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy) {
  private readonly logger = new Logger(JwtStrategy.name);

  constructor(private authService: AuthService) {
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      ignoreExpiration: false,
      secretOrKey: process.env.JWT_SECRET || 'karhebti-jwt-super-secret-key-2024',
    });
    this.logger.log(`JWT Secret configured: ${this.getSecretKey()?.substring(0, 10)}...`);
  }

  private getSecretKey() {
    return process.env.JWT_SECRET || 'karhebti-jwt-super-secret-key-2024';
  }

  async validate(payload: any) {
    this.logger.log(`JWT validation called with payload: ${JSON.stringify(payload)}`);
    const result = await this.authService.validateUser(payload);
    this.logger.log(`JWT validation result: ${JSON.stringify(result)}`);
    return result;
  }
}
