# WebSocket Real-Time Notifications Setup

## ✅ Implementation Complete

WebSocket integration has been added for **instant (0 delay) real-time notifications**.

## How It Works

### Backend Flow:
1. **Ticket Created** → Kafka Event
2. **Kafka Consumer** receives event → Saves to MongoDB
3. **WebSocket Push** → Instantly sends to connected frontend clients

### Frontend Flow:
1. **Component Loads** → Connects to WebSocket
2. **WebSocket Receives** → Instantly updates UI
3. **Toast Notification** → Appears immediately
4. **Badge Updates** → Shows new count
5. **List Updates** → New notification appears

## What Was Added

### Backend (notification-service):

1. **WebSocket Dependency** - `spring-boot-starter-websocket` in pom.xml
2. **WebSocketConfig.java** - Configures STOMP over WebSocket
3. **WebSocketNotificationService.java** - Sends real-time messages
4. **Updated TicketEventConsumer.java** - Pushes via WebSocket after Kafka consume

### Frontend (Angular):

1. **WebSocketService** - Manages WebSocket connection
2. **Updated NotificationDropdownComponent** - Subscribes to WebSocket
3. **Libraries** - `sockjs-client` and `@stomp/stompjs`

## Testing

### 1. Install Frontend Dependencies
```bash
cd d:\help-desk
npm install sockjs-client @stomp/stompjs
```

### 2. Restart Backend
```bash
cd d:\helpdesk-backend\notification-service
mvn spring-boot:run
```

### 3. Start Frontend
```bash
cd d:\help-desk
npm start
```

### 4. Test Real-Time Notifications

**Open your app:**
1. Login to the application
2. Keep the app open in browser
3. Create a new ticket (via API or another browser tab)
4. **INSTANTLY** you'll see:
   - ✅ Toast notification popup
   - ✅ Bell badge number increases
   - ✅ New notification appears in dropdown
   - ✅ **No page refresh needed!**

## WebSocket Connection

**Endpoint:** `ws://localhost:8086/ws-notifications`

**Topics:**
- `/topic/notifications/{userId}` - User-specific notifications
- `/topic/notifications/{userId}/count` - Unread count updates

## Advantages Over Polling

| Feature | Polling (5s) | WebSocket |
|---------|-------------|-----------|
| Delay | 0-5 seconds | **0 seconds (instant)** |
| Server Load | High (constant requests) | Low (push only) |
| Bandwidth | High | Low |
| Real-time | No | **Yes** |
| Scalability | Poor | Excellent |

## Architecture

```
Ticket Created
    ↓
Kafka Topic: ticket-created
    ↓
Notification Service (Kafka Consumer)
    ↓
┌─────────────────┬──────────────────┐
│   MongoDB Save  │  WebSocket Push  │
└─────────────────┴──────────────────┘
                         ↓
                  Angular Frontend
                         ↓
              Instant UI Update
```

## Features

✅ **Instant notifications** - 0 delay
✅ **Auto-reconnection** - Reconnects if connection drops
✅ **Toast popups** - Like Facebook
✅ **Badge animation** - Pulsing red badge
✅ **Real-time count** - Updates immediately
✅ **No polling** - More efficient

## Configuration

### Change WebSocket URL (if needed)
In `websocket.service.ts`:
```typescript
const socket = new SockJS('http://localhost:8086/ws-notifications');
```

### Reconnection Settings
```typescript
reconnectDelay: 5000, // Reconnect after 5 seconds
heartbeatIncoming: 4000,
heartbeatOutgoing: 4000,
```

## Troubleshooting

### WebSocket not connecting
1. Check notification-service is running on port 8086
2. Check browser console for WebSocket errors
3. Verify CORS settings allow WebSocket

### Notifications not appearing
1. Check WebSocket is connected (console logs)
2. Verify recipientId matches ('admin')
3. Check backend logs for WebSocket push messages

### Connection keeps dropping
1. Check firewall/proxy settings
2. Verify heartbeat settings
3. Check server logs for errors

## Next Steps

### Optional Enhancements:
1. **User-specific IDs** - Get recipientId from auth service
2. **Connection indicator** - Show WebSocket status
3. **Notification sounds** - Play sound on new notification
4. **Desktop notifications** - Browser notification API
5. **Message history** - Show offline messages on reconnect
6. **Typing indicators** - Show when notifications are being sent
