package org.hsw.exceptions;

import org.hsw.protocol.message.ErrorCode;

public class RemoteActionException extends RuntimeException {
    private final ErrorCode code;

    public RemoteActionException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
