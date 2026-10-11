package org.hsw.protocol.io;

import java.util.List;
import java.util.UUID;

public final class ContentReader {

    private final List<String> fields;
    private int position;

    public ContentReader(List<String> fields) {
        this.fields = fields;
        this.position = 0;
    }

    public String readString() {
        return next();
    }

    public char readChar() {
        String value = next();
        if (value.length() != 1) {
            throw new ContentFormatException("Genau ein Zeichen erwartet.");
        }
        return value.charAt(0);
    }

    public int readInt() {
        String value = next();

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new ContentFormatException("Integer erwartet, erhalten: " + value, e);
        }
    }

    public boolean readBoolean() {
        String value = next();

        if (value.equals("true")) {
            return true;
        }

        if (value.equals("false")) {
            return false;
        }

        throw new ContentFormatException("Boolean erwartet, erhalten: " + value);
    }

    public <E extends Enum<E>> E readEnum(Class<E> type) {
        String value = next();

        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException e) {
            throw new ContentFormatException("Ungültiger Enum-Wert: " + value, e);
        }
    }

    public UUID readUUID() {
        String value = next();

        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new ContentFormatException("UUID erwartet, erhalten: " + value, e);
        }
    }

    public void requireEnd() {
        if (position != fields.size()) {
            throw new ContentFormatException("Unerwartete zusätzliche Content-Felder.");
        }
    }

    private String next() {
        if (position >= fields.size()) {
            throw new ContentFormatException("Zu wenige Content-Felder.");
        }

        return fields.get(position++);
    }
}
