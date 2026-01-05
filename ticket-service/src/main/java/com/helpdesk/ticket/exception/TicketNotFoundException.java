package com.helpdesk.ticket.exception;

public class TicketNotFoundException extends HelpdeskException {
    public TicketNotFoundException(String id) {
        super("Ticket not found with id: " + id);
    }
}
