package org.hsw.protocol.io;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ContentWriter {

    private final List<String> fields;

    public ContentWriter() {
        this.fields = new ArrayList<>();
    }

    public void writeString(String value) {
        add(value);
    }

    public void writeChar(char value) {
        add(String.valueOf(value));
    }

    public void writeInt(int value) {
        add(Integer.toString(value));
    }

    public void writeBoolean(boolean value) {
        add(Boolean.toString(value));
    }

    public <E extends Enum<E>> void writeEnum(E value) {
        add(value.name());
    }

    public void writeUUID(UUID value) {
        if (value == null) {
            throw new ProtocolException("UUID darf nicht null sein.");
        }

        fields.add(value.toString());
    }

    public List<String> getFields() {
        return List.copyOf(fields);
    }

    private void add(String value) {
        if (value == null) {
            throw new ProtocolException("Content-Feld darf nicht null sein.");
        }
        fields.add(value);
    }
}
