package com.vamsi.screener.exception;

/** Thrown when a resume, job description or screening result does not exist. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String type, Object id) {
        return new ResourceNotFoundException(type + " " + id + " was not found");
    }
}
