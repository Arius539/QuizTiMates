package org.hsw.rpc.proxy;

import org.hsw.application.session.ClientSession;
import org.hsw.contract.*;
import org.hsw.contract.model.*;
import org.hsw.exceptions.RemoteActionException;
import org.hsw.protocol.io.*;
import org.hsw.protocol.message.*;
import org.hsw.protocol.rpc.*;
import org.hsw.rpc.communication.ClientConnection;
import org.hsw.transfer.PlayerView;
import org.hsw.transfer.codec.PlayerViewCodec;

import java.util.*;

public final class LobbyClientProxy implements ILobby {
    private final ClientSession session;
    private final ClientConnection connection;
    private final UUID lobbyId;

    LobbyClientProxy(ClientSession session, ClientConnection connection, UUID lobbyId) {
        this.session = Objects.requireNonNull(session);
        this.connection = Objects.requireNonNull(connection);
        this.lobbyId = Objects.requireNonNull(lobbyId);
    }

    @Override
    public UUID getId() {
        return lobbyId;
    }

    @Override
    public String getCode() {
        if (!isCurrentLobby()) {
            throw new IllegalStateException("Lobbyproxy ist nicht mehr gültig.");
        }
        try {
            RpcResponse response = connection.request(new RpcRequest(MethodId.GET_LOBBY_CODE, lobbyIdFields().getFields()));
            ContentReader reader = new ContentReader(response.content());
            String code = reader.readString();
            reader.requireEnd();
            return code;
        } catch (ProtocolException exception) {
            connection.abort(exception);
            throw exception;
        }
    }

    @Override
    public IPlayerInfo getHost() {
        if (!isCurrentLobby()) {
            throw new IllegalStateException("Lobbyproxy ist nicht mehr gültig.");
        }
        try {
            RpcResponse response = connection.request(new RpcRequest(MethodId.GET_LOBBY_HOST, lobbyIdFields().getFields()));
            ContentReader reader = new ContentReader(response.content());
            final PlayerView hostView = PlayerViewCodec.read(reader);
            reader.requireEnd();
            return new PlayerInfo(hostView.id(), hostView.name());
        } catch (ProtocolException exception) {
            connection.abort(exception);
            throw exception;
        }
    }

    @Override
    public List<IPlayerInfo> getPlayers() {
        if (!isCurrentLobby()) {
            throw new IllegalStateException("Lobbyproxy ist nicht mehr gültig.");
        }
        try {
            RpcResponse response = connection.request(new RpcRequest(MethodId.GET_LOBBY_PLAYERS, lobbyIdFields().getFields()));
            ContentReader reader = new ContentReader(response.content());
            int count = reader.readInt();
            if (count < 1 || count > (ProtocolConstants.MAX_CONTENT_FIELDS - 1) / 2) {
                throw new ProtocolException("Ungültige Spieleranzahl.");
            }
            List<IPlayerInfo> players = new ArrayList<>(count);
            Set<UUID> ids = new HashSet<>();
            for (int i = 0; i < count; i++) {
                PlayerView playerView = PlayerViewCodec.read(reader);
                if (!ids.add(playerView.id())) {
                    throw new ProtocolException("Spieler mehrfach in Lobbyantwort.");
                }
                players.add(new PlayerInfo(playerView.id(), playerView.name()));
            }
            if (!ids.contains(session.player().getId())) {
                throw new ProtocolException("Spieler fehlt in Lobbyantwort.");
            }
            reader.requireEnd();
            return List.copyOf(players);
        } catch (ProtocolException exception) {
            connection.abort(exception);
            throw exception;
        }
    }

    @Override
    public boolean isMatchRunning() {
        if (!isCurrentLobby()) {
            throw new IllegalStateException("Lobbyproxy ist nicht mehr gültig.");
        }
        try {
            RpcResponse response = connection.request(new RpcRequest(MethodId.GET_LOBBY_MATCH_RUNNING, lobbyIdFields().getFields()));
            ContentReader reader = new ContentReader(response.content());
            boolean running = reader.readBoolean();
            reader.requireEnd();
            return running;
        } catch (ProtocolException exception) {
            connection.abort(exception);
            throw exception;
        }
    }

    @Override
    public Optional<IMatch> getRunningMatch() {
        if (!isCurrentLobby()) {
            throw new IllegalStateException("Lobbyproxy ist nicht mehr gültig.");
        }
        try {
            RpcResponse response = connection.request(new RpcRequest(MethodId.GET_MATCH_ID, List.of()));
            ContentReader reader = new ContentReader(response.content());
            UUID matchId = reader.readUUID();
            reader.requireEnd();
            return Optional.of(new MatchClientProxy(connection, matchId));
        } catch (RemoteActionException rejection) {
            if (rejection.code() == ErrorCode.INVALID_STATE) {
                return Optional.empty();
            }
            throw rejection;
        } catch (ProtocolException exception) {
            connection.abort(exception);
            throw exception;
        }
    }

    @Override
    public void startMatch(IPlayerInfo player, int roundCount, Set<Topic> topics) {
        session.requirePlayer(player);
        if (!isCurrentLobby()) {
            throw new IllegalStateException("Lobbyproxy ist nicht mehr gültig.");
        }
        ContentWriter writer = lobbyIdFields();
        writer.writeUUID(player.getId());
        writer.writeInt(roundCount);
        Set<Topic> orderedTopics = EnumSet.noneOf(Topic.class);
        orderedTopics.addAll(topics);
        writer.writeInt(orderedTopics.size());
        for (Topic topic : orderedTopics) writer.writeEnum(topic);
        try {
            RpcResponse response = connection.request(new RpcRequest(MethodId.START_GAME, writer.getFields()));
            new ContentReader(response.content()).requireEnd();
        } catch (ProtocolException exception) {
            connection.abort(exception);
            throw exception;
        }
    }

    @Override
    public void leave(IPlayerInfo player) {
        session.requirePlayer(player);
        if (!isCurrentLobby()) {
            throw new IllegalStateException("Lobbyproxy ist nicht mehr gültig.");
        }
        ContentWriter writer = lobbyIdFields();
        try {
            RpcResponse response = connection.request(new RpcRequest(MethodId.LEAVE_LOBBY, writer.getFields()));
            ContentReader reader = new ContentReader(response.content());
            reader.requireEnd();
            session.clearLobby();
        } catch (ProtocolException exception) {
            connection.abort(exception);
            throw exception;
        }
    }

    private ContentWriter lobbyIdFields() {
        ContentWriter writer = new ContentWriter();
        writer.writeUUID(lobbyId);
        return writer;
    }

    private boolean isCurrentLobby() {
        return lobbyId.equals(session.lobby());
    }
}
