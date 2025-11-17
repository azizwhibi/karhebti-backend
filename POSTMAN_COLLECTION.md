# Postman Collection for Car Marketplace

## Setup

1. Create a new Postman Environment with these variables:
   - `baseUrl`: `http://localhost:3000`
   - `sellerToken`: (will be set after login)
   - `buyerToken`: (will be set after login)
   - `carId`: (will be set after creating car)
   - `swipeId`: (will be set after swiping)
   - `conversationId`: (will be set after accepting swipe)

2. Import the requests below

## Authentication

### Register Seller
```
POST {{baseUrl}}/auth/register
Content-Type: application/json

{
  "nom": "Seller",
  "prenom": "User",
  "email": "seller@test.com",
  "motDePasse": "password123",
  "telephone": "+1234567890"
}
```

### Register Buyer
```
POST {{baseUrl}}/auth/register
Content-Type: application/json

{
  "nom": "Buyer",
  "prenom": "User",
  "email": "buyer@test.com",
  "motDePasse": "password123",
  "telephone": "+0987654321"
}
```

### Login Seller
```
POST {{baseUrl}}/auth/login
Content-Type: application/json

{
  "email": "seller@test.com",
  "motDePasse": "password123"
}

# Save response token to {{sellerToken}}
```

### Login Buyer
```
POST {{baseUrl}}/auth/login
Content-Type: application/json

{
  "email": "buyer@test.com",
  "motDePasse": "password123"
}

# Save response token to {{buyerToken}}
```

## Cars - Seller Actions

### Create Car
```
POST {{baseUrl}}/cars
Authorization: Bearer {{sellerToken}}
Content-Type: application/json

{
  "marque": "Peugeot",
  "modele": "208",
  "annee": 2020,
  "immatriculation": "AB-123-CD",
  "typeCarburant": "Essence",
  "price": 15000,
  "description": "Excellent condition, single owner",
  "forSale": true,
  "saleStatus": "available"
}

# Save response._id to {{carId}}
```

### List Car for Sale
```
POST {{baseUrl}}/cars/{{carId}}/list-for-sale
Authorization: Bearer {{sellerToken}}
```

### Get My Cars
```
GET {{baseUrl}}/cars
Authorization: Bearer {{sellerToken}}
```

### Unlist Car
```
POST {{baseUrl}}/cars/{{carId}}/unlist
Authorization: Bearer {{sellerToken}}
```

## Cars - Buyer Actions

### Get Available Cars
```
GET {{baseUrl}}/cars/marketplace/available
Authorization: Bearer {{buyerToken}}
```

### Get Car Details
```
GET {{baseUrl}}/cars/{{carId}}
Authorization: Bearer {{buyerToken}}
```

## Swipes - Buyer Actions

### Swipe Right (Interested)
```
POST {{baseUrl}}/swipes
Authorization: Bearer {{buyerToken}}
Content-Type: application/json

{
  "carId": "{{carId}}",
  "direction": "right"
}

# Save response._id to {{swipeId}}
```

### Swipe Left (Not Interested)
```
POST {{baseUrl}}/swipes
Authorization: Bearer {{buyerToken}}
Content-Type: application/json

{
  "carId": "{{carId}}",
  "direction": "left"
}
```

### Get My Swipes
```
GET {{baseUrl}}/swipes/my-swipes
Authorization: Bearer {{buyerToken}}
```

## Swipes - Seller Actions

### Get Pending Swipes
```
GET {{baseUrl}}/swipes/pending
Authorization: Bearer {{sellerToken}}
```

### Accept Swipe
```
POST {{baseUrl}}/swipes/respond
Authorization: Bearer {{sellerToken}}
Content-Type: application/json

{
  "swipeId": "{{swipeId}}",
  "response": "accepted"
}

# Save response.conversation._id to {{conversationId}}
```

### Decline Swipe
```
POST {{baseUrl}}/swipes/respond
Authorization: Bearer {{sellerToken}}
Content-Type: application/json

{
  "swipeId": "{{swipeId}}",
  "response": "declined"
}
```

## Conversations

### Get My Conversations (Buyer or Seller)
```
GET {{baseUrl}}/conversations
Authorization: Bearer {{buyerToken}}
```

### Get Conversation Details
```
GET {{baseUrl}}/conversations/{{conversationId}}
Authorization: Bearer {{buyerToken}}
```

### Mark Messages as Read
```
POST {{baseUrl}}/conversations/{{conversationId}}/mark-read
Authorization: Bearer {{buyerToken}}
```

## Notifications

### Get All Notifications
```
GET {{baseUrl}}/notifications
Authorization: Bearer {{buyerToken}}
```

### Get Unread Notifications Only
```
GET {{baseUrl}}/notifications?unreadOnly=true
Authorization: Bearer {{sellerToken}}
```

### Get Unread Count
```
GET {{baseUrl}}/notifications/unread-count
Authorization: Bearer {{buyerToken}}
```

### Mark Notification as Read
```
POST {{baseUrl}}/notifications/{{notificationId}}/mark-read
Authorization: Bearer {{buyerToken}}
```

### Mark All as Read
```
POST {{baseUrl}}/notifications/mark-all-read
Authorization: Bearer {{buyerToken}}
```

## Testing Flow

1. **Setup** (run once)
   - Register Seller
   - Register Buyer
   - Login Seller (save token)
   - Login Buyer (save token)

2. **Seller Lists Car**
   - Create Car (save carId)
   - Verify with Get My Cars

3. **Buyer Discovers and Swipes**
   - Get Available Cars
   - Swipe Right (save swipeId)
   - Check notifications

4. **Seller Reviews and Accepts**
   - Get Pending Swipes
   - Accept Swipe (save conversationId)
   - Check notifications

5. **Both Check Conversation**
   - Get My Conversations
   - Get Conversation Details

6. **Test WebSocket** (use separate WebSocket client)
   - Connect with buyer token
   - Connect with seller token
   - Join conversation
   - Send messages

## Notes

- All endpoints except auth require JWT token
- Tokens should be included in Authorization header: `Bearer <token>`
- Replace `{{variable}}` with actual values or use Postman environment variables
- WebSocket testing requires a WebSocket client (not Postman HTTP requests)

## WebSocket Testing

Use a WebSocket client like:
- **Browser Console**: See QUICK_START.md for code
- **Postman WebSocket**: ws://localhost:3000/chat
- **Online tools**: https://www.piesocket.com/websocket-tester

Connection headers:
```json
{
  "auth": {
    "token": "your-jwt-token"
  }
}
```
