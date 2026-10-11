package org.hsw.rpc.handler;

import org.hsw.protocol.io.*;
import org.hsw.protocol.message.MethodId;
import org.hsw.rpc.connection.ServerRpcExchange;
import org.hsw.rpc.session.SessionCloseCoordinator;

import java.util.Objects;
import java.util.concurrent.ExecutionException;

public final class ServerSessionControlHandler implements Runnable {
    private final ServerRpcExchange exchange;
    private final SessionCloseCoordinator closeCoordinator;

    public ServerSessionControlHandler(ServerRpcExchange exchange, SessionCloseCoordinator closeCoordinator) {
        this.exchange = Objects.requireNonNull(exchange);
        this.closeCoordinator = Objects.requireNonNull(closeCoordinator);
    }

    public boolean supports(MethodId method) {
        return method == MethodId.DISCONNECT;
    }

    @Override
    public void run() {
        if (exchange.processNextOnConnectionThread(this::dispatch)) {
            closeCoordinator.disconnectAckWritten();
        }
    }

    private void dispatch(MethodId method, ContentReader reader, ContentWriter writer) {
        switch (method) {
            case DISCONNECT -> disconnect(reader);
            default -> throw new IllegalArgumentException("Keine Controlhandler-Methode.");
        }
    }

    private void disconnect(ContentReader reader) {
        reader.requireEnd();
        try {
            closeCoordinator.prepareDisconnect().toCompletableFuture().get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Disconnect unterbrochen.", exception);
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof Error failure) throw failure;
            throw new IllegalStateException("Disconnect fehlgeschlagen.", exception.getCause());
        }
    }
}
