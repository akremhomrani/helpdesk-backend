package com.helpdesk.ticket.service;

import com.helpdesk.ticket.dto.TicketRequest;
import com.helpdesk.ticket.dto.TicketResponse;
import com.helpdesk.ticket.entity.*;
import com.helpdesk.ticket.event.DepartmentResponseEvent;
import com.helpdesk.ticket.event.TicketEvent;
import com.helpdesk.ticket.exception.*;
import com.helpdesk.ticket.kafka.DepartmentKafkaClient;
import com.helpdesk.ticket.repository.TicketHistoryRepository;
import com.helpdesk.ticket.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketHistoryRepository ticketHistoryRepository;
    private final FileStorageService fileStorageService;
    private final KafkaProducerService kafkaProducerService;
    private final DepartmentKafkaClient departmentKafkaClient;

    public TicketResponse createTicket(TicketRequest request, String creatorId, String creatorName) {
        // 1. Validate required fields
        if (request.getTitle() == null || request.getTitle().isEmpty()) {
            throw new InvalidRequestException("Title is required");
        }
        if (request.getDescription() == null || request.getDescription().isEmpty()) {
            throw new InvalidRequestException("Description is required");
        }
        if (request.getType() == null) {
            throw new InvalidRequestException("Ticket type is required");
        }
        if (request.getDepartementId() == null || request.getDepartementId().isEmpty()) {
            throw new InvalidRequestException("Department is required");
        }
        if (request.getPriority() == null) {
            throw new InvalidRequestException("Priority is required");
        }

        // 2. Validate department exists via Kafka
        try {
            DepartmentResponseEvent departmentResponse = departmentKafkaClient.getDepartmentById(request.getDepartementId());
            if (!departmentResponse.isExists()) {
                throw new InvalidRequestException("Department not found: " + request.getDepartementId());
            }
            log.info("Department validated: {} - {}", departmentResponse.getName(), departmentResponse.getDescription());
        } catch (Exception e) {
            log.error("Failed to validate department: {}", request.getDepartementId(), e);
            throw new InvalidRequestException("Failed to validate department. Please try again.");
        }

        // 3. Handle file upload if provided
        String attachmentPath = null;
        if (request.getAttachment() != null && !request.getAttachment().isEmpty()) {
            try {
                attachmentPath = fileStorageService.storeFile(request.getAttachment());
            } catch (FileStorageException e) {
                log.error("Failed to store attachment for ticket", e);
                // Continue without attachment if storage fails
            }
        }

        // 4. Create and save the ticket
        Ticket ticket = Ticket.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(TicketStatus.PENDING)
                .type(request.getType())
                .priority(request.getPriority())
                .departementId(request.getDepartementId())
                .creatorId(creatorId)
                .creatorName(creatorName)
                .attachmentPath(attachmentPath)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        Ticket savedTicket = ticketRepository.save(ticket);

        // 5. Create history record
        TicketHistory history = TicketHistory.builder()
                .ticketId(savedTicket.getId())
                .userId(creatorId)
                .oldStatus(null)
                .newStatus(TicketStatus.PENDING)
                .action("Ticket created")
                .comment("Ticket was created")
                .timestamp(LocalDateTime.now())
                .build();
        ticketHistoryRepository.save(history);

        // 6. Publish Kafka event
        TicketEvent event = buildTicketEvent(savedTicket, "CREATED");
        kafkaProducerService.sendTicketCreatedEvent(event);

        // 7. Return response
        return mapToResponse(savedTicket);
    }

    public List<TicketResponse> getAllTickets() {
        return ticketRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public TicketResponse getTicketById(String id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        return mapToResponse(ticket);
    }

    public TicketResponse updateTicket(String id, TicketRequest request, String updaterId) {
        // 1. Find the existing ticket
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        // Save old values for history
        TicketStatus oldStatus = ticket.getStatus();

        // 2. Update fields if they are provided in the request
        if (request.getTitle() != null && !request.getTitle().isEmpty()) {
            ticket.setTitle(request.getTitle());
        }
        if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            ticket.setDescription(request.getDescription());
        }
        if (request.getType() != null) {
            ticket.setType(request.getType());
        }
        if (request.getDepartementId() != null && !request.getDepartementId().isEmpty()) {
            // Validate new department via Kafka
            try {
                DepartmentResponseEvent departmentResponse = departmentKafkaClient.getDepartmentById(request.getDepartementId());
                if (!departmentResponse.isExists()) {
                    throw new InvalidRequestException("Department not found: " + request.getDepartementId());
                }
                ticket.setDepartementId(request.getDepartementId());
                log.info("Department updated: {} - {}", departmentResponse.getName(), departmentResponse.getDescription());
            } catch (Exception e) {
                log.error("Failed to validate department: {}", request.getDepartementId(), e);
                throw new InvalidRequestException("Failed to validate department. Please try again.");
            }
        }
        if (request.getPriority() != null) {
            ticket.setPriority(request.getPriority());
        }

        // Handle status update
        if (request.getStatus() != null) {
            TicketStatus newStatus = request.getStatus();

            // Validate status transition
            if (newStatus == TicketStatus.CLOSED && ticket.getStatus() != TicketStatus.RESOLVED) {
                throw new InvalidRequestException("Ticket must be RESOLVED before being CLOSED");
            }

            ticket.setStatus(newStatus);

            if (newStatus == TicketStatus.RESOLVED) {
                ticket.setResolvedAt(LocalDateTime.now());
            }
        }

        // 3. Handle file upload if provided
        if (request.getAttachment() != null && !request.getAttachment().isEmpty()) {
            // Remove old file if exists
            if (ticket.getAttachmentPath() != null) {
                try {
                    fileStorageService.deleteFile(ticket.getAttachmentPath());
                } catch (FileStorageException e) {
                    log.error("Failed to delete old attachment", e);
                }
            }
            // Store new file
            String attachmentPath = fileStorageService.storeFile(request.getAttachment());
            ticket.setAttachmentPath(attachmentPath);
        }

        // 4. Update the timestamp
        ticket.setUpdatedAt(LocalDateTime.now());

        // 5. Create history record for general update
        TicketHistory history = TicketHistory.builder()
                .ticketId(ticket.getId())
                .userId(updaterId)
                .oldStatus(oldStatus)
                .newStatus(ticket.getStatus())
                .action("Ticket updated")
                .comment("Ticket details were modified")
                .timestamp(LocalDateTime.now())
                .build();

        ticketHistoryRepository.save(history);

        // 6. Save the updated ticket
        Ticket updatedTicket = ticketRepository.save(ticket);

        // 7. Publish Kafka event
        TicketEvent event = buildTicketEvent(updatedTicket, "UPDATED");
        kafkaProducerService.sendTicketUpdatedEvent(event);

        return mapToResponse(updatedTicket);
    }

    public TicketResponse updateTicketStatus(String id, TicketStatus newStatus, String userId, String feedback) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        // Validate status transition
        if (newStatus == TicketStatus.CLOSED && ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new InvalidRequestException("Ticket must be RESOLVED before being CLOSED");
        }

        TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(newStatus);
        ticket.setUpdatedAt(LocalDateTime.now());

        if (newStatus == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
        }

        // If status is REJECTED, save feedback to ticket
        if (newStatus == TicketStatus.REJECTED && feedback != null && !feedback.isEmpty()) {
            ticket.setFeedback(feedback);
        }

        // Create history record
        TicketHistory history = TicketHistory.builder()
                .ticketId(ticket.getId())
                .userId(userId)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .action("Status changed")
                .comment(feedback)
                .timestamp(LocalDateTime.now())
                .build();

        ticketHistoryRepository.save(history);

        Ticket updatedTicket = ticketRepository.save(ticket);

        // Publish Kafka event
        TicketEvent event = buildTicketEvent(updatedTicket, "UPDATED");
        kafkaProducerService.sendTicketUpdatedEvent(event);

        return mapToResponse(updatedTicket);
    }

    public TicketResponse assignTicket(String ticketId, String assigneeId, String assignerId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        ticket.setAssignedUserId(assigneeId);
        ticket.setUpdatedAt(LocalDateTime.now());

        // Create history record
        TicketHistory history = TicketHistory.builder()
                .ticketId(ticket.getId())
                .userId(assignerId)
                .oldStatus(ticket.getStatus())
                .newStatus(ticket.getStatus())
                .action("Ticket assigned")
                .comment("Ticket assigned to user: " + assigneeId)
                .timestamp(LocalDateTime.now())
                .build();

        ticketHistoryRepository.save(history);

        Ticket updatedTicket = ticketRepository.save(ticket);

        // Publish Kafka event
        TicketEvent event = buildTicketEvent(updatedTicket, "ASSIGNED");
        kafkaProducerService.sendTicketAssignedEvent(event);

        return mapToResponse(updatedTicket);
    }

    public TicketResponse reviewTicket(String id, boolean accept, String comment, String reviewerId) {
        // 1. Find the ticket
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        // 2. Validate ticket status
        if (ticket.getStatus() != TicketStatus.UNDER_REVIEW) {
            throw new InvalidRequestException("Ticket must be in UNDER_REVIEW status for review");
        }

        // 3. Validate comment for rejection
        if (!accept && (comment == null || comment.isEmpty())) {
            throw new InvalidRequestException("Comment is required when rejecting a ticket");
        }

        // 4. Save old status for history
        TicketStatus oldStatus = ticket.getStatus();

        // 5. Determine new status
        TicketStatus newStatus = accept ? TicketStatus.IN_PROGRESS : TicketStatus.REJECTED;

        // 6. Update ticket
        ticket.setStatus(newStatus);
        ticket.setUpdatedAt(LocalDateTime.now());

        // 7. Store feedback if rejecting
        if (!accept) {
            ticket.setFeedback(comment);
        }

        // 8. Create history record
        TicketHistory history = TicketHistory.builder()
                .ticketId(ticket.getId())
                .userId(reviewerId)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .action(accept ? "Ticket accepted" : "Ticket rejected")
                .comment(comment)
                .timestamp(LocalDateTime.now())
                .build();

        ticketHistoryRepository.save(history);

        // 9. Save the updated ticket
        Ticket updatedTicket = ticketRepository.save(ticket);

        // 10. Publish Kafka event
        TicketEvent event = buildTicketEvent(updatedTicket, "UPDATED");
        kafkaProducerService.sendTicketUpdatedEvent(event);

        // 11. Return response
        return mapToResponse(updatedTicket);
    }

    public TicketResponse resubmitTicket(String id, TicketRequest request, String clientId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        // Validate that the requester is the ticket creator
        if (!ticket.getCreatorId().equals(clientId)) {
            throw new InvalidRequestException("Only the ticket creator can resubmit this ticket");
        }

        if (ticket.getStatus() != TicketStatus.REJECTED &&
                ticket.getStatus() != TicketStatus.AWAITING_CLIENT) {
            throw new InvalidRequestException("Ticket must be in REJECTED or AWAITING_CLIENT status for resubmission");
        }

        // Update ticket fields
        if (request.getTitle() != null && !request.getTitle().isEmpty()) {
            ticket.setTitle(request.getTitle());
        }
        if (request.getDescription() != null && !request.getDescription().isEmpty()) {
            ticket.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            ticket.setPriority(request.getPriority());
        }
        if (request.getDepartementId() != null && !request.getDepartementId().isEmpty()) {
            // Validate new department via Kafka
            try {
                DepartmentResponseEvent departmentResponse = departmentKafkaClient.getDepartmentById(request.getDepartementId());
                if (!departmentResponse.isExists()) {
                    throw new InvalidRequestException("Department not found: " + request.getDepartementId());
                }
                ticket.setDepartementId(request.getDepartementId());
                log.info("Department updated: {} - {}", departmentResponse.getName(), departmentResponse.getDescription());
            } catch (Exception e) {
                log.error("Failed to validate department: {}", request.getDepartementId(), e);
                throw new InvalidRequestException("Failed to validate department. Please try again.");
            }
        }

        // Handle file upload if provided
        if (request.getAttachment() != null && !request.getAttachment().isEmpty()) {
            String attachmentPath = fileStorageService.storeFile(request.getAttachment());
            ticket.setAttachmentPath(attachmentPath);
        }

        TicketStatus oldStatus = ticket.getStatus();
        TicketStatus newStatus = TicketStatus.UNDER_REVIEW;

        ticket.setStatus(newStatus);
        ticket.setUpdatedAt(LocalDateTime.now());

        // Create history record
        TicketHistory history = TicketHistory.builder()
                .ticketId(ticket.getId())
                .userId(clientId)
                .oldStatus(oldStatus)
                .newStatus(newStatus)
                .action("Ticket resubmitted by client")
                .comment("Client made updates after rejection")
                .timestamp(LocalDateTime.now())
                .build();

        ticketHistoryRepository.save(history);

        Ticket updatedTicket = ticketRepository.save(ticket);

        // Publish Kafka event
        TicketEvent event = buildTicketEvent(updatedTicket, "UPDATED");
        kafkaProducerService.sendTicketUpdatedEvent(event);

        return mapToResponse(updatedTicket);
    }

    public TicketResponse transferTicket(String ticketId, String newAssigneeId, String transferrerId) {
        // 1. Find the ticket
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        // 2. Save old values for history
        String oldAssigneeId = ticket.getAssignedUserId();
        TicketStatus oldStatus = ticket.getStatus();

        // 3. Update ticket
        ticket.setAssignedUserId(newAssigneeId);
        ticket.setTransferredById(transferrerId);
        ticket.setUpdatedAt(LocalDateTime.now());

        // 4. Create history record
        String comment = String.format("Transferred from user %s to user %s",
                oldAssigneeId != null ? oldAssigneeId : "unassigned",
                newAssigneeId);

        TicketHistory history = TicketHistory.builder()
                .ticketId(ticket.getId())
                .userId(transferrerId)
                .oldStatus(oldStatus)
                .newStatus(ticket.getStatus())
                .action("Ticket transferred")
                .comment(comment)
                .timestamp(LocalDateTime.now())
                .build();

        ticketHistoryRepository.save(history);

        // 5. Save the updated ticket
        Ticket updatedTicket = ticketRepository.save(ticket);

        // 6. Publish Kafka event
        TicketEvent event = buildTicketEvent(updatedTicket, "UPDATED");
        kafkaProducerService.sendTicketUpdatedEvent(event);

        return mapToResponse(updatedTicket);
    }

    public TicketResponse uploadSolution(String ticketId, MultipartFile file, String userId) {
        // 1. Find the ticket
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        // 2. Validate ticket status
        if (ticket.getStatus() != TicketStatus.REJECTED && ticket.getStatus() != TicketStatus.IN_PROGRESS) {
            throw new InvalidRequestException("Solution can only be uploaded for tickets in IN_PROGRESS status");
        }

        // 3. Store the solution file
        String solutionPath = fileStorageService.storeFile(file);

        // 4. Delete old solution if exists
        if (ticket.getSolutionPath() != null) {
            try {
                fileStorageService.deleteFile(ticket.getSolutionPath());
            } catch (FileStorageException e) {
                log.error("Failed to delete old solution file", e);
            }
        }

        // 5. Update ticket and change status to RESOLVED
        TicketStatus oldStatus = ticket.getStatus();
        ticket.setSolutionPath(solutionPath);
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now());
        ticket.setUpdatedAt(LocalDateTime.now());

        // 6. Create history record
        TicketHistory history = TicketHistory.builder()
                .ticketId(ticket.getId())
                .userId(userId)
                .oldStatus(oldStatus)
                .newStatus(TicketStatus.RESOLVED)
                .action("Solution file uploaded and ticket resolved")
                .comment("Solution file added and ticket marked as resolved")
                .timestamp(LocalDateTime.now())
                .build();

        ticketHistoryRepository.save(history);

        // 7. Save the updated ticket
        Ticket updatedTicket = ticketRepository.save(ticket);

        // 8. Publish Kafka event
        TicketEvent event = buildTicketEvent(updatedTicket, "RESOLVED");
        kafkaProducerService.sendTicketResolvedEvent(event);

        return mapToResponse(updatedTicket);
    }

    public void deleteTicket(String id, String requesterId) {
        // 1. Find the ticket
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));

        // 2. Check permissions (only creator should be able to delete)
        if (!requesterId.equals(ticket.getCreatorId())) {
            throw new InvalidRequestException("Only the ticket creator can delete this ticket");
        }

        // 3. Delete associated files if exist
        if (ticket.getAttachmentPath() != null) {
            try {
                fileStorageService.deleteFile(ticket.getAttachmentPath());
            } catch (FileStorageException e) {
                log.error("Failed to delete attachment file: " + ticket.getAttachmentPath(), e);
            }
        }
        if (ticket.getSolutionPath() != null) {
            try {
                fileStorageService.deleteFile(ticket.getSolutionPath());
            } catch (FileStorageException e) {
                log.error("Failed to delete solution file: " + ticket.getSolutionPath(), e);
            }
        }

        // 4. Delete ticket history
        ticketHistoryRepository.deleteByTicketId(id);

        // 5. Delete the ticket
        ticketRepository.delete(ticket);

        // 6. Publish Kafka event
        TicketEvent event = buildTicketEvent(ticket, "DELETED");
        kafkaProducerService.sendTicketDeletedEvent(event);
    }

    public List<TicketResponse> getTicketsByCurrentUser(String userId, TicketStatus status) {
        List<Ticket> tickets;
        if (status != null) {
            tickets = ticketRepository.findByCreatorIdAndStatus(userId, status);
        } else {
            tickets = ticketRepository.findByCreatorIdOrderByCreatedAtDesc(userId);
        }
        return tickets.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TicketResponse> getAssignedTickets(String userId) {
        List<Ticket> tickets = ticketRepository.findByAssignedUserIdOrderByCreatedAtDesc(userId);
        return tickets.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TicketResponse> getAssignedTicketsByStatus(String userId, TicketStatus status) {
        List<Ticket> tickets = ticketRepository.findByAssignedUserIdAndStatusOrderByCreatedAtDesc(userId, status);
        return tickets.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<TicketResponse> getDepartmentTickets(String departmentId, TicketStatus status) {
        List<Ticket> tickets;
        if (status != null) {
            tickets = ticketRepository.findByDepartementIdAndStatusOrderByCreatedAtDesc(departmentId, status);
        } else {
            tickets = ticketRepository.findByDepartementIdOrderByCreatedAtDesc(departmentId);
        }
        return tickets.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private TicketResponse mapToResponse(Ticket ticket) {
        TicketResponse response = TicketResponse.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .type(ticket.getType())
                .departementId(ticket.getDepartementId())
                .creatorId(ticket.getCreatorId())
                .creatorName(ticket.getCreatorName())
                .assignedUserId(ticket.getAssignedUserId())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .resolvedAt(ticket.getResolvedAt())
                .feedback(ticket.getFeedback())
                .transferredById(ticket.getTransferredById())
                .transferredFromDepartementId(ticket.getTransferredFromDepartementId())
                .attachmentName(ticket.getAttachmentPath() != null ?
                        ticket.getAttachmentPath().substring(ticket.getAttachmentPath().lastIndexOf("/") + 1) :
                        null)
                .hasAttachment(ticket.getAttachmentPath() != null)
                .solutionName(ticket.getSolutionPath() != null ?
                        ticket.getSolutionPath().substring(ticket.getSolutionPath().lastIndexOf("/") + 1) :
                        null)
                .hasSolution(ticket.getSolutionPath() != null)
                .build();

        // Fetch department name via Kafka
        try {
            DepartmentResponseEvent departmentResponse = departmentKafkaClient.getDepartmentById(ticket.getDepartementId());
            if (departmentResponse.isExists()) {
                response.setDepartementName(departmentResponse.getName());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch department name for ticket {}: {}", ticket.getId(), e.getMessage());
            // Continue without department name
        }

        return response;
    }

    private TicketEvent buildTicketEvent(Ticket ticket, String eventType) {
        return TicketEvent.builder()
                .ticketId(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .creatorId(ticket.getCreatorId())
                .creatorName(ticket.getCreatorName())
                .assignedUserId(ticket.getAssignedUserId())
                .departementId(ticket.getDepartementId())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .type(ticket.getType())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .eventType(eventType)
                .build();
    }
}
