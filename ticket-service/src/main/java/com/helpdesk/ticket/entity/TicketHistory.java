package com.helpdesk.ticket.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "ticket_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TicketHistory {
    @Id
    private String id;
    
    private String ticketId; // Reference to Ticket
    private String userId; // Keycloak user ID or email
    private String userName; // Retrieved from user service
    private TicketStatus oldStatus;
    private TicketStatus newStatus;
    private String action;
    
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    private String comment;
}
