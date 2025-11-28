import { Injectable, Logger } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';
import { ExecutionContext } from '@nestjs/common';

@Injectable()
export class JwtAuthGuard extends AuthGuard('jwt') {
  private readonly logger = new Logger(JwtAuthGuard.name);

  canActivate(context: ExecutionContext) {
    this.logger.log(`JWT Guard invoked for route: ${context.getClass().name}`);
    const request = context.switchToHttp().getRequest();
    const authHeader = request.headers.authorization;
    this.logger.log(`Authorization header: ${authHeader?.substring(0, 20)}...`);
    return super.canActivate(context);
  }

  handleRequest(err: any, user: any, info: any) {
    this.logger.log(`JWT Guard handleRequest - Error: ${err}, User: ${JSON.stringify(user)}, Info: ${info}`);
    if (err || !user) {
      this.logger.error(`JWT validation failed: ${err?.message || info}`);
      throw err || new (require('@nestjs/common').UnauthorizedException)();
    }
    return user;
  }
}
