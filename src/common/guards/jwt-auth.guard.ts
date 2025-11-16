import { Injectable, ExecutionContext, UnauthorizedException } from '@nestjs/common';
import { AuthGuard } from '@nestjs/passport';

@Injectable()
export class JwtAuthGuard extends AuthGuard('jwt') {
  canActivate(context: ExecutionContext) {
    const request = context.switchToHttp().getRequest();
    console.log('=== JWT AUTH GUARD ===');
    console.log('🔒 Protecting endpoint:', request.method, request.url);
    console.log('📨 Authorization header:', request.headers.authorization ? 'PRESENT' : 'MISSING');
    
    if (request.headers.authorization) {
      const parts = request.headers.authorization.split(' ');
      console.log('🔑 Auth header parts:', parts.length);
      console.log('🔑 Auth scheme:', parts[0]);
      if (parts[1]) {
        console.log('🔑 Token (first 20 chars):', parts[1].substring(0, 20) + '...');
        console.log('🔑 Token (last 20 chars):', '...' + parts[1].substring(parts[1].length - 20));
      }
    } else {
      console.error('❌ No Authorization header found');
    }
    
    return super.canActivate(context);
  }

  handleRequest(err: any, user: any, info: any, context: ExecutionContext) {
    console.log('=== JWT GUARD HANDLE REQUEST ===');
    console.log('❓ Error:', err ? err.message : 'NONE');
    console.log('👤 User:', user ? 'FOUND' : 'NOT FOUND');
    console.log('ℹ️ Info:', info ? info.message || info : 'NONE');
    
    if (err) {
      console.error('❌ Error object:', JSON.stringify(err, null, 2));
    }
    
    if (!user) {
      console.error('❌ Authentication failed - no user object');
      if (info) {
        console.error('📋 Additional info:', info);
      }
      throw err || new UnauthorizedException('Authentication failed');
    }
    
    console.log('✅ Authentication successful');
    return user;
  }
}
