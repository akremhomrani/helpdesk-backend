package com.helpdesk.notification.websocket;

import com.helpdesk.notification.model.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Service for sending real-time notifications via WebSocket.
 * Uses STOMP protocol over WebSocket to push notifications to connected clients.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSocketNotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Sends a notification to a specific user via WebSocket.
     *
     * @param userId The user ID to send the notification to
     * @param notification The notification object to send
     */
    public void sendNotificationToUser(String userId, Notification notification) {
        log.debug("Sending WebSocket notification to user: {}", userId);
        messagingTemplate.convertAndSend("/topic/notifications/" + userId, notification);
    }

    /**
     * Broadcasts a notification to all connected users.
     *
     * @param notification The notification object to broadcast
     */
    public void broadcastNotification(Notification notification) {
        log.debug("Broadcasting WebSocket notification");
        messagingTemplate.convertAndSend("/topic/notifications/all", notification);
    }

    /**
     * Sends an unread notification count update to a specific user.
     *
     * @param userId The user ID to send the count to
     * @param count The number of unread notifications
     */
    public void sendUnreadCountToUser(String userId, long count) {
        log.debug("Sending unread count {} to user: {}", count, userId);
        messagingTemplate.convertAndSend("/topic/notifications/" + userId + "/count", count);
    }
}
