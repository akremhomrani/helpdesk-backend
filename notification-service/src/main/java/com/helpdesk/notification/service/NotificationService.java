package com.helpdesk.notification.service;

import com.helpdesk.notification.model.Notification;
import com.helpdesk.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing notifications.
 * Provides CRUD operations and business logic for notification handling.
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Notification createNotification(Notification notification) {
        return notificationRepository.save(notification);
    }

    public List<Notification> getAllNotifications() {
        return notificationRepository.findAll();
    }

    public List<Notification> getNotificationsByRecipient(String recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
    }

    public List<Notification> getUnreadNotificationsByRecipient(String recipientId) {
        return notificationRepository.findByRecipientIdAndIsReadOrderByCreatedAtDesc(recipientId, false);
    }

    public long getUnreadCount(String recipientId) {
        return notificationRepository.countByRecipientIdAndIsRead(recipientId, false);
    }

    public Optional<Notification> markAsRead(String notificationId) {
        Optional<Notification> notification = notificationRepository.findById(notificationId);
        notification.ifPresent(n -> {
            n.setRead(true);
            notificationRepository.save(n);
        });
        return notification;
    }

    public void markAllAsRead(String recipientId) {
        List<Notification> notifications = notificationRepository
                .findByRecipientIdAndIsReadOrderByCreatedAtDesc(recipientId, false);
        notifications.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(notifications);
    }

    public void deleteNotification(String notificationId) {
        notificationRepository.deleteById(notificationId);
    }
}
