package org.hsw.application.error;

import java.util.Objects;

public final class ServiceException extends RuntimeException {
    private final ServiceErrorKind kind;

    public ServiceException(ServiceErrorKind kind) {
        this(kind, defaultUserMessage(kind), null);
    }

    public ServiceException(ServiceErrorKind kind, Throwable cause) {
        this(kind, cause.getMessage(), cause);
    }

    public ServiceException(ServiceErrorKind kind, String userMessage) {
        this(kind, userMessage, null);
    }

    public ServiceException(ServiceErrorKind kind, String userMessage, Throwable cause) {
        super(requireUserMessage(userMessage), cause);
        this.kind = Objects.requireNonNull(kind, "kind");
    }

    public ServiceErrorKind kind() {
        return kind;
    }

    public String kindMessage() {
        return kind.defaultUserMessage();
    }

    public String userMessage() {
        return getMessage();
    }

    private static String defaultUserMessage(ServiceErrorKind kind) {
        return Objects.requireNonNull(kind, "kind").defaultUserMessage();
    }

    private static String requireUserMessage(String message) {
        Objects.requireNonNull(message, "userMessage");
        if (message.isBlank()) {
            throw new IllegalArgumentException("Benutzermeldung darf nicht leer sein.");
        }
        return message;
    }
}
