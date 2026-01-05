package com.helpdesk.ticket.repository;

import com.helpdesk.ticket.entity.Ticket;
import com.helpdesk.ticket.entity.TicketStatus;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketRepository extends MongoRepository<Ticket, String> {
    
    // Find tickets by creator
    List<Ticket> findByCreatorIdOrderByCreatedAtDesc(String creatorId);
    
    // Find tickets by creator and status
    @Query("{ 'creatorId': ?0, $or: [ { 'status': ?1 }, { ?1: null } ] }")
    List<Ticket> findByCreatorIdAndStatus(String creatorId, TicketStatus status);
    
    // Find tickets by department
    List<Ticket> findByDepartementIdOrderByCreatedAtDesc(String departementId);
    
    // Find tickets by department and status
    List<Ticket> findByDepartementIdAndStatusOrderByCreatedAtDesc(String departementId, TicketStatus status);
    
    // Find tickets assigned to user
    List<Ticket> findByAssignedUserIdOrderByCreatedAtDesc(String assignedUserId);
    
    // Find tickets assigned to user by status
    List<Ticket> findByAssignedUserIdAndStatusOrderByCreatedAtDesc(String assignedUserId, TicketStatus status);
    
    // Find all tickets ordered by created date
    List<Ticket> findAllByOrderByCreatedAtDesc();
}
