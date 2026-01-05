package com.helpdesk.ticket.exception;

public class FileStorageException extends HelpdeskException {
    public FileStorageException(String message) {
        super(message);
    }

    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
