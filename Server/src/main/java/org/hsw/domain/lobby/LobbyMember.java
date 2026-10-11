package org.hsw.domain.lobby;

import org.hsw.domain.player.ServerPlayer;

import java.util.Objects;
import java.util.UUID;

public final class LobbyMember {
    private final UUID lobbyId;
    private final ServerPlayer player;
    private boolean ready;

    public LobbyMember(UUID lobbyId, ServerPlayer player) {
        this.lobbyId = Objects.requireNonNull(lobbyId);
        this.player = Objects.requireNonNull(player);
        this.ready = false;
    }

    public UUID getLobbyId() {
        return lobbyId;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }
}
