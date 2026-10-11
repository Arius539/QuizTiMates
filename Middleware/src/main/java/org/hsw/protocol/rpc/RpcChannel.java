package org.hsw.protocol.rpc;

import org.hsw.protocol.io.*;
import org.hsw.protocol.message.*;
import org.slf4j.*;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class RpcChannel {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcChannel.class);
    private final ProtocolReader reader;
    private final ProtocolWriter writer;
    private final String logContext;
    private long nextCallId;

    public RpcChannel(ProtocolReader reader, ProtocolWriter writer) {
        this(reader, writer, "channel=UNSPECIFIED");
    }

    public RpcChannel(ProtocolReader reader, ProtocolWriter writer, String logContext) {
        this.nextCallId = 1;
        this.reader = Objects.requireNonNull(reader);
        this.writer = Objects.requireNonNull(writer);
        this.logContext = Objects.requireNonNull(logContext);
    }

    public synchronized RpcResponse invoke(RpcRequest request) {
        if (nextCallId == Long.MAX_VALUE) {
            throw new RpcException("Call-ID erschöpft.");
        }

        long callId = nextCallId++;
        long startedAt = System.nanoTime();
        final ProtocolMessage message = new ProtocolMessage(ProtocolVersion.VERSION_1, MessageType.REQUEST, callId, request.method(), MessageStatus.NONE, ErrorCode.NONE, "", request.content());
        LOGGER.info("RPC request senden: {}, method={}, callId={}", logContext, message.method(), message.callId());
        writer.writeMessage(message);

        ProtocolMessage response = reader.readMessage();

        if (response == null) {
            throw new RpcException("Verbindung vor der Antwort geschlossen.");
        }
        if (response.version() != ProtocolVersion.VERSION_1 || response.type() != MessageType.RESPONSE || response.callId() != callId || response.method() != request.method()) {
            throw new RpcException("Antwort gehört nicht zum ausstehenden Aufruf.");
        }
        RpcResponse rpcResponse = new RpcResponse(response.status(), response.errorCode(), response.errorMessage(), response.content());
        validateSuccessFields(rpcResponse);
        LOGGER.info("RPC response empfangen: {}, method={}, callId={}, status={}, errorCode={}, durationMs={}",
                logContext,
                response.method(),
                response.callId(),
                response.status(),
                response.errorCode(),
                TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt));

        return rpcResponse;
    }

    private static void validateSuccessFields(RpcResponse response) {
        if (response.status() == MessageStatus.OK && (response.errorCode() != ErrorCode.NONE || !response.errorMessage().isEmpty())) {
            throw new RpcException("OK-Antwort darf keine Fehlerfelder enthalten.");
        }
    }

    public synchronized void respond(ReceivedRpcRequest receivedRequest, RpcResponse response) {
        if (receivedRequest == null || receivedRequest.request() == null || response == null || response.status() != MessageStatus.OK && response.status() != MessageStatus.ERROR) {
            throw new RpcException("Ungültiger Response-Kontext.");
        }
        validateSuccessFields(response);

        final ProtocolMessage message = new ProtocolMessage(ProtocolVersion.VERSION_1, MessageType.RESPONSE, receivedRequest.callId(), receivedRequest.request().method(), response.status(), response.errorCode(), response.errorMessage(), response.content());
        LOGGER.info("RPC response senden: {}, method={}, callId={}, status={}, errorCode={}",
                logContext,
                message.method(),
                message.callId(),
                message.status(),
                message.errorCode());
        writer.writeMessage(message);
    }

    public ReceivedRpcRequest receiveRequest() {
        ProtocolMessage request = reader.readMessage();
        if (request == null) {
            return null;
        }

        if (request.version() != ProtocolVersion.VERSION_1 || request.type() != MessageType.REQUEST || request.status() != MessageStatus.NONE || request.errorCode() != ErrorCode.NONE || !request.errorMessage().isEmpty()) {
            throw new RpcException("Ungültiger Request-Header.");
        }
        LOGGER.info("RPC request empfangen: {}, method={}, callId={}", logContext, request.method(), request.callId());

        return new ReceivedRpcRequest(request.callId(), new RpcRequest(request.method(), request.content()));
    }
}
