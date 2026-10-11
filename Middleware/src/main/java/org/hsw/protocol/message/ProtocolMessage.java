package org.hsw.protocol.message;

import java.util.List;

public record ProtocolMessage(ProtocolVersion version, MessageType type, long callId, MethodId method,
                              MessageStatus status, ErrorCode errorCode, String errorMessage, List<String> content) {
}