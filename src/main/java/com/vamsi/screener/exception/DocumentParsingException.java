package com.vamsi.screener.exception;

/** Thrown when an uploaded file cannot be turned into usable resume text. */
public class DocumentParsingException extends RuntimeException {

    public DocumentParsingException(String message) {
        super(message);
    }

    public DocumentParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
