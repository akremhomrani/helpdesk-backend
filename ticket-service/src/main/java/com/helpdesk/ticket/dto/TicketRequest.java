package com.helpdesk.ticket.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;
import com.helpdesk.ticket.entity.TicketPriority;
import com.helpdesk.ticket.entity.TicketStatus;
import com.helpdesk.ticket.entity.TicketType;

@Data
public class TicketRequest {
    private String title;
    private String description;
    private TicketType type;
    private String departementId; // Changed from Category to departementId
    private TicketPriority priority;
    private MultipartFile attachment;
    private TicketStatus status;
}
