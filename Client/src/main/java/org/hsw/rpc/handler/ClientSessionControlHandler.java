package org.hsw.rpc.handler;

import org.hsw.exceptions.RemoteActionException;
import org.hsw.protocol.io.ContentReader;
import org.hsw.protocol.io.ContentWriter;
import org.hsw.protocol.message.ErrorCode;
import org.hsw.protocol.message.MethodId;
import org.hsw.rpc.communication.ClientRpcExchange;
import org.hsw.rpc.session.ClientSessionCloseCoordinator;

import java.util.Objects;

public final class ClientSessionControlHandler implements Runnable {
    private final ClientRpcExchange exchange;
    private final ClientSessionCloseCoordinator closeCoordinator;

    public ClientSessionControlHandler(ClientRpcExchange exchange, ClientSessionCloseCoordinator closeCoordinator) {
        this.exchange = Objects.requireNonNull(exchange);
        this.closeCoordinator = Objects.requireNonNull(closeCoordinator);
    }

    public boolean supports(MethodId method) {
        return method == MethodId.DISCONNECT;
    }

    @Override
    public void run() {
        if (exchange.processNext(this::dispatch)) closeCoordinator.close();
    }

    private void dispatch(MethodId method, ContentReader reader, ContentWriter writer) {
        switch (method) {
            case DISCONNECT -> reader.requireEnd();
            default ->
                    throw new RemoteActionException(ErrorCode.NOT_IMPLEMENTED, "Methode wird von diesem Handler nicht unterstützt.");
        }
    }
}
