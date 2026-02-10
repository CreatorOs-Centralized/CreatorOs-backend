package com.creatoros.publishing.exceptions;

public class PublishException extends RuntimeException {

    private String errorCode;
    private Object details;

    public PublishException(String message) {
        super(message);
    }

    public PublishException(String message, Throwable cause) {
        super(message, cause);
    }

    public PublishException(String message, String errorCode) {
        super(message);
        this.errorCode = errorCode;
    }

    public PublishException(String message, String errorCode, Object details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public String getErrorCode() { return errorCode; }
    public void setErrorCode(String errorCode) { this.errorCode = errorCode; }

    public Object getDetails() { return details; }
    public void setDetails(Object details) { this.details = details; }
}
