package com.creatoros.profile.exceptions;

/**
 * Bad Request Exception
 * 
 * Thrown when a request contains invalid data or violates business rules.
 * This exception typically maps to HTTP 400 Bad Request.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
