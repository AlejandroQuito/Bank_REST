package com.example.bankcards.exception;

public class JwtAuthException extends RuntimeException {
    public JwtAuthException(String message) {
        super(message);
    }
}
