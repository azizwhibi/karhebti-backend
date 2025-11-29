import { Injectable, ExecutionContext, UnauthorizedException, Logger } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';

@Injectable()
export class JwtAuthGuard extends AuthGuard('jwt') {
  private readonly logger = new Logger(JwtAuthGuard.name);

  canActivate(context: ExecutionContext) {
    const request = context.switchToHttp().getRequest();
    const hasAuth = !!request.headers.authorization;
    
    this.logger.debug(
      `Protecting ${request.method} ${request.url} - Auth header: ${hasAuth ? 'present' : 'missing'}`
    );
    
    return super.canActivate(context);
  }

  handleRequest(err: any, user: any, info: any, context: ExecutionContext) {
    if (err || !user) {
      const request = context.switchToHttp().getRequest();
      this.logger.warn(
        `Authentication failed for ${request.method} ${request.url}: ${err?.message || info?.message || 'No user found'}`
      );
      throw err || new UnauthorizedException('Authentication required');
    }
    
    return user;
  }
}
