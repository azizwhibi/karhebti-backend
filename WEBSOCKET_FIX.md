# iOS Swift WebSocket (Socket.IO) Fix Guide

## 1. Backend Context
Namespace: `/chat` (NestJS `ChatGateway`).
Auth: JWT required in handshake (via `handshake.auth.token` or `Authorization: Bearer <jwt>` header).
Events (client -> server): `join_conversation`, `leave_conversation`, `send_message`, `typing`.
Events (server -> client): `joined_conversation`, `new_message`, `user_typing`, `notification`, `user_online`, `user_offline`, `error`.
Room naming: `convo_<conversationId>`.

## 2. Goal
Implement a reliable Socket.IO connection from iOS (Swift) that:
- Authenticates with JWT before connect
- Joins conversation rooms
- Sends & receives messages
- Shows typing indicators
- Receives real-time notifications
- Handles reconnection & token refresh

## 3. Dependency
Add Socket.IO-Client-Swift:
- Swift Package Manager: Xcode > File > Add Packages > `https://github.com/socketio/socket.io-client-swift`
- Or CocoaPods: `pod 'Socket.IO-Client-Swift', '~> 16.0'`

## 4. Swift Singleton Implementation
```swift
import SocketIO

final class ChatSocket {
    static let shared = ChatSocket()
    private var manager: SocketManager!
    private(set) var socket: SocketIOClient!
    private var jwt: String = ""

    private init() {}

    func configure(jwt: String, baseURL: String = "http://192.168.1.190:3000") {
        self.jwt = jwt
        guard let url = URL(string: baseURL + "/chat") else { return }

        manager = SocketManager(
            socketURL: url,
            config: [
                .log(true),
                .compress,
                .forceWebsockets(true),
                .reconnects(true),
                .reconnectAttempts(10),
                .reconnectWait(2),
                .extraHeaders(["Authorization": "Bearer \(jwt)"]), // header path
                .connectParams(["token": jwt]) // handshake auth path
            ]
        )
        socket = manager.defaultSocket
        registerHandlers()
    }

    private func registerHandlers() {
        socket.on(clientEvent: .connect) { [weak self] _, _ in
            print("✅ Connected: \(self?.socket.sid ?? "no sid")")
        }
        socket.on(clientEvent: .disconnect) { data, _ in
            print("⚠️ Disconnected: \(data)")
        }
        socket.on("error") { data, _ in
            print("❌ Server error: \(data)")
        }
        socket.on("user_online") { data, _ in print("🔵 User online: \(data)") }
        socket.on("user_offline") { data, _ in print("⚫️ User offline: \(data)") }
        socket.on("joined_conversation") { data, _ in print("📥 Joined: \(data)") }
        socket.on("new_message") { data, _ in
            print("💬 New message: \(data)")
            // Parse & update UI
        }
        socket.on("user_typing") { data, _ in print("⌨️ Typing: \(data)") }
        socket.on("notification") { data, _ in print("🔔 Notification: \(data)") }
    }

    func connect() { if socket.status != .connected { socket.connect() } }
    func disconnect() { socket.disconnect() }

    // MARK: - Conversation
    func joinConversation(id: String) {
        socket.emit("join_conversation", ["conversationId": id])
    }
    func leaveConversation(id: String) {
        socket.emit("leave_conversation", ["conversationId": id])
    }

    // MARK: - Messaging
    func sendMessage(conversationId: String, content: String) {
        socket.emit("send_message", [
            "conversationId": conversationId,
            "content": content
        ])
    }

    // MARK: - Typing
    func setTyping(conversationId: String, isTyping: Bool) {
        socket.emit("typing", [
            "conversationId": conversationId,
            "isTyping": isTyping
        ])
    }

    // MARK: - Token Refresh
    func refreshToken(newJwt: String) {
        // Disconnect, rebuild with new token, reconnect
        disconnect()
        configure(jwt: newJwt)
        connect()
    }
}
```

### Usage Example
```swift
ChatSocket.shared.configure(jwt: userJwt)
ChatSocket.shared.connect()
ChatSocket.shared.joinConversation(id: currentConversationId)
ChatSocket.shared.sendMessage(conversationId: currentConversationId, content: "Hello 👋")
ChatSocket.shared.setTyping(conversationId: currentConversationId, isTyping: true)
```

## 5. Common Pitfalls & Fixes
| Problem | Cause | Fix |
|---------|-------|-----|
| Disconnect immediately | Missing/invalid JWT | Set headers BEFORE `connect()` |
| No messages received | Not joined room | Call `joinConversation` first |
| Typing not shown | Not in room | Must join conversation |
| 404 errors | Wrong namespace | Use `/chat` in URL |
| Duplicate connections | Multiple managers | Use singleton pattern |
| Auth not applied | Late header injection | Configure then connect |

## 6. Reconnection Strategy
- Built-in reconnect: `.reconnects(true)` with attempts and wait.
- Optionally implement exponential backoff manually on disconnect.
- Avoid flooding UI with repeated alerts.

## 7. Token Expiry Handling
On auth error or forbidden event:
1. Refresh JWT via REST.
2. Call `ChatSocket.shared.refreshToken(newJwt: ...)`.

## 8. Production Hardening
- Use HTTPS + real domain.
- Limit CORS origins (remove `origin: '*'`).
- Consider using only handshake `auth.token` and drop header parsing.
- Add ack callbacks for critical emits (e.g., message delivery confirmations).

## 9. Debugging
- Enable `.log(true)` in config.
- Print `socket.status` transitions.
- Add server-side logging around `handleConnection` (already present).

## 10. Minimal Smoke Test
1. Connect.
2. Join known conversation.
3. Send message; see `new_message`.
4. Open second device; repeat.
5. Emit typing; observe `user_typing`.
6. Trigger notification via REST (new message from other user); see `notification` event.

## 11. Error Handling Pattern
```swift
socket.on("error") { data, _ in
    if let obj = data.first as? [String: Any], let msg = obj["message"] as? String {
        print("Server error: \(msg)")
    }
}
```

## 12. Checklist
- [ ] JWT set before connect
- [ ] Handlers registered pre-connect
- [ ] Single active socket instance
- [ ] Conversations joined before messaging
- [ ] Typing emitted only inside active conversation
- [ ] Token refresh path tested
- [ ] Proper disconnect on logout

## 13. Prompt (For AI Assistants)
```
Fix my iOS Swift Socket.IO chat integration. Backend NestJS gateway namespace /chat. Client must send JWT in Authorization header or handshake auth token. Support events: join_conversation, leave_conversation, send_message, typing. Receive joined_conversation, new_message, user_typing, notification, user_online, user_offline, error. Provide robust Swift singleton code, reconnection, token refresh, error handling, and usage examples.
```

---
**Next Step:** Integrate into your Swift project and run a smoke test against the existing NestJS gateway.
