package com.helpdesk.notification.kafka;

import com.helpdesk.notification.client.DepartmentServiceClient;
import com.helpdesk.notification.client.UserServiceClient;
import com.helpdesk.notification.event.TicketEvent;
import com.helpdesk.notification.model.Notification;
import com.helpdesk.notification.model.NotificationType;
import com.helpdesk.notification.service.NotificationService;
import com.helpdesk.notification.websocket.WebSocketNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketEventConsumer {

    private final NotificationService notificationService;
    private final WebSocketNotificationService webSocketNotificationService;
    private final DepartmentServiceClient departmentServiceClient;
    private final UserServiceClient userServiceClient;

    /**
     * Handles ticket creation events and notifies admin users.
     *
     * @param event The ticket creation event from Kafka
     */
    @KafkaListener(
            topics = "ticket-created",
            groupId = "notification-service-group",
            containerFactory = "ticketEventKafkaListenerContainerFactory"
    )
    public void handleTicketCreated(TicketEvent event) {
        log.info("Processing ticket-created event for ticket ID: {}", event.getTicketId());
        
        try {
            String creatorDisplay = event.getCreatorName() != null && !event.getCreatorName().isEmpty() 
                    ? event.getCreatorName() 
                    : event.getCreatorId();
            
            // Notify admin
            Notification notification = createNotification(
                    "admin",
                    "New Ticket Created",
                    String.format("A new ticket '%s' has been created by %s", event.getTitle(), creatorDisplay),
                    NotificationType.TICKET_CREATED,
                    event.getTicketId()
            );
            
            sendNotificationAndUpdateCount("admin", notification);
            log.info("Notification sent to admin for ticket: {}", event.getTicketId());
            
            // Notify technical support in department
            if (event.getDepartementId() != null) {
                notifyTechnicalSupport(event, creatorDisplay);
            }
            
        } catch (Exception e) {
            log.error("Error processing ticket-created event for ticket ID {}: {}", 
                    event.getTicketId(), e.getMessage(), e);
        }
    }

    /**
     * Handles ticket update events and notifies admin users.
     *
     * @param event The ticket update event from Kafka
     */
    @KafkaListener(
            topics = "ticket-updated",
            groupId = "notification-service-group",
            containerFactory = "ticketEventKafkaListenerContainerFactory"
    )
    public void handleTicketUpdated(TicketEvent event) {
        log.info("Processing ticket-updated event for ticket ID: {}", event.getTicketId());
        
        try {
            Notification notification = createNotification(
                    "admin",
                    "Ticket Updated",
                    String.format("Ticket '%s' has been updated", event.getTitle()),
                    NotificationType.TICKET_UPDATED,
                    event.getTicketId()
            );
            
            sendNotificationAndUpdateCount("admin", notification);
            log.info("Notification sent to admin for updated ticket: {}", event.getTicketId());
            
        } catch (Exception e) {
            log.error("Error processing ticket-updated event for ticket ID {}: {}", 
                    event.getTicketId(), e.getMessage(), e);
        }
    }

    /**
     * Handles ticket assignment events and notifies both assigned user and admin.
     *
     * @param event The ticket assignment event from Kafka
     */
    @KafkaListener(
            topics = "ticket-assigned",
            groupId = "notification-service-group",
            containerFactory = "ticketEventKafkaListenerContainerFactory"
    )
    public void handleTicketAssigned(TicketEvent event) {
        log.info("Processing ticket-assigned event for ticket ID: {}", event.getTicketId());
        
        try {
            // Notify assigned user
            if (event.getAssignedUserId() != null) {
                Notification assigneeNotification = createNotification(
                        event.getAssignedUserId(),
                        "Ticket Assigned to You",
                        String.format("You have been assigned to ticket '%s'", event.getTitle()),
                        NotificationType.TICKET_ASSIGNED,
                        event.getTicketId()
                );
                
                sendNotificationAndUpdateCount(event.getAssignedUserId(), assigneeNotification);
                log.info("Notification sent to user {} for ticket assignment: {}", 
                        event.getAssignedUserId(), event.getTicketId());
            }
            
            // Notify admin
            Notification adminNotification = createNotification(
                    "admin",
                    "Ticket Assigned",
                    String.format("Ticket '%s' has been assigned", event.getTitle()),
                    NotificationType.TICKET_ASSIGNED,
                    event.getTicketId()
            );
            
            sendNotificationAndUpdateCount("admin", adminNotification);
            log.info("Notification sent to admin for ticket assignment: {}", event.getTicketId());
            
        } catch (Exception e) {
            log.error("Error processing ticket-assigned event for ticket ID {}: {}", 
                    event.getTicketId(), e.getMessage(), e);
        }
    }

    /**
     * Handles ticket resolved events and notifies the ticket creator (client).
     *
     * @param event The ticket resolved event from Kafka
     */
    @KafkaListener(
            topics = "ticket-resolved",
            groupId = "notification-service-group",
            containerFactory = "ticketEventKafkaListenerContainerFactory"
    )
    public void handleTicketResolved(TicketEvent event) {
        log.info("Processing ticket-resolved event for ticket ID: {}", event.getTicketId());
        
        try {
            // Notify the ticket creator (client)
            if (event.getCreatorId() != null) {
                String creatorDisplay = event.getCreatorName() != null && !event.getCreatorName().isEmpty() 
                        ? event.getCreatorName() 
                        : "Client";
                
                Notification clientNotification = createNotification(
                        event.getCreatorId(),
                        "Your Ticket Has Been Resolved",
                        String.format("Your ticket '%s' has been resolved. A solution has been provided.", event.getTitle()),
                        NotificationType.TICKET_UPDATED,
                        event.getTicketId()
                );
                
                sendNotificationAndUpdateCount(event.getCreatorId(), clientNotification);
                log.info("Notification sent to ticket creator {} for resolved ticket: {}", 
                        event.getCreatorId(), event.getTicketId());
            }
            
            // Also notify admin
            Notification adminNotification = createNotification(
                    "admin",
                    "Ticket Resolved",
                    String.format("Ticket '%s' has been resolved", event.getTitle()),
                    NotificationType.TICKET_UPDATED,
                    event.getTicketId()
            );
            
            sendNotificationAndUpdateCount("admin", adminNotification);
            log.info("Notification sent to admin for resolved ticket: {}", event.getTicketId());
            
        } catch (Exception e) {
            log.error("Error processing ticket-resolved event for ticket ID {}: {}", 
                    event.getTicketId(), e.getMessage(), e);
        }
    }

    /**
     * Helper method to create a notification object.
     */
    private Notification createNotification(String recipientId, String title, String message,
                                           NotificationType type, String ticketId) {
        Notification notification = new Notification();
        notification.setRecipientId(recipientId);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setType(type);
        notification.setRelatedEntityId(ticketId);
        notification.setRelatedEntityType("TICKET");
        return notification;
    }

    /**
     * Helper method to save notification and send via WebSocket with unread count.
     */
    private void sendNotificationAndUpdateCount(String userId, Notification notification) {
        notificationService.createNotification(notification);
        webSocketNotificationService.sendNotificationToUser(userId, notification);
        
        long unreadCount = notificationService.getUnreadCount(userId);
        webSocketNotificationService.sendUnreadCountToUser(userId, unreadCount);
    }

    /**
     * Notify technical support users in the ticket's department.
     */
    private void notifyTechnicalSupport(TicketEvent event, String creatorDisplay) {
        try {
            log.info("[DEBUG] Starting technical support notification for ticket {} in department {}", 
                    event.getTicketId(), event.getDepartementId());
            
            // Get users in department
            List<String> userIds = departmentServiceClient.getDepartmentUserIds(event.getDepartementId());
            
            if (userIds == null || userIds.isEmpty()) {
                log.info("[DEBUG] No users in department {} for ticket {}", event.getDepartementId(), event.getTicketId());
                return;
            }
            
            log.info("[DEBUG] Found {} users in department, checking for technical support", userIds.size());
            
            // Notify each technical support user
            int notifiedCount = 0;
            for (String userId : userIds) {
                log.info("[DEBUG] Checking user: {}", userId);
                if (userServiceClient.hasTechnicalSupportRole(userId)) {
                    Notification techNotification = createNotification(
                            userId,
                            "New Ticket in Your Department",
                            String.format("A new ticket '%s' has been created by %s", event.getTitle(), creatorDisplay),
                            NotificationType.TICKET_CREATED,
                            event.getTicketId()
                    );
                    
                    sendNotificationAndUpdateCount(userId, techNotification);
                    notifiedCount++;
                    log.info("[DEBUG] Notification sent to technical support user {} for ticket {}", userId, event.getTicketId());
                } else {
                    log.info("[DEBUG] User {} does not have TECH_SUPPORT role", userId);
                }
            }
            log.info("[DEBUG] Notified {} technical support users for ticket {}", notifiedCount, event.getTicketId());
        } catch (Exception e) {
            log.error("[DEBUG] Error notifying technical support for ticket {}: {}", event.getTicketId(), e.getMessage(), e);
        }
    }
}
