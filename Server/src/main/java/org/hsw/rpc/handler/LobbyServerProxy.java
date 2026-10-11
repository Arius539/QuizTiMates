package org.hsw.rpc.handler;

import org.hsw.application.session.ServerSession;
import org.hsw.application.session.ServerSessionService;
import org.hsw.contract.ILobby;
import org.hsw.contract.model.IPlayerInfo;
import org.hsw.contract.model.Topic;
import org.hsw.protocol.io.ContentFormatException;
import org.hsw.protocol.io.ContentReader;
import org.hsw.protocol.io.ContentWriter;
import org.hsw.protocol.message.MethodId;
import org.hsw.rpc.connection.ServerRpcExchange;
import org.hsw.transfer.PlayerView;
import org.hsw.transfer.codec.PlayerViewCodec;

import java.util.List;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

public final class LobbyServerProxy implements Runnable {
    private static final Set<MethodId> METHODS = Set.of(MethodId.GET_LOBBY_CODE, MethodId.GET_LOBBY_HOST, MethodId.GET_LOBBY_PLAYERS, MethodId.GET_LOBBY_MATCH_RUNNING, MethodId.LEAVE_LOBBY, MethodId.START_GAME);
    private final ServerRpcExchange exchange;
    private final ServerSessionService sessionService;
    private final ServerSession session;

    public LobbyServerProxy(ServerRpcExchange exchange, ServerSession session, ServerSessionService sessionService) {
        this.exchange = exchange;
        this.session = session;
        this.sessionService = sessionService;
    }

    public boolean supports(MethodId method) {
        return METHODS.contains(method);
    }

    @Override
    public void run() {
        exchange.processNext(this::dispatch);
    }

    private void dispatch(MethodId method, ContentReader reader, ContentWriter writer) {
        session.requireMembership();
        switch (method) {
            case GET_LOBBY_CODE -> getCode(reader, writer);
            case GET_LOBBY_HOST -> getHost(reader, writer);
            case GET_LOBBY_PLAYERS -> getPlayers(reader, writer);
            case GET_LOBBY_MATCH_RUNNING -> isMatchRunning(reader, writer);
            case START_GAME -> startMatch(reader);
            case LEAVE_LOBBY -> leave(reader);
            default -> throw new IllegalArgumentException("Keine Lobby-Methode.");
        }
    }

    private void getCode(ContentReader reader, ContentWriter writer) {
        writer.writeString(readLobby(reader).getCode());
    }

    private void getHost(ContentReader reader, ContentWriter writer) {
        writePlayer(writer, readLobby(reader).getHost());
    }

    private void getPlayers(ContentReader reader, ContentWriter writer) {
        List<IPlayerInfo> players = readLobby(reader).getPlayers();
        writer.writeInt(players.size());
        for (IPlayerInfo player : players) writePlayer(writer, player);
    }

    private void isMatchRunning(ContentReader reader, ContentWriter writer) {
        writer.writeBoolean(readLobby(reader).isMatchRunning());
    }

    private void startMatch(ContentReader reader) {
        UUID lobbyId = reader.readUUID();
        UUID playerId = reader.readUUID();
        int roundCount = reader.readInt();
        int topicCount = reader.readInt();
        if (topicCount < 0 || topicCount > Topic.values().length) {
            throw new ContentFormatException("Ungültige Themenanzahl.");
        }
        Set<Topic> topics = EnumSet.noneOf(Topic.class);
        for (int i = 0; i < topicCount; i++) {
            if (!topics.add(reader.readEnum(Topic.class))) {
                throw new ContentFormatException("Thema mehrfach angegeben.");
            }
        }
        reader.requireEnd();
        ILobby lobby = sessionService.requireLobby(session, lobbyId);
        lobby.startMatch(session.requirePlayer(playerId), roundCount, topics);
    }

    private void leave(ContentReader reader) {
        UUID lobbyId = reader.readUUID();
        reader.requireEnd();
        sessionService.requireLobby(session, lobbyId);
        sessionService.leaveLobby(session);
    }

    private ILobby readLobby(ContentReader reader) {
        UUID lobbyId = reader.readUUID();
        reader.requireEnd();
        return sessionService.requireLobby(session, lobbyId);
    }

    private static void writePlayer(ContentWriter writer, IPlayerInfo player) {
        PlayerViewCodec.write(writer, new PlayerView(player.getId(), player.getName()));
    }
}
