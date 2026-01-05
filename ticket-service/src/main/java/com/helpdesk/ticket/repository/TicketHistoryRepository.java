package com.helpdesk.ticket.repository;

import com.helpdesk.ticket.entity.TicketHistory;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketHistoryRepository extends MongoRepository<TicketHistory, String> {
    
    // Find history by ticket ID ordered by timestamp descending
    List<TicketHistory> findByTicketIdOrderByTimestampDesc(String ticketId);
    
    // Delete history by ticket ID
    void deleteByTicketId(String ticketId);
}
