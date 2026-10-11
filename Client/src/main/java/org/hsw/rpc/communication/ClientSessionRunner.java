package org.hsw.rpc.communication;

import org.hsw.contract.IPlayerCallback;
import org.hsw.protocol.io.ProtocolException;
import org.hsw.protocol.io.ProtocolReader;
import org.hsw.protocol.io.ProtocolWriter;
import org.hsw.protocol.io.TransportException;
import org.hsw.protocol.message.MethodId;
import org.hsw.protocol.rpc.RpcChannel;
import org.hsw.protocol.rpc.RpcException;
import org.hsw.rpc.handler.ClientSessionControlHandler;
import org.hsw.rpc.handler.PlayerServerProxy;
import org.hsw.rpc.session.ClientSessionCloseCoordinator;
import org.hsw.rpc.session.ClientSessionLifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.util.Objects;

public final class ClientSessionRunner implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionRunner.class);
    private final Socket socket;
    private final ClientSessionLifecycle lifecycle;
    private final ClientSessionCloseCoordinator closeCoordinator;
    private final IPlayerCallback callbackTarget;

    public ClientSessionRunner(Socket socket, IPlayerCallback callbackTarget, ClientSessionLifecycle lifecycle, ClientSessionCloseCoordinator closeCoordinator) {
        this.socket = Objects.requireNonNull(socket);
        this.lifecycle = Objects.requireNonNull(lifecycle);
        this.closeCoordinator = Objects.requireNonNull(closeCoordinator);
        this.callbackTarget = Objects.requireNonNull(callbackTarget);
    }

    @Override
    public void run() {
        try {
            RpcChannel channel = new RpcChannel(new ProtocolReader(socket.getInputStream()), new ProtocolWriter(socket.getOutputStream()), "side=CLIENT channel=CALLBACK peer=" + socket.getRemoteSocketAddress());
            ClientRpcExchange exchange = new ClientRpcExchange(channel, lifecycle);
            PlayerServerProxy callbackHandler = new PlayerServerProxy(callbackTarget, exchange);
            ClientSessionControlHandler controlHandler = new ClientSessionControlHandler(exchange, closeCoordinator);
            while (exchange.receiveNext()) {
                MethodId method = exchange.currentMethod();
                if (callbackHandler.supports(method)) callbackHandler.run();
                else if (controlHandler.supports(method)) controlHandler.run();
                else exchange.processNext();
            }
        } catch (IOException exception) {
            TransportException failure = new TransportException("Callbackverbindung konnte nicht initialisiert werden.", exception);
            if (lifecycle.isOpen()) {
                closeCoordinator.abort(failure);
                LOGGER.warn("Client-Rückrufkanal konnte nicht initialisiert werden: peer={}, cause={}", socket.getRemoteSocketAddress(), exception.getClass().getSimpleName());
            }
        } catch (ProtocolException | TransportException | RpcException exception) {
            if (lifecycle.isOpen()) {
                closeCoordinator.abort(exception);
                LOGGER.warn("Client-Rückrufkanal unerwartet beendet: peer={}, cause={}, ioCause={}", socket.getRemoteSocketAddress(), exception.getClass().getSimpleName(), exception.getCause() == null ? "NONE" : exception.getCause().getClass().getSimpleName());
            }
        } catch (RuntimeException | Error exception) {
            if (lifecycle.isOpen()) {
                closeCoordinator.abort(exception);
                LOGGER.error("Client-Rückrufverarbeitung fehlgeschlagen.", exception);
            }
        } finally {
            closeCoordinator.close();
        }
    }
}
