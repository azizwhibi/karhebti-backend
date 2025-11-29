import { Injectable, UnauthorizedException, Logger } from '@nestjs/common';
import { PassportStrategy } from '@nestjs/passport';
import { ExtractJwt, Strategy } from 'passport-jwt';
import { AuthService } from './auth.service';

@Injectable()
export class JwtStrategy extends PassportStrategy(Strategy) {
  private readonly logger = new Logger(JwtStrategy.name);

  constructor(private authService: AuthService) {
    const secret = process.env.JWT_SECRET || 'your-secret-key-change-in-production';
    
    super({
      jwtFromRequest: ExtractJwt.fromAuthHeaderAsBearerToken(),
      ignoreExpiration: false,
      secretOrKey: secret,
    });
    
    this.logger.log(`JWT Strategy initialized with secret: ${secret.substring(0, 10)}...`);
  }

  async validate(payload: any) {
    this.logger.debug(`Validating JWT payload for user: ${payload.sub}`);
    
    try {
      const user = await this.authService.validateUser(payload);
      
      if (!user) {
        this.logger.warn(`User validation failed for payload: ${JSON.stringify(payload)}`);
        throw new UnauthorizedException('User validation failed');
      }
      
      this.logger.debug(`User ${payload.sub} validated successfully`);
      return user;
    } catch (error) {
      this.logger.error(`JWT validation error: ${error.message}`, error.stack);
      throw error;
    }
  }
}
