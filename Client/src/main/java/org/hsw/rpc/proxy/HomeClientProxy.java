package org.hsw.rpc.proxy;

import org.hsw.application.session.ClientSession;
import org.hsw.contract.IHome;
import org.hsw.contract.ILobby;
import org.hsw.contract.model.IPlayerInfo;
import org.hsw.contract.model.PlayerInfo;
import org.hsw.exceptions.RemoteActionException;
import org.hsw.protocol.io.ContentReader;
import org.hsw.protocol.io.ContentWriter;
import org.hsw.protocol.io.ProtocolException;
import org.hsw.protocol.message.MethodId;
import org.hsw.protocol.rpc.RpcRequest;
import org.hsw.protocol.rpc.RpcResponse;
import org.hsw.rpc.communication.ClientConnection;
import org.hsw.rpc.communication.ClientRegistrationService;
import org.hsw.rpc.session.ClientSessionCloseCoordinator;
import org.hsw.rpc.session.ClientSessionLifecycle;
import org.hsw.transfer.PlayerView;
import org.hsw.transfer.codec.PlayerViewCodec;

import java.util.Objects;
import java.util.UUID;

public final class HomeClientProxy implements IHome, AutoCloseable {
    private final ClientSession session;
    private final ClientSessionLifecycle lifecycle;
    private final ClientRegistrationService registration;
    private final ClientSessionCloseCoordinator closeCoordinator;
    private final ClientConnection connection;

    public HomeClientProxy(ClientConnection connection, ClientSession session, ClientSessionLifecycle lifecycle, ClientRegistrationService registration, ClientSessionCloseCoordinator closeCoordinator) {
        this.connection = Objects.requireNonNull(connection);
        this.session = Objects.requireNonNull(session);
        this.lifecycle = Objects.requireNonNull(lifecycle);
        this.registration = Objects.requireNonNull(registration);
        this.closeCoordinator = Objects.requireNonNull(closeCoordinator);
    }

    @Override
    public synchronized IPlayerInfo createPlayer(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Spielername darf nicht leer sein.");
        }
        if (session.player() != null) {
            throw new IllegalStateException("Spielerregistrierung läuft bereits oder ist abgeschlossen.");
        }
        lifecycle.requireOpen();
        String trimmedName = name.trim();
        return registration.registerPlayer(port -> requestPlayer(trimmedName, port));
    }

    @Override
    public ILobby createLobby(IPlayerInfo host) {
        session.requirePlayer(host);
        if (session.lobby() != null) {
            throw new IllegalStateException("Spieler ist nicht im Hauptmenü.");
        }
        ContentWriter writer = new ContentWriter();
        writer.writeUUID(host.getId());
        try {
            RpcResponse response = connection.request(new RpcRequest(MethodId.CREATE_LOBBY, writer.getFields()));
            ContentReader reader = new ContentReader(response.content());
            UUID lobbyId = reader.readUUID();
            reader.requireEnd();
            lifecycle.publishWhileOpen(() -> session.setLobby(lobbyId));
            return new LobbyClientProxy(session, connection, lobbyId);
        } catch (RemoteActionException rejection) {
            throw rejection;
        } catch (RuntimeException | Error failure) {
            connection.abort(failure);
            throw failure;
        }
    }

    @Override
    public boolean lobbyExists(String lobbyCode) {
        if (lobbyCode == null || lobbyCode.isBlank()) {
            return false;
        }
        if (session.player() == null) {
            throw new IllegalStateException("Kein Spieler angemeldet.");
        }
        if (session.lobby() != null) {
            throw new IllegalStateException("Spieler ist nicht im Hauptmenü.");
        }
        ContentWriter writer = new ContentWriter();
        writer.writeString(lobbyCode);
        try {
            RpcResponse response = connection.request(new RpcRequest(MethodId.LOBBY_EXISTS, writer.getFields()));
            ContentReader reader = new ContentReader(response.content());
            boolean exists = reader.readBoolean();
            reader.requireEnd();
            return exists;
        } catch (ProtocolException failure) {
            connection.abort(failure);
            throw failure;
        }
    }

    @Override
    public ILobby joinLobby(String code, IPlayerInfo player) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Lobbycode darf nicht leer sein.");
        }
        session.requirePlayer(player);
        if (session.lobby() != null) {
            throw new IllegalStateException("Spieler ist nicht im Hauptmenü.");
        }
        ContentWriter writer = new ContentWriter();
        writer.writeString(code);
        writer.writeUUID(player.getId());
        try {
            RpcResponse response = connection.request(new RpcRequest(MethodId.JOIN_LOBBY, writer.getFields()));
            ContentReader reader = new ContentReader(response.content());
            UUID lobbyId = reader.readUUID();
            reader.requireEnd();
            lifecycle.publishWhileOpen(() -> session.setLobby(lobbyId));
            return new LobbyClientProxy(session, connection, lobbyId);
        } catch (RemoteActionException rejection) {
            throw rejection;
        } catch (RuntimeException | Error failure) {
            connection.abort(failure);
            throw failure;
        }
    }

    @Override
    public void close() {
        closeCoordinator.close();
    }

    private IPlayerInfo requestPlayer(String name, int callbackPort) {
        ContentWriter writer = new ContentWriter();
        writer.writeString(name);
        writer.writeInt(callbackPort);
        RpcResponse response = connection.request(new RpcRequest(MethodId.CREATE_PLAYER, writer.getFields()));
        if (!response.isSuccess()) {
            throw new ProtocolException("Registrierung erwartet eine erfolgreiche Antwort.");
        }
        ContentReader reader = new ContentReader(response.content());
        PlayerView confirmedPlayer = PlayerViewCodec.read(reader);
        reader.requireEnd();
        IPlayerInfo player = new PlayerInfo(confirmedPlayer.id(), confirmedPlayer.name());
        if (!name.equals(player.getName())) {
            throw new ProtocolException("Registrierungsantwort enthält einen abweichenden Spielernamen.");
        }
        return player;
    }
}
