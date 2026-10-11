package org.hsw.application.error;

import java.util.Objects;

public enum ServiceErrorKind {
    VALIDATION("Bitte überprüfe deine Eingaben."), INVALID_STATE("Diese Aktion ist im aktuellen Zustand nicht möglich."), CONNECTION("Die Verbindung zum Server wurde unterbrochen."), FEATURE_UNAVAILABLE("Diese Funktion ist derzeit nicht verfügbar."), SERVER_ERROR("Der Server konnte die Anfrage nicht verarbeiten."), PROTOCOL_ERROR("Bei der Kommunikation mit dem Server ist ein technischer Fehler aufgetreten."), VIEW_ERROR("Die Ansicht konnte nicht geladen werden.");

    private final String defaultUserMessage;

    ServiceErrorKind(String defaultUserMessage) {
        this.defaultUserMessage = requireUserMessage(defaultUserMessage);
    }

    public String defaultUserMessage() {
        return defaultUserMessage;
    }

    private static String requireUserMessage(String message) {
        Objects.requireNonNull(message, "defaultUserMessage");
        if (message.isBlank()) {
            throw new IllegalArgumentException("Standardbenutzermeldung darf nicht leer sein.");
        }
        return message;
    }
}
