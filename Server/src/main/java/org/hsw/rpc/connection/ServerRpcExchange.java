package org.hsw.rpc.connection;

import org.hsw.application.StateExecutor;
import org.hsw.domain.exception.*;
import org.hsw.exceptions.RemoteActionException;
import org.hsw.protocol.io.*;
import org.hsw.protocol.message.*;
import org.hsw.protocol.rpc.*;
import org.hsw.rpc.session.SessionLifecycle;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Function;

public final class ServerRpcExchange {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ServerRpcExchange.class.getName());

    private final RpcChannel channel;
    private final StateExecutor stateExecutor;
    private final SessionLifecycle lifecycle;
    private boolean finished;
    private ReceivedRpcRequest currentRequest;

    public ServerRpcExchange(RpcChannel channel, StateExecutor stateExecutor, SessionLifecycle lifecycle) {
        this.channel = channel;
        this.stateExecutor = stateExecutor;
        this.lifecycle = lifecycle;
        this.finished = false;
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

    public void processNext() {
        processNext((method, reader, writer) -> {
            throw new RemoteActionException(ErrorCode.NOT_IMPLEMENTED, "Methode noch nicht unterstützt.");
        });
    }

    public void processNext(RequestHandler handler) {
        processCurrent(request -> stateExecutor.execute(() -> {
            lifecycle.requireOpen();
            return handle(request, handler);
        }));
    }

    public boolean processNextOnConnectionThread(RequestHandler handler) {
        return processCurrent(request -> {
            lifecycle.requireOpen();
            return handle(request, handler);
        });
    }

    private static RpcResponse failure(ErrorCode code, String message) {
        return new RpcResponse(MessageStatus.ERROR, code, message, List.of());
    }

    private boolean processCurrent(Function<RpcRequest, RpcResponse> responseFactory) {
        ReceivedRpcRequest received = requireCurrentRequest();
        try {
            RpcResponse response = responseFactory.apply(received.request());
            if (response.isError()) {
                if (response.errorCode() == ErrorCode.INTERNAL_ERROR) {
                    LOGGER.error("Interner RPC-Fehler: method={}, callId={}, errorCode={}", received.request().method(), received.callId(), response.errorCode());
                } else {
                    LOGGER.info("RPC fachlich abgelehnt: method={}, callId={}, errorCode={}",
                            received.request().method(),
                            received.callId(),
                            response.errorCode());
                }
            }
            channel.respond(received, response);
            return response.isSuccess();
        } finally {
            currentRequest = null;
        }
    }

    /**
     * Beantwortet über den übergebenen RequestHandler die RpcRequest mit einer RpcResponse
     *
     * @param request einegangene Anfrage
     * @param handler auszuführende Methode aus einem der Proxies
     * @return RpcResponse
     */
    private RpcResponse handle(RpcRequest request, RequestHandler handler) {
        try {
            ContentReader reader = new ContentReader(request.content());
            ContentWriter writer = new ContentWriter();
            handler.handle(request.method(), reader, writer);
            return new RpcResponse(MessageStatus.OK, ErrorCode.NONE, "", writer.getFields());
        } catch (RemoteActionException exception) {
            return failure(exception.code(), exception.getMessage());
        } catch (BusinessOperationException | ContentFormatException exception) {
            return failure(ErrorCode.INVALID_ARGUMENT, exception.getMessage());
        } catch (BusinessStateException exception) {
            return failure(ErrorCode.INVALID_STATE, exception.getMessage());
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
