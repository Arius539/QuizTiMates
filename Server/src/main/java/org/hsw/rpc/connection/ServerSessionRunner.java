package org.hsw.rpc.connection;

import org.hsw.application.StateExecutor;
import org.hsw.application.session.ServerSession;
import org.hsw.application.session.ServerSessionService;
import org.hsw.domain.home.Home;
import org.hsw.protocol.io.ProtocolException;
import org.hsw.protocol.io.ProtocolReader;
import org.hsw.protocol.io.ProtocolWriter;
import org.hsw.protocol.io.TransportException;
import org.hsw.protocol.message.MethodId;
import org.hsw.protocol.rpc.RpcChannel;
import org.hsw.protocol.rpc.RpcException;
import org.hsw.rpc.handler.HomeServerProxy;
import org.hsw.rpc.handler.LobbyServerProxy;
import org.hsw.rpc.handler.MatchServerProxy;
import org.hsw.rpc.handler.ServerSessionControlHandler;
import org.hsw.rpc.session.ServerTransportSession;
import org.hsw.rpc.session.SessionCloseCoordinator;
import org.hsw.rpc.session.SessionLifecycle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

public final class ServerSessionRunner {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerSessionRunner.class);
    private final ServerSessionService sessionService;
    private final StateExecutor stateExecutor;
    private final SessionLifecycle lifecycle;
    private final SessionCloseCoordinator closeCoordinator;
    private final Socket mainSocket;
    private final ServerSession session;
    private final ServerTransportSession transportSession;

    public ServerSessionRunner(Socket mainSocket) throws SocketException {
        this(mainSocket, new ServerSession());
    }

    ServerSessionRunner(Socket mainSocket, ServerSession session) throws SocketException {
        this.mainSocket = mainSocket;
        this.session = session;
        this.sessionService = new ServerSessionService(Home.getInstance());
        this.stateExecutor = StateExecutor.getInstance();
        this.lifecycle = new SessionLifecycle();
        this.transportSession = new ServerTransportSession(lifecycle);
        this.closeCoordinator = new SessionCloseCoordinator(lifecycle, transportSession, stateExecutor, () -> sessionService.cleanupSessionDomain(session));
        closeCoordinator.completion().thenAccept(result -> {
            if (result.successful()) {
                LOGGER.info("Serversitzung vollständig bereinigt: sessionId={}, outcome={}, cause={}", session.getId(), result.abortCause().isPresent() ? "ABORT" : "CLOSE", result.abortCause().map(cause -> cause.getClass().getSimpleName()).orElse("NONE"));
            }
        });
        try {
            transportSession.attachMainSocket(mainSocket);
        } catch (RuntimeException | Error exception) {
            closeCoordinator.abort(exception);
            throw exception;
        }
    }

    public void start() {
        try {
            Thread connectionThread = Thread.ofPlatform().daemon(true).name("ServerSessionRunner of " + mainSocket.getRemoteSocketAddress()).unstarted(this::run);
            transportSession.registerAndStartConnectionThread(connectionThread);
        } catch (RuntimeException | Error failure) {
            closeCoordinator.abort(failure);
            throw failure;
        }
    }

    public void abort(Throwable cause) {
        closeCoordinator.abort(cause);
    }

    private void run() {
        try {
            RpcChannel channel = new RpcChannel(new ProtocolReader(mainSocket.getInputStream()), new ProtocolWriter(mainSocket.getOutputStream()), "side=SERVER channel=MAIN sessionId=" + session.getId());
            ServerRpcExchange exchange = new ServerRpcExchange(channel, stateExecutor, lifecycle);
            HomeServerProxy homeProxy = new HomeServerProxy(exchange, session, transportSession, sessionService, lifecycle, closeCoordinator, mainSocket.getInetAddress());
            LobbyServerProxy lobbyProxy = new LobbyServerProxy(exchange, session, sessionService);
            MatchServerProxy matchProxy = new MatchServerProxy(exchange, session, sessionService);
            ServerSessionControlHandler controlHandler = new ServerSessionControlHandler(exchange, closeCoordinator);
            LOGGER.info("Server-Hauptkanal bereit: sessionId={}, peer={}", session.getId(), mainSocket.getRemoteSocketAddress());

            while (exchange.receiveNext()) {
                MethodId method = exchange.currentMethod();
                if (homeProxy.supports(method)) homeProxy.run();
                else if (lobbyProxy.supports(method)) lobbyProxy.run();
                else if (matchProxy.supports(method)) matchProxy.run();
                else if (controlHandler.supports(method)) controlHandler.run();
                else exchange.processNext();
            }
        } catch (IOException | ProtocolException | TransportException | RpcException exception) {
            if (lifecycle.isOpen()) {
                LOGGER.warn("Server-Hauptkanal unerwartet beendet: sessionId={}, cause={}, ioCause={}", session.getId(), exception.getClass().getSimpleName(), exception.getCause() == null ? "NONE" : exception.getCause().getClass().getSimpleName());
            }
            closeCoordinator.abort(exception);
        } catch (RuntimeException | Error exception) {
            closeCoordinator.abort(exception);
            LOGGER.error("Verbindung unerwartet beendet: sessionId={}", session.getId(), exception);
        } finally {
            closeCoordinator.close();
        }
    }

}
