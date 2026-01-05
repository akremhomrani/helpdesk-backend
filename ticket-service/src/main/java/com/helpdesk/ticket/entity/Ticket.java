package com.helpdesk.ticket.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ticket {
    @Id
    private String id;
    private String title;
    private String description;
    private String creatorId; // Keycloak user id or email
    private String creatorName; // User's full name from Keycloak
    private String assignedUserId; // Keycloak user id or email
    private String departementId; // Department id from departement-service
    private TicketStatus status;
    private TicketPriority priority;
    private TicketType type;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime resolvedAt;
    private String attachmentPath;
    private Integer estimatedResolutionTime;
    private String transferredById;
    private String transferredFromDepartementId;
    private String feedback;
    private String solutionPath;
}
