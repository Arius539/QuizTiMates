package org.hsw.protocol.io;

public class ContentFormatException extends ProtocolException {
    public ContentFormatException(String message) {
        super(message);
    }

    public ContentFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}
