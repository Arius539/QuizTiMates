package org.hsw.rpc.handler;

import org.hsw.application.session.*;
import org.hsw.config.ConnectionConfig;
import org.hsw.contract.model.IPlayerInfo;
import org.hsw.domain.exception.BusinessOperationException;
import org.hsw.domain.lobby.Lobby;
import org.hsw.domain.player.ServerPlayer;
import org.hsw.protocol.io.*;
import org.hsw.protocol.message.MethodId;
import org.hsw.protocol.rpc.RpcChannel;
import org.hsw.rpc.connection.*;
import org.hsw.rpc.proxy.PlayerClientProxy;
import org.hsw.rpc.session.*;
import org.hsw.transfer.PlayerView;
import org.hsw.transfer.codec.PlayerViewCodec;
import org.slf4j.*;

import java.io.IOException;
import java.net.*;
import java.util.*;

public final class HomeServerProxy implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(HomeServerProxy.class);
    private static final Set<MethodId> METHODS = Set.of(MethodId.CREATE_PLAYER, MethodId.CREATE_LOBBY, MethodId.JOIN_LOBBY, MethodId.LOBBY_EXISTS);
    private final ServerRpcExchange exchange;
    private final ServerSessionService sessionService;
    private final SessionLifecycle lifecycle;
    private final SessionCloseCoordinator closeCoordinator;
    private final InetAddress peerAddress;
    private final ServerSession session;
    private final ServerTransportSession transportSession;

    public HomeServerProxy(ServerRpcExchange exchange, ServerSession session, ServerTransportSession transportSession, ServerSessionService sessionService, SessionLifecycle lifecycle, SessionCloseCoordinator closeCoordinator, InetAddress peerAddress) {
        this.exchange = exchange;
        this.session = session;
        this.transportSession = transportSession;
        this.sessionService = sessionService;
        this.lifecycle = lifecycle;
        this.closeCoordinator = closeCoordinator;
        this.peerAddress = peerAddress;
    }

    public boolean supports(MethodId method) {
        return METHODS.contains(method);
    }

    @Override
    public void run() {
        exchange.processNext(this::dispatch);
    }

    private void dispatch(MethodId method, ContentReader reader, ContentWriter writer) {
        session.requireNoLobby();
        switch (method) {
            case CREATE_PLAYER -> createPlayer(reader, writer);
            case CREATE_LOBBY -> createLobby(reader, writer);
            case JOIN_LOBBY -> joinLobby(reader, writer);
            case LOBBY_EXISTS -> lobbyExists(reader, writer);
            default -> throw new IllegalArgumentException("Keine Home-Methode.");
        }
    }

    private void createPlayer(ContentReader reader, ContentWriter writer) {
        final String name = reader.readString();
        final int port = reader.readInt();
        reader.requireEnd();
        final IPlayerInfo info = sessionService.createPlayerInfo(session, name);
        if (port < 1 || port > 65535) {
            throw new BusinessOperationException("Ungültiger Rückrufport.");
        }

        try {
            Socket playerSocket = new Socket();
            playerSocket.setSoTimeout(ConnectionConfig.SOCKET_TIMEOUT_TIME);
            transportSession.attachCallbackSocket(playerSocket);
            LOGGER.info("TCP-Rückrufverbindung aufbauen: sessionId={}, peer={}, port={}", session.getId(), peerAddress.getHostAddress(), port);
            playerSocket.connect(new InetSocketAddress(peerAddress, port), 5000);
            LOGGER.info("TCP-Rückrufverbindung aufgebaut: sessionId={}, peer={}", session.getId(), playerSocket.getRemoteSocketAddress());
            ProtocolReader protocolReader = new ProtocolReader(playerSocket.getInputStream());
            ProtocolWriter protocolWriter = new ProtocolWriter(playerSocket.getOutputStream());
            RpcChannel channel = new RpcChannel(protocolReader, protocolWriter, "side=SERVER channel=CALLBACK sessionId=" + session.getId());
            ServerConnection connection = new ServerConnection(playerSocket, channel, () -> {
            });
            ServerCallbackSender sender = new ServerCallbackSender(connection, lifecycle, closeCoordinator::abort);
            transportSession.attachCallbackSender(sender);
            PlayerClientProxy playerProxy = new PlayerClientProxy(sender, matchId -> sessionService.bindMatchParticipation(session, matchId), session::clearMatchParticipant);
            ServerPlayer player = new ServerPlayer(info, playerProxy);
            lifecycle.publishWhileOpen(() -> sessionService.registerPlayer(session, player));
            LOGGER.info("Spieler an Serversitzung gebunden: sessionId={}, playerId={}", session.getId(), info.getId());
            PlayerViewCodec.write(writer, new PlayerView(info.getId(), info.getName()));
        } catch (IOException exception) {
            throw new TransportException("Callbackverbindung konnte nicht aufgebaut werden.", exception);
        }
    }

    private void createLobby(ContentReader reader, ContentWriter writer) {
        UUID playerId = readPlayerId(reader);
        final Lobby lobby = sessionService.createLobby(session, playerId);
        writer.writeUUID(lobby.getId());
    }

    private void lobbyExists(ContentReader reader, ContentWriter writer) {
        String lobbyCode = reader.readString();
        reader.requireEnd();
        writer.writeBoolean(sessionService.lobbyExists(session, lobbyCode));
    }

    private void joinLobby(ContentReader reader, ContentWriter writer) {
        String code = reader.readString();
        UUID playerId = readPlayerId(reader);
        final Lobby lobby = sessionService.joinLobby(session, code, playerId);
        writer.writeUUID(lobby.getId());
    }

    private UUID readPlayerId(ContentReader reader) {
        UUID playerId = reader.readUUID();
        reader.requireEnd();
        return playerId;
    }
}
