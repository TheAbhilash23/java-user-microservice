package com.example.user.core.exceptions;

public class TokenTerminatedException extends RuntimeException {
    public TokenTerminatedException(String message) {
        super(message);
    }
}
