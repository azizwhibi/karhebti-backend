# Car Marketplace Feature - Implementation Guide

## Overview
A complete car marketplace system with swipe-based discovery, real-time chat, and notifications.

## Features Implemented

### 1. Data Models

#### Swipe Schema
- `userId`: Reference to user who swiped
- `carId`: Reference to car being swiped
- `direction`: 'left' or 'right'
- `sellerId`: Reference to car owner
- `status`: 'pending', 'accepted', or 'declined'
- Compound index prevents duplicate swipes

#### Conversation Schema
- `buyerId`: Reference to buyer
- `sellerId`: Reference to seller
- `carId`: Reference to car
- `status`: 'pending', 'active', or 'closed'
- `messages[]`: Array of message objects with senderId, content, timestamp, read status
- Compound index prevents duplicate conversations

#### Notification Schema
- `userId`: Recipient user
- `type`: Notification type (swipe_right, swipe_accepted, etc.)
- `title` & `message`: Display content
- `data`: Additional metadata
- `read`: Boolean flag
- `fromUserId`: Sender reference

#### Car Schema Updates
- `forSale`: Boolean flag
- `saleStatus`: 'available', 'sold', or 'not-listed'
- `price`: Optional price field
- `description`: Car description for marketplace
- `images[]`: Multiple images support

### 2. REST Endpoints

#### Cars Module
- `GET /cars/marketplace/available` - Get cars available for swiping (excludes own cars and already swiped)
- `POST /cars/:id/list-for-sale` - List a car for sale
- `POST /cars/:id/unlist` - Remove car from marketplace

#### Swipes Module
- `POST /swipes` - Record a swipe (left/right)
- `POST /swipes/respond` - Seller accepts/declines buyer interest
- `GET /swipes/my-swipes` - Get user's swipe history
- `GET /swipes/pending` - Get pending swipes for seller

#### Conversations Module
- `GET /conversations` - Get all user conversations
- `GET /conversations/:id` - Get specific conversation
- `POST /conversations/:id/mark-read` - Mark messages as read

#### Notifications Module
- `POST /notifications` - Create notification (system/admin)
- `GET /notifications` - Get user notifications (optional ?unreadOnly=true)
- `GET /notifications/unread-count` - Get unread count
- `POST /notifications/:id/mark-read` - Mark single as read
- `POST /notifications/mark-all-read` - Mark all as read

### 3. WebSocket Integration

#### Chat Gateway (`/chat` namespace)

**Connection**
- JWT authentication via handshake
- Automatic user-socket mapping
- Online/offline status broadcasting

**Events to Emit (Client → Server)**
```typescript
// Join a conversation room
socket.emit('join_conversation', { conversationId: 'xxx' });

// Leave a conversation room
socket.emit('leave_conversation', { conversationId: 'xxx' });

// Send a message
socket.emit('send_message', {
  conversationId: 'xxx',
  content: 'Hello!'
});

// Typing indicator
socket.emit('typing', {
  conversationId: 'xxx',
  isTyping: true
});
```

**Events to Listen (Server → Client)**
```typescript
// When successfully joined
socket.on('joined_conversation', ({ conversationId }) => {});

// New message received
socket.on('new_message', ({ conversationId, message }) => {});

// User typing status
socket.on('user_typing', ({ conversationId, userId, isTyping }) => {});

// Real-time notification
socket.on('notification', (notification) => {});

// User status
socket.on('user_online', ({ userId }) => {});
socket.on('user_offline', ({ userId }) => {});

// Errors
socket.on('error', ({ message }) => {});
```

### 4. Swipe Logic Flow

1. **User swipes right on a car**
   - Create swipe record with status 'pending'
   - Create notification for seller: "[Buyer] wants to buy your [Car]"
   - Real-time notification sent via WebSocket

2. **Seller views pending swipes**
   - GET /swipes/pending returns all pending offers

3. **Seller accepts offer**
   - POST /swipes/respond with response='accepted'
   - Create active Conversation between buyer & seller
   - Notify buyer of acceptance
   - Auto-enable WebSocket chat room

4. **Seller declines offer**
   - POST /swipes/respond with response='declined'
   - Notify buyer of decline
   - No conversation created

### 5. Security & Ownership

- JWT authentication required for all endpoints
- Users can only swipe on cars they don't own
- Duplicate swipes prevented by database index
- Conversation access verified before joining WebSocket room
- Only conversation participants can send messages
- Sellers control who they chat with via accept/decline

## Client Integration Example

### WebSocket Connection
```typescript
import io from 'socket.io-client';

const socket = io('http://localhost:3000/chat', {
  auth: {
    token: 'your-jwt-token'
  }
});

// Join conversation
socket.emit('join_conversation', { conversationId: '...' });

// Listen for messages
socket.on('new_message', ({ conversationId, message }) => {
  console.log('New message:', message);
});

// Send message
socket.emit('send_message', {
  conversationId: '...',
  content: 'Hello!'
});
```

### API Calls
```typescript
// Get available cars
const cars = await fetch('/cars/marketplace/available', {
  headers: { 'Authorization': `Bearer ${token}` }
});

// Swipe right
await fetch('/swipes', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    carId: 'xxx',
    direction: 'right'
  })
});

// Accept swipe
await fetch('/swipes/respond', {
  method: 'POST',
  headers: {
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  },
  body: JSON.stringify({
    swipeId: 'xxx',
    response: 'accepted'
  })
});
```

## Dependencies Installed
- `@nestjs/websockets` - WebSocket support
- `@nestjs/platform-socket.io` - Socket.IO adapter
- `socket.io` - Socket.IO library
- `@nestjs/event-emitter` - Event system for notifications

## Testing the Feature

1. **List a car for sale**
   ```bash
   POST /cars/:carId/list-for-sale
   ```

2. **Get available cars (as different user)**
   ```bash
   GET /cars/marketplace/available
   ```

3. **Swipe right on a car**
   ```bash
   POST /swipes
   {
     "carId": "xxx",
     "direction": "right"
   }
   ```

4. **Check pending swipes (as seller)**
   ```bash
   GET /swipes/pending
   ```

5. **Accept the swipe**
   ```bash
   POST /swipes/respond
   {
     "swipeId": "xxx",
     "response": "accepted"
   }
   ```

6. **Connect to WebSocket and join conversation**
   ```javascript
   socket.emit('join_conversation', { conversationId: 'xxx' });
   ```

7. **Send messages**
   ```javascript
   socket.emit('send_message', {
     conversationId: 'xxx',
     content: 'Is the car still available?'
   });
   ```

## Notes
- All endpoints require JWT authentication
- WebSocket uses same JWT for authentication
- Real-time notifications are sent automatically on swipe actions
- Conversations are created automatically on swipe acceptance
- Each conversation has isolated chat room
