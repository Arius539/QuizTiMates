package org.hsw.protocol.io;

import java.io.IOException;
import java.util.Objects;

public class TransportException extends RuntimeException {
    public TransportException(String message, IOException cause) {
        super(message, Objects.requireNonNull(cause));
    }
}
