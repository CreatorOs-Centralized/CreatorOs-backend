package com.creatoros.profile.dtos.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * Error Response DTO
 * 
 * Standard error response format for API errors.
 */
@Getter
@Builder
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;
}
