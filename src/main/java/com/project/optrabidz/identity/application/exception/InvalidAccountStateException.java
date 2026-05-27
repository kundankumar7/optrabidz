package com.project.optrabidz.identity.application.exception;

public class InvalidAccountStateException extends RuntimeException {
    public InvalidAccountStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
