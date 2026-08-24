package com.vamsi.screener.exception;

/**
 * Thrown when the language model cannot be reached or returns something unusable. Callers catch
 * this and fall back to the deterministic engine, so it never reaches the client.
 */
public class LlmException extends RuntimeException {

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
