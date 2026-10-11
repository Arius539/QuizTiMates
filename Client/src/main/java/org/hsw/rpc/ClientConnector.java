package org.hsw.rpc;

import org.hsw.application.session.ClientSession;
import org.hsw.config.ConnectionConfig;
import org.hsw.contract.*;
import org.hsw.protocol.io.*;
import org.hsw.protocol.rpc.RpcChannel;
import org.hsw.rpc.communication.*;
import org.hsw.rpc.proxy.HomeClientProxy;
import org.hsw.rpc.session.*;
import org.slf4j.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.*;
import java.util.*;

@Component
public class ClientConnector {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientConnector.class);
    private final ISessionClosedNotifier sessionClosedNotifier;
    private final IPlayerCallback callbackTarget;

    public ClientConnector(ISessionClosedNotifier sessionClosedNotifier, IPlayerCallback callbackTarget) {
        this.sessionClosedNotifier = Objects.requireNonNull(sessionClosedNotifier);
        this.callbackTarget = Objects.requireNonNull(callbackTarget);
    }

    public IHome connect(UUID sessionId, String serverAddress, int serverPort) {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(serverAddress, "serverAddress");
        ClientSession session = new ClientSession(sessionId);
        ClientSessionLifecycle lifecycle = new ClientSessionLifecycle();
        ClientTransportSession transportSession = new ClientTransportSession(lifecycle);
        ClientSessionCloseCoordinator closeCoordinator = new ClientSessionCloseCoordinator(lifecycle, transportSession, session, sessionClosedNotifier);
        try {
            Socket socket = new Socket();
            socket.setSoTimeout(ConnectionConfig.SOCKET_TIMEOUT_TIME);
            transportSession.attachMainSocket(socket);
            LOGGER.info("Hauptverbindung aufbauen: sessionId={}, host={}, port={}", sessionId, serverAddress, serverPort);
            socket.connect(new InetSocketAddress(serverAddress, serverPort), 5000);
            RpcChannel channel = new RpcChannel(new ProtocolReader(socket.getInputStream()), new ProtocolWriter(socket.getOutputStream()), "side=CLIENT channel=MAIN sessionId=" + sessionId);
            ClientConnection connection = new ClientConnection(socket, channel, closeCoordinator::close, closeCoordinator::abort, lifecycle);
            transportSession.attachMainConnection(connection);
            ClientRegistrationService registration = new ClientRegistrationService(session, lifecycle, transportSession, closeCoordinator, callbackTarget);
            lifecycle.requireOpen();
            LOGGER.info("Hauptkanal bereit: sessionId={}, peer={}", sessionId, socket.getRemoteSocketAddress());
            return new HomeClientProxy(connection, session, lifecycle, registration, closeCoordinator);
        } catch (IOException exception) {
            TransportException failure = new TransportException("Hauptverbindung konnte nicht initialisiert werden.", exception);
            closeCoordinator.abort(failure);
            throw failure;
        } catch (RuntimeException | Error failure) {
            if (lifecycle.isOpen()) closeCoordinator.abort(failure);
            throw failure;
        }
    }
}
