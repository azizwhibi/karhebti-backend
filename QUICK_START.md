# Quick Start Guide - Car Marketplace

## Running the Application

```bash
# Install dependencies (already done)
npm install

# Start development server
npm run start:dev

# Or use the existing PowerShell script
.\start-dev.ps1
```

The server will start on the configured port (typically 3000).

## API Base URL
```
http://localhost:3000
```

## WebSocket URL
```
ws://localhost:3000/chat
```

## Quick Test Flow

### 1. Register/Login Two Users
```bash
# User A (Seller)
POST /auth/register
{
  "nom": "Seller",
  "prenom": "User",
  "email": "seller@test.com",
  "motDePasse": "password123"
}

# User B (Buyer)  
POST /auth/register
{
  "nom": "Buyer",
  "prenom": "User",
  "email": "buyer@test.com",
  "motDePasse": "password123"
}
```

### 2. User A: Create and List a Car
```bash
# Create car
POST /cars
Authorization: Bearer <seller_token>
{
  "marque": "Peugeot",
  "modele": "208",
  "annee": 2020,
  "immatriculation": "AB-123-CD",
  "typeCarburant": "Essence",
  "price": 15000,
  "description": "Excellent condition",
  "forSale": true,
  "saleStatus": "available"
}

# Or list existing car
POST /cars/:carId/list-for-sale
Authorization: Bearer <seller_token>
```

### 3. User B: Browse and Swipe
```bash
# Get available cars
GET /cars/marketplace/available
Authorization: Bearer <buyer_token>

# Swipe right on a car
POST /swipes
Authorization: Bearer <buyer_token>
{
  "carId": "<car_id>",
  "direction": "right"
}
```

### 4. User A: View and Accept Swipe
```bash
# Get pending swipes
GET /swipes/pending
Authorization: Bearer <seller_token>

# Accept the swipe
POST /swipes/respond
Authorization: Bearer <seller_token>
{
  "swipeId": "<swipe_id>",
  "response": "accepted"
}
```

### 5. Both Users: Connect WebSocket and Chat
```javascript
// User A & B connect
import io from 'socket.io-client';

const socket = io('http://localhost:3000/chat', {
  auth: {
    token: '<jwt_token>'
  }
});

// Get conversation ID from accept response or GET /conversations
const conversationId = '<conversation_id>';

// Join conversation
socket.emit('join_conversation', { conversationId });

// Listen for messages
socket.on('new_message', ({ message }) => {
  console.log('Received:', message.content);
});

// Send message
socket.emit('send_message', {
  conversationId,
  content: 'Hello! Is the car still available?'
});
```

### 6. Check Notifications
```bash
# Get all notifications
GET /notifications
Authorization: Bearer <user_token>

# Get unread count
GET /notifications/unread-count
Authorization: Bearer <user_token>

# Mark as read
POST /notifications/:id/mark-read
Authorization: Bearer <user_token>
```

## Testing with Swagger

The application includes Swagger documentation (if configured). Access it at:
```
http://localhost:3000/api
```

## Environment Variables

Make sure your `.env` file includes:
```env
MONGODB_URI=mongodb://localhost:27017/karhebti
JWT_SECRET=your-secret-key
PORT=3000
```

## Database Indexes

The following indexes are automatically created:
- Swipe: `{ userId: 1, carId: 1 }` (unique)
- Conversation: `{ buyerId: 1, sellerId: 1, carId: 1 }` (unique)

## Common Issues

### WebSocket Connection Failed
- Ensure JWT token is valid
- Check CORS configuration
- Verify WebSocket namespace is `/chat`

### Can't Swipe on Car
- Make sure you don't own the car
- Check if you've already swiped on this car
- Verify car is listed for sale (forSale=true, saleStatus='available')

### Can't Join Conversation
- Ensure you're either the buyer or seller
- Verify conversation exists and is active
- Check JWT token is valid

## File Structure
```
src/
├── swipes/           # Swipe functionality
├── conversations/    # Chat conversations
├── notifications/    # Notification system
├── chat/            # WebSocket gateway
└── cars/            # Car module (updated)
```

## Documentation
- `MARKETPLACE_README.md` - Detailed feature documentation
- `IMPLEMENTATION_SUMMARY.md` - Implementation overview

## Support
For issues or questions, check the documentation files or review the code comments.
