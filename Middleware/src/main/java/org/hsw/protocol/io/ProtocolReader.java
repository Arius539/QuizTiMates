package org.hsw.protocol.io;

import org.hsw.protocol.message.*;

import java.io.*;
import java.nio.charset.*;
import java.util.*;

public final class ProtocolReader {
    private final BufferedReader reader;
    private int messageLength;

    public ProtocolReader(InputStream inputStream) {
        this.reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT)));
        this.messageLength = 0;
    }

    public ProtocolMessage readMessage() {
        messageLength = 0;
        try {
            String first = readLine();
            if (first == null) {
                return null;
            }
            ProtocolVersion version = parse(ProtocolVersion.class, first);
            MessageType type = parse(MessageType.class, requiredLine());
            long callId = Long.parseLong(requiredLine());
            if (callId < 1) {
                throw new ProtocolException("Call-ID muss positiv sein.");
            }
            MethodId method = parse(MethodId.class, requiredLine());
            MessageStatus status = parse(MessageStatus.class, requiredLine());
            ErrorCode errorCode = parse(ErrorCode.class, requiredLine());
            String errorMessage = TextValueCodec.decode(requiredLine());
            int fieldCount = Integer.parseInt(requiredLine());
            if (fieldCount < 0 || fieldCount > ProtocolConstants.MAX_CONTENT_FIELDS) {
                throw new ProtocolException("Ungültige Content-Anzahl.");
            }
            List<String> content = new ArrayList<>(fieldCount);
            for (int index = 0; index < fieldCount; index++) {
                content.add(TextValueCodec.decode(requiredLine()));
            }
            return new ProtocolMessage(version, type, callId, method, status, errorCode, errorMessage, content);
        } catch (CharacterCodingException exception) {
            throw new ProtocolException("Ungültige UTF-8-Protokollnachricht.", exception);
        } catch (NumberFormatException exception) {
            throw new ProtocolException("Ungültige oder unvollständige Protokollnachricht.", exception);
        } catch (IOException exception) {
            throw new TransportException("Fehler beim Lesen der Protokollnachricht.", exception);
        }
    }

    private String requiredLine() throws IOException {
        String value = readLine();
        if (value == null) {
            throw new ProtocolException("Nachricht mitten im Stream beendet.");
        }
        return value;
    }

    /**
     * liest vom BufferedReader bis zum Zeilenumbruch {@code \n}  und gibt den gelesenen String zurück
     *
     * @return Zeile im BufferedReader
     * @throws IOException, wenn während des Lesevorgangs ein Fehler auftritt
     */
    private String readLine() throws IOException {
        StringBuilder line = new StringBuilder();
        while (true) {
            int character = reader.read();
            if (character == -1) {
                if (line.isEmpty()) {
                    return null;
                }
                throw new ProtocolException("Unvollständige Protokollzeile.");
            }
            if (character == '\n') {
                messageLength++;
                if (messageLength > ProtocolConstants.MAX_MESSAGE_LENGTH) {
                    throw new ProtocolException("Protokollnachricht überschreitet Größenlimit.");
                }
                return line.toString();
            }
            if (character == '\r') {
                throw new ProtocolException("Unmaskierter Zeilenumbruch.");
            }
            line.append((char) character);
            messageLength++;
            if (line.length() > ProtocolConstants.MAX_LINE_LENGTH || messageLength > ProtocolConstants.MAX_MESSAGE_LENGTH) {
                throw new ProtocolException("Protokollnachricht überschreitet Größenlimit.");
            }
        }
    }

    private static <T extends Enum<T>> T parse(Class<T> type, String value) {
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            throw new ProtocolException("Unbekannter Protokollwert: " + value, exception);
        }
    }
}
