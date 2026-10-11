package org.hsw.rpc.communication;

import org.hsw.application.session.ClientSession;
import org.hsw.config.ConnectionConfig;
import org.hsw.contract.IPlayerCallback;
import org.hsw.contract.model.IPlayerInfo;
import org.hsw.exceptions.RemoteActionException;
import org.hsw.protocol.io.TransportException;
import org.hsw.rpc.session.*;
import org.slf4j.*;

import java.io.IOException;
import java.net.*;
import java.util.Objects;
import java.util.function.IntFunction;

import static org.hsw.config.ConnectionConfig.RECEIVING_CLIENT_PORT;

public final class ClientRegistrationService {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientRegistrationService.class);
    private final ClientSession session;
    private final ClientSessionLifecycle lifecycle;
    private final ClientTransportSession transportSession;
    private final ClientSessionCloseCoordinator closeCoordinator;
    private final IPlayerCallback callbackTarget;

    public ClientRegistrationService(ClientSession session, ClientSessionLifecycle lifecycle, ClientTransportSession transportSession, ClientSessionCloseCoordinator closeCoordinator, IPlayerCallback callbackTarget) {
        this.session = Objects.requireNonNull(session);
        this.lifecycle = Objects.requireNonNull(lifecycle);
        this.transportSession = Objects.requireNonNull(transportSession);
        this.closeCoordinator = Objects.requireNonNull(closeCoordinator);
        this.callbackTarget = Objects.requireNonNull(callbackTarget);
    }

    public IPlayerInfo registerPlayer(IntFunction<IPlayerInfo> createPlayerRequest) {
        Objects.requireNonNull(createPlayerRequest);
        lifecycle.requireOpen();
        ServerSocket listener = null;
        Throwable failure = null;
        try {
            listener = new ServerSocket(RECEIVING_CLIENT_PORT);
            transportSession.attachRegistrationListener(listener);
            listener.setSoTimeout(ConnectionConfig.SERVERSOCKET_CALLBACK_CONNECTION_TIMEOUT_TIME);
            LOGGER.info("Callback-Listener bereit: sessionId={}, port={}", session.id(), listener.getLocalPort());
            IPlayerInfo player = createPlayerRequest.apply(listener.getLocalPort());
            Socket callbackSocket = listener.accept();
            transportSession.attachCallbackSocket(callbackSocket);
            callbackSocket.setSoTimeout(ConnectionConfig.SOCKET_TIMEOUT_TIME);
            LOGGER.info("TCP-Rückrufverbindung angenommen: sessionId={}, peer={}", session.id(), callbackSocket.getRemoteSocketAddress());
            ClientSessionRunner runner = new ClientSessionRunner(callbackSocket, callbackTarget, lifecycle, closeCoordinator);
            Thread callbackThread = Thread.ofPlatform().daemon(true).name("Quiz-Client-Callback-" + player.getId()).unstarted(runner);
            transportSession.registerAndStartCallbackThread(callbackThread, () -> session.setPlayer(player));
            LOGGER.info("Clientspieler übernommen und Callbackthread gestartet: sessionId={}, playerId={}", session.id(), player.getId());
            return player;
        } catch (IOException exception) {
            TransportException transportFailure = new TransportException("Registrierung konnte nicht abgeschlossen werden.", exception);
            failure = transportFailure;
            if (lifecycle.isOpen()) closeCoordinator.abort(transportFailure);
            throw transportFailure;
        } catch (RuntimeException | Error exception) {
            failure = exception;
            if (!(exception instanceof RemoteActionException) && lifecycle.isOpen()) closeCoordinator.abort(exception);
            throw exception;
        } finally {
            if (listener != null) {
                try {
                    transportSession.releaseRegistrationListener(listener);
                } catch (RuntimeException | Error closeFailure) {
                    if (lifecycle.isOpen()) closeCoordinator.abort(closeFailure);
                    if (failure == null) throw closeFailure;
                    if (failure != closeFailure) failure.addSuppressed(closeFailure);
                }
            }
        }
    }
}
