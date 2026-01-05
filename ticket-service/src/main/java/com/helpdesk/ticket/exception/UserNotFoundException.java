package com.helpdesk.ticket.exception;

public class UserNotFoundException extends HelpdeskException {
    public UserNotFoundException(String email) {
        super("User not found with email: " + email);
    }
}
