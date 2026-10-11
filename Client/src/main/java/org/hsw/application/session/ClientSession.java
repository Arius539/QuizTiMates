package org.hsw.application.session;

import org.hsw.contract.model.IPlayerInfo;

import java.util.Objects;
import java.util.UUID;

public final class ClientSession {
    private final UUID id;
    private IPlayerInfo player;
    private UUID lobby;

    public ClientSession() {
        this(UUID.randomUUID());
    }

    public ClientSession(UUID id) {
        this.id = Objects.requireNonNull(id);
    }

    public UUID id() {
        return id;
    }

    public synchronized IPlayerInfo player() {
        return player;
    }

    public synchronized UUID lobby() {
        return lobby;
    }

    public synchronized void requirePlayer(IPlayerInfo value) {
        if (player == null) {
            throw new IllegalStateException("Kein Spieler angemeldet.");
        }
        if (value == null || !player.getId().equals(value.getId())) {
            throw new IllegalArgumentException("Spieler passt nicht zur Sitzung.");
        }
    }

    public synchronized void setPlayer(IPlayerInfo value) {
        if (player != null) {
            throw new IllegalStateException("Spieler bereits angemeldet.");
        }
        player = Objects.requireNonNull(value);
    }

    public synchronized void setLobby(UUID value) {
        lobby = Objects.requireNonNull(value);
    }

    public synchronized void clearLobby() {
        lobby = null;
    }

    public synchronized void reset() {
        lobby = null;
        player = null;
    }
}
