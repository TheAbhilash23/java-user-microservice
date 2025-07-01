package com.example.user.core.exceptions;

public class SessionStillActiveException extends RuntimeException {
    public SessionStillActiveException(String message) {
        super(message);
    }
}
