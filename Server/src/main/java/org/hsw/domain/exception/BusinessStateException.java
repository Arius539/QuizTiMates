package org.hsw.domain.exception;

public class BusinessStateException extends RuntimeException {
    public BusinessStateException(String message) {
        super(message);
    }

    public BusinessStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
