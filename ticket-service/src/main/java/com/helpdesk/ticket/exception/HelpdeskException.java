package com.helpdesk.ticket.exception;

public class HelpdeskException extends RuntimeException {
    public HelpdeskException(String message) {
        super(message);
    }

    public HelpdeskException(String message, Throwable cause) {
        super(message, cause);
    }
}
