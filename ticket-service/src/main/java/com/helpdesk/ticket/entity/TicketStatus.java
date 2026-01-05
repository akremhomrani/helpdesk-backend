package com.helpdesk.ticket.entity;

public enum TicketStatus {
    PENDING,           // Initial state when created
    UNDER_REVIEW,      // When assigned to developer for initial review
    IN_PROGRESS,       // Developer accepted and working on it
    REJECTED,          // Developer rejected with comments
    AWAITING_CLIENT,   // Waiting for client updates after rejection
    RESOLVED,          // Ticket completed
    CLOSED             // Ticket closed (after resolution or abandonment)
}
