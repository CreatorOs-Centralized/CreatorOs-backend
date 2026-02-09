package com.creatoros.profile.exceptions;

/**
 * Resource Not Found Exception
 * 
 * Thrown when a requested resource (profile, social link, etc.) is not found.
 * This exception typically maps to HTTP 404 Not Found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
