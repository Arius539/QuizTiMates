package org.hsw.protocol.io;

import org.hsw.protocol.message.ProtocolMessage;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public final class ProtocolWriter {
    private final BufferedWriter writer;

    public ProtocolWriter(OutputStream outputStream) {
        writer = new BufferedWriter(new OutputStreamWriter(outputStream, StandardCharsets.UTF_8));
    }

    public void writeMessage(ProtocolMessage message) {
        if (message.content().size() > ProtocolConstants.MAX_CONTENT_FIELDS) {
            throw new ProtocolException("Zu viele Content-Felder.");
        }
        requireValidUnicode(message.errorMessage());
        for (String field : message.content()) {
            requireValidUnicode(field);
        }
        StringBuilder frame = new StringBuilder();
        append(frame, message.version().name());
        append(frame, message.type().name());
        append(frame, Long.toString(message.callId()));
        append(frame, message.method().name());
        append(frame, message.status().name());
        append(frame, message.errorCode().name());
        append(frame, TextValueCodec.encode(message.errorMessage()));
        append(frame, Integer.toString(message.content().size()));
        for (String field : message.content()) append(frame, TextValueCodec.encode(field));
        try {
            writer.write(frame.toString());
            writer.flush();
        } catch (IOException exception) {
            throw new TransportException("Fehler beim Schreiben der Protokollnachricht.", exception);
        }
    }

    private static void append(StringBuilder frame, String field) {
        if (field.length() > ProtocolConstants.MAX_LINE_LENGTH) {
            throw new ProtocolException("Protokollzeile überschreitet Größenlimit.");
        }
        frame.append(field).append('\n');
        if (frame.length() > ProtocolConstants.MAX_MESSAGE_LENGTH) {
            throw new ProtocolException("Protokollnachricht überschreitet Größenlimit.");
        }
    }

    private static void requireValidUnicode(String value) {
        if (value == null) {
            throw new ProtocolException("Textfeld darf nicht null sein.");
        }

        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (Character.isHighSurrogate(character)) {
                if (index + 1 >= value.length() || !Character.isLowSurrogate(value.charAt(index + 1))) {
                    throw new ProtocolException("Textfeld enthält ein ungültiges Unicode-Surrogat.");
                }
                index++;
            } else if (Character.isLowSurrogate(character)) {
                throw new ProtocolException("Textfeld enthält ein ungültiges Unicode-Surrogat.");
            }
        }
    }
}
