import {
  WebSocketGateway,
  WebSocketServer,
  SubscribeMessage,
  OnGatewayConnection,
  OnGatewayDisconnect,
  ConnectedSocket,
  MessageBody,
} from '@nestjs/websockets';
import { Server, Socket } from 'socket.io';
import { Logger, UseGuards } from '@nestjs/common';
import { ConversationsService } from '../conversations/conversations.service';
import { NotificationsService } from '../notifications/notifications.service';
import { JwtService } from '@nestjs/jwt';
import { OnEvent } from '@nestjs/event-emitter';

@WebSocketGateway({
  cors: {
    origin: '*',
  },
  namespace: '/chat',
})
export class ChatGateway implements OnGatewayConnection, OnGatewayDisconnect {
  @WebSocketServer()
  server: Server;

  private logger = new Logger('ChatGateway');
  private userSockets = new Map<string, string>(); // userId -> socketId

  constructor(
    private conversationsService: ConversationsService,
    private notificationsService: NotificationsService,
    private jwtService: JwtService,
  ) {}

  async handleConnection(client: Socket) {
    try {
      // Extract JWT token from handshake
      const token = client.handshake.auth?.token || client.handshake.headers?.authorization?.split(' ')[1];
      
      if (!token) {
        client.disconnect();
        return;
      }

      // Verify token
      const payload = this.jwtService.verify(token);
      const userId = payload.sub || payload.userId;

      // Store user socket mapping
      this.userSockets.set(userId, client.id);
      client.data.userId = userId;

      this.logger.log(`Client connected: ${client.id}, User: ${userId}`);
      
      // Notify user is online
      client.broadcast.emit('user_online', { userId });
    } catch (error) {
      this.logger.error('Connection error:', error.message);
      client.disconnect();
    }
  }

  handleDisconnect(client: Socket) {
    const userId = client.data.userId;
    if (userId) {
      this.userSockets.delete(userId);
      client.broadcast.emit('user_offline', { userId });
    }
    this.logger.log(`Client disconnected: ${client.id}`);
  }

  @SubscribeMessage('join_conversation')
  async handleJoinConversation(
    @ConnectedSocket() client: Socket,
    @MessageBody() data: { conversationId: string },
  ) {
    const { conversationId } = data;
    const userId = client.data.userId;

    try {
      // Verify user is part of conversation
      const conversation = await this.conversationsService.getConversation(conversationId);
      
      if (!conversation) {
        client.emit('error', { message: 'Conversation not found' });
        return;
      }

      const isBuyer = conversation.buyerId.toString() === userId;
      const isSeller = conversation.sellerId.toString() === userId;

      if (!isBuyer && !isSeller) {
        client.emit('error', { message: 'Unauthorized access to conversation' });
        return;
      }

      // Join room
      const roomName = `convo_${conversationId}`;
      client.join(roomName);
      
      this.logger.log(`User ${userId} joined conversation ${conversationId}`);
      client.emit('joined_conversation', { conversationId });
    } catch (error) {
      this.logger.error('Error joining conversation:', error.message);
      client.emit('error', { message: 'Failed to join conversation' });
    }
  }

  @SubscribeMessage('leave_conversation')
  handleLeaveConversation(
    @ConnectedSocket() client: Socket,
    @MessageBody() data: { conversationId: string },
  ) {
    const roomName = `convo_${data.conversationId}`;
    client.leave(roomName);
    this.logger.log(`User ${client.data.userId} left conversation ${data.conversationId}`);
  }

  @SubscribeMessage('send_message')
  async handleSendMessage(
    @ConnectedSocket() client: Socket,
    @MessageBody() data: { conversationId: string; content: string },
  ) {
    const { conversationId, content } = data;
    const userId = client.data.userId;

    try {
      // Add message to conversation
      const { conversation, message } = await this.conversationsService.addMessage(
        conversationId,
        userId,
        content,
      );

      // Emit to all users in the conversation room
      const roomName = `convo_${conversationId}`;
      this.server.to(roomName).emit('new_message', {
        conversationId,
        message: {
          _id: message._id,
          senderId: userId,
          content: message.content,
          createdAt: message.createdAt,
          isRead: message.isRead,
        },
      });

      // Send notification to the other user
      const otherUserId = conversation.buyerId.toString() === userId 
        ? conversation.sellerId.toString() 
        : conversation.buyerId.toString();

      await this.notificationsService.createNotification({
        userId: otherUserId,
        type: 'new_message',
        title: 'New Message',
        message: `You have a new message`,
        data: {
          conversationId,
        },
        fromUserId: userId,
      });
    } catch (error) {
      this.logger.error('Error sending message:', error.message);
      client.emit('error', { message: 'Failed to send message' });
    }
  }

  // Broadcast messages created via REST endpoint
  @OnEvent('message.created')
  async handleMessageCreated(payload: {
    conversationId: string;
    message: { _id: string; senderId: string; content: string; createdAt: Date; isRead: boolean };
    otherUserId: string;
  }) {
    const roomName = `convo_${payload.conversationId}`;
    this.server.to(roomName).emit('new_message', {
      conversationId: payload.conversationId,
      message: payload.message,
    });

    // Notify the other participant
    await this.notificationsService.createNotification({
      userId: payload.otherUserId,
      type: 'new_message',
      title: 'New Message',
      message: `You have a new message`,
      data: { conversationId: payload.conversationId },
      fromUserId: payload.message.senderId,
    });
  }

  @SubscribeMessage('typing')
  handleTyping(
    @ConnectedSocket() client: Socket,
    @MessageBody() data: { conversationId: string; isTyping: boolean },
  ) {
    const roomName = `convo_${data.conversationId}`;
    const userId = client.data.userId;
    
    // Broadcast typing status to other users in the room
    client.to(roomName).emit('user_typing', {
      conversationId: data.conversationId,
      userId,
      isTyping: data.isTyping,
    });
  }

  // Listen for notification events and send via WebSocket
  @OnEvent('notification.created')
  handleNotificationCreated(payload: { userId: string; notification: any }) {
    const socketId = this.userSockets.get(payload.userId);
    if (socketId) {
      this.server.to(socketId).emit('notification', payload.notification);
    }
  }
}
