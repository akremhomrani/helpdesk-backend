package com.helpdesk.ticket.event;

import com.helpdesk.ticket.entity.TicketPriority;
import com.helpdesk.ticket.entity.TicketStatus;
import com.helpdesk.ticket.entity.TicketType;
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
    private TicketStatus status;
    private TicketPriority priority;
    private TicketType type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String eventType; // CREATED, UPDATED, ASSIGNED, RESOLVED, DELETED, etc.
}
