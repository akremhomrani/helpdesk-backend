package com.helpdesk.ticket.controller;

import com.helpdesk.ticket.dto.ErrorResponse;
import com.helpdesk.ticket.dto.TicketRequest;
import com.helpdesk.ticket.dto.TicketResponse;
import com.helpdesk.ticket.entity.TicketHistory;
import com.helpdesk.ticket.entity.TicketPriority;
import com.helpdesk.ticket.entity.TicketStatus;
import com.helpdesk.ticket.entity.TicketType;
import com.helpdesk.ticket.exception.FileStorageException;
import com.helpdesk.ticket.exception.InvalidRequestException;
import com.helpdesk.ticket.exception.TicketNotFoundException;
import com.helpdesk.ticket.exception.UserNotFoundException;
import com.helpdesk.ticket.service.FileStorageService;
import com.helpdesk.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final FileStorageService fileStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createTicket(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam TicketType type,
            @RequestParam String departementId,
            @RequestParam TicketPriority priority,
            @RequestParam(required = false) MultipartFile attachment,
            @AuthenticationPrincipal Jwt jwt) {

        try {
            String userId = extractUserId(jwt);
            String userName = extractUserName(jwt);

            TicketRequest request = new TicketRequest();
            request.setTitle(title);
            request.setDescription(description);
            request.setType(type);
            request.setDepartementId(departementId);
            request.setPriority(priority);
            request.setAttachment(attachment);

            TicketResponse response = ticketService.createTicket(request, userId, userName);
            return ResponseEntity.ok(response);
        } catch (InvalidRequestException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to create ticket", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to create ticket"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAllTickets() {
        try {
            List<TicketResponse> tickets = ticketService.getAllTickets();
            return ResponseEntity.ok(tickets);
        } catch (Exception e) {
            log.error("Failed to retrieve tickets", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to retrieve tickets"));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTicketById(@PathVariable String id) {
        try {
            TicketResponse ticket = ticketService.getTicketById(id);
            return ResponseEntity.ok(ticket);
        } catch (TicketNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to retrieve ticket", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to retrieve ticket"));
        }
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateTicket(
            @PathVariable String id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) TicketType type,
            @RequestParam(required = false) String departementId,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) MultipartFile attachment,
            @AuthenticationPrincipal Jwt jwt) {

        try {
            String userId = extractUserId(jwt);

            TicketRequest request = new TicketRequest();
            request.setTitle(title);
            request.setDescription(description);
            request.setType(type);
            request.setDepartementId(departementId);
            request.setPriority(priority);
            request.setStatus(status);
            request.setAttachment(attachment);

            TicketResponse response = ticketService.updateTicket(id, request, userId);
            return ResponseEntity.ok(response);

        } catch (TicketNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (UserNotFoundException | InvalidRequestException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (FileStorageException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to store file: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update ticket", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to update ticket"));
        }
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<?> updateTicketStatus(
            @PathVariable String id,
            @RequestParam TicketStatus status,
            @RequestParam(required = false) String feedback,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            String userId = extractUserId(jwt);
            
            if (status == null) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Status is required"));
            }

            TicketResponse ticket = ticketService.updateTicketStatus(id, status, userId, feedback);
            return ResponseEntity.ok(ticket);
        } catch (InvalidRequestException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (TicketNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to update ticket status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to update ticket status"));
        }
    }

    @PatchMapping("/{id}/assign")
    public ResponseEntity<?> assignTicket(
            @PathVariable String id,
            @RequestParam String assigneeId,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            String assignerId = extractUserId(jwt);
            
            if (assigneeId == null || assigneeId.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Assignee ID is required"));
            }

            TicketResponse ticket = ticketService.assignTicket(id, assigneeId, assignerId);
            return ResponseEntity.ok(ticket);
        } catch (InvalidRequestException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (TicketNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to assign ticket", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to assign ticket"));
        }
    }

    @PostMapping("/{id}/review")
    public ResponseEntity<?> reviewTicket(
            @PathVariable String id,
            @RequestParam boolean accept,
            @RequestParam(required = false) String comment,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            String reviewerId = extractUserId(jwt);
            
            if (!accept && (comment == null || comment.isEmpty())) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponse("Comment is required when rejecting a ticket"));
            }

            TicketResponse response = ticketService.reviewTicket(id, accept, comment, reviewerId);
            return ResponseEntity.ok(response);
        } catch (InvalidRequestException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (TicketNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to process ticket review", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to process ticket review"));
        }
    }

    @PostMapping("/{id}/resubmit")
    public ResponseEntity<?> resubmitTicket(
            @PathVariable String id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String departementId,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) MultipartFile attachment,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            String clientId = extractUserId(jwt);

            TicketRequest request = new TicketRequest();
            request.setTitle(title);
            request.setDescription(description);
            request.setDepartementId(departementId);
            request.setPriority(priority);
            request.setAttachment(attachment);

            TicketResponse response = ticketService.resubmitTicket(id, request, clientId);
            return ResponseEntity.ok(response);
        } catch (InvalidRequestException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (TicketNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (FileStorageException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to store file: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to resubmit ticket", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to resubmit ticket"));
        }
    }

    @GetMapping("/my-tickets")
    public ResponseEntity<?> getCurrentUserTickets(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) TicketStatus status) {
        try {
            String userId = extractUserId(jwt);
            List<TicketResponse> tickets = ticketService.getTicketsByCurrentUser(userId, status);
            return ResponseEntity.ok(tickets);
        } catch (Exception e) {
            log.error("Failed to retrieve user tickets", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to retrieve user tickets"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTicket(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            String requesterId = extractUserId(jwt);
            ticketService.deleteTicket(id, requesterId);
            return ResponseEntity.noContent().build();
        } catch (TicketNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (UserNotFoundException | InvalidRequestException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to delete ticket", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to delete ticket"));
        }
    }

    @GetMapping("/assigned")
    public ResponseEntity<?> getAssignedTickets(@AuthenticationPrincipal Jwt jwt) {
        try {
            String userId = extractUserId(jwt);
            List<TicketResponse> tickets = ticketService.getAssignedTickets(userId);
            return ResponseEntity.ok(tickets);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to retrieve assigned tickets", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to retrieve assigned tickets"));
        }
    }

    @GetMapping("/assigned/{status}")
    public ResponseEntity<?> getAssignedTicketsByStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable TicketStatus status) {
        try {
            String userId = extractUserId(jwt);
            List<TicketResponse> tickets = ticketService.getAssignedTicketsByStatus(userId, status);
            return ResponseEntity.ok(tickets);
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to retrieve assigned tickets by status", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to retrieve assigned tickets by status"));
        }
    }

    @GetMapping("/{id}/attachment")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable String id) {
        try {
            TicketResponse ticket = ticketService.getTicketById(id);

            if (!ticket.isHasAttachment() || ticket.getAttachmentName() == null) {
                throw new InvalidRequestException("Ticket has no attachment");
            }

            Resource resource = fileStorageService.loadFileAsResource(ticket.getAttachmentName());

            String contentType = "application/octet-stream";
            try {
                contentType = Files.probeContentType(Paths.get(resource.getFile().getAbsolutePath()));
            } catch (IOException ex) {
                log.warn("Could not determine file type.");
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (TicketNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to download attachment", e);
            throw new RuntimeException("Failed to download attachment", e);
        }
    }

    @PatchMapping("/{id}/transfer")
    public ResponseEntity<?> transferTicket(
            @PathVariable String id,
            @RequestParam String newAssigneeId,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            String transferrerId = extractUserId(jwt);
            
            if (newAssigneeId == null || newAssigneeId.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("New assignee ID is required"));
            }

            TicketResponse response = ticketService.transferTicket(id, newAssigneeId, transferrerId);
            return ResponseEntity.ok(response);
        } catch (InvalidRequestException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (TicketNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to transfer ticket", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to transfer ticket"));
        }
    }

    @PostMapping("/{id}/solution")
    public ResponseEntity<?> uploadSolution(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            String userId = extractUserId(jwt);
            
            if (file == null || file.isEmpty()) {
                return ResponseEntity.badRequest().body(new ErrorResponse("Solution file is required"));
            }

            TicketResponse response = ticketService.uploadSolution(id, file, userId);
            return ResponseEntity.ok(response);
        } catch (InvalidRequestException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (TicketNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse(e.getMessage()));
        } catch (FileStorageException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to store solution file: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Failed to upload solution", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("Failed to upload solution"));
        }
    }

    @GetMapping("/{id}/solution")
    public ResponseEntity<Resource> downloadSolution(@PathVariable String id) {
        try {
            TicketResponse ticket = ticketService.getTicketById(id);

            if (!ticket.isHasSolution() || ticket.getSolutionName() == null) {
                throw new InvalidRequestException("Ticket has no solution");
            }

            Resource resource = fileStorageService.loadFileAsResource(ticket.getSolutionName());

            String contentType = "application/octet-stream";
            try {
                contentType = Files.probeContentType(Paths.get(resource.getFile().getAbsolutePath()));
            } catch (IOException ex) {
                log.warn("Could not determine file type.");
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + resource.getFilename() + "\"")
                    .body(resource);

        } catch (TicketNotFoundException | InvalidRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to download solution", e);
            throw new RuntimeException("Failed to download solution", e);
        }
    }

    /**
     * Extract user ID from Keycloak JWT token.
     * You can extract from "sub" (subject) or "preferred_username" or "email" claim.
     * Returns a default user ID if JWT is not available (for testing without authentication).
     */
    private String extractUserId(Jwt jwt) {
        if (jwt == null) {
            // For development/testing without authentication
            return "test-user-id";
        }
        
        // Option 1: Use subject (unique user ID in Keycloak)
        String userId = jwt.getSubject();
        
        // Option 2: Use email if preferred
        // String userId = jwt.getClaimAsString("email");
        
        // Option 3: Use preferred username
        // String userId = jwt.getClaimAsString("preferred_username");
        
        if (userId == null || userId.isEmpty()) {
            throw new InvalidRequestException("Unable to extract user ID from token");
        }
        
        return userId;
    }

    private String extractUserName(Jwt jwt) {
        if (jwt == null) {
            return "Test User";
        }
        
        // Get full name from "name" claim
        String userName = jwt.getClaimAsString("name");
        
        // If not available, construct from given_name and family_name
        if (userName == null || userName.isEmpty()) {
            String givenName = jwt.getClaimAsString("given_name");
            String familyName = jwt.getClaimAsString("family_name");
            
            if (givenName != null && familyName != null) {
                userName = givenName + " " + familyName;
            } else if (givenName != null) {
                userName = givenName;
            } else {
                userName = jwt.getClaimAsString("preferred_username");
            }
        }
        
        return userName != null ? userName : "Unknown User";
    }
}
