package com.helpdesk.ticket.dto;

import lombok.Builder;
import lombok.Data;
import com.helpdesk.ticket.entity.TicketStatus;
import com.helpdesk.ticket.entity.TicketPriority;
import com.helpdesk.ticket.entity.TicketType;

import java.time.LocalDateTime;

@Data
@Builder
public class TicketResponse {
    private String id;
    private String title;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private String departementId; // Changed from Category to departementId
    private String departementName; // For display purposes
    private TicketType type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private Integer estimatedResolutionTime;
    private String feedback;
    private boolean hasAttachment;
    private String transferredById;
    private String transferredFromDepartementId;
    private String attachmentName;
    private String creatorId; // Keycloak user ID or email
    private String creatorName; // Retrieved from user service
    private String assignedUserId; // Keycloak user ID or email
    private String assignedUserName; // Retrieved from user service
    private String solutionName;
    private LocalDateTime solutionUploadedAt;
    private boolean hasSolution;
}
