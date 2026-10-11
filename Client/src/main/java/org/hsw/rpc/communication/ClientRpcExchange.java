package org.hsw.rpc.communication;

import org.hsw.exceptions.RemoteActionException;
import org.hsw.protocol.io.*;
import org.hsw.protocol.message.*;
import org.hsw.protocol.rpc.*;
import org.hsw.rpc.session.ClientSessionLifecycle;

import java.util.*;

public final class ClientRpcExchange {
    private final RpcChannel channel;
    private final ClientSessionLifecycle lifecycle;
    private boolean finished;
    private ReceivedRpcRequest currentRequest;

    public ClientRpcExchange(RpcChannel channel, ClientSessionLifecycle lifecycle) {
        this.channel = Objects.requireNonNull(channel);
        this.lifecycle = Objects.requireNonNull(lifecycle);
    }

    public boolean receiveNext() {
        if (currentRequest != null) {
            throw new IllegalStateException("Ausstehende Anfrage muss zuerst beantwortet werden.");
        }
        if (finished) return false;
        if (!lifecycle.isOpen()) {
            finished = true;
            return false;
        }
        currentRequest = channel.receiveRequest();
        if (currentRequest == null) finished = true;
        return currentRequest != null;
    }

    public boolean processNext() {
        return processNext((method, reader, writer) -> {
            throw new RemoteActionException(ErrorCode.NOT_IMPLEMENTED, "Methode noch nicht unterstützt.");
        });
    }

    public boolean processNext(RequestHandler handler) {
        ReceivedRpcRequest received = requireCurrentRequest();
        try {
            lifecycle.requireOpen();
            RpcResponse response = handle(received.request(), handler);
            channel.respond(received, response);
            return response.isSuccess();
        } finally {
            currentRequest = null;
        }
    }

    private static RpcResponse failure(ErrorCode code, String message) {
        return new RpcResponse(MessageStatus.ERROR, code, message, List.of());
    }

    private RpcResponse handle(RpcRequest request, RequestHandler handler) {
        try {
            ContentReader reader = new ContentReader(request.content());
            ContentWriter writer = new ContentWriter();
            handler.handle(request.method(), reader, writer);
            return new RpcResponse(MessageStatus.OK, ErrorCode.NONE, "", writer.getFields());
        } catch (RemoteActionException exception) {
            return failure(exception.code(), exception.getMessage());
        } catch (ContentFormatException exception) {
            return failure(ErrorCode.INVALID_ARGUMENT, exception.getMessage());
        }
    }

    public MethodId currentMethod() {
        return requireCurrentRequest().request().method();
    }

    private ReceivedRpcRequest requireCurrentRequest() {
        if (currentRequest == null) {
            throw new IllegalStateException("Keine eingelesene Anfrage vorhanden.");
        }
        return currentRequest;
    }

    @FunctionalInterface
    public interface RequestHandler {
        void handle(MethodId method, ContentReader reader, ContentWriter writer);
    }
}
