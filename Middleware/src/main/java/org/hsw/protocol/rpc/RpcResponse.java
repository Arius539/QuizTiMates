package org.hsw.protocol.rpc;

import org.hsw.protocol.message.ErrorCode;
import org.hsw.protocol.message.MessageStatus;

import java.util.List;

public record RpcResponse(MessageStatus status, ErrorCode errorCode, String errorMessage, List<String> content) {

    public RpcResponse {
        if (status == null) {
            throw new IllegalArgumentException("Status darf nicht null sein.");
        }

        errorCode = errorCode == null ? ErrorCode.NONE : errorCode;

        errorMessage = errorMessage == null ? "" : errorMessage;

        content = content == null ? List.of() : List.copyOf(content);
    }

    public boolean isSuccess() {
        return status == MessageStatus.OK;
    }

    public boolean isError() {
        return status == MessageStatus.ERROR;
    }
}