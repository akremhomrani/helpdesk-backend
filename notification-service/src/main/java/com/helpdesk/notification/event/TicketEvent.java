package com.helpdesk.notification.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketEvent {
    private String ticketId;
    private String title;
    private String description;
    private String creatorId;
    private String creatorName;
    private String assignedUserId;
    private String departementId;
    private String status;
    private String priority;
    private String type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String eventType; // CREATED, UPDATED, ASSIGNED, RESOLVED, DELETED, etc.
}
