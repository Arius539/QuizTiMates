package org.hsw.application.session;

import org.hsw.domain.exception.BusinessStateException;
import org.hsw.domain.lobby.LobbyMember;
import org.hsw.domain.match.MatchParticipant;
import org.hsw.domain.player.ServerPlayer;

import java.util.Objects;
import java.util.UUID;

public final class ServerSession {
    private final UUID id;
    private ServerPlayer player;
    private LobbyMember membership;
    private MatchParticipant matchParticipant;

    public ServerSession() {
        this.id = UUID.randomUUID();
        this.player = null;
        this.membership = null;
        this.matchParticipant = null;
    }

    public UUID getId() {
        return id;
    }

    public ServerPlayer getPlayer() {
        return player;
    }

    public LobbyMember getMembership() {
        return membership;
    }

    public MatchParticipant getMatchParticipant() {
        return matchParticipant;
    }

    public ServerPlayer requirePlayer() {
        if (player == null) {
            throw new BusinessStateException("Kein Spieler angemeldet.");
        }
        return player;
    }

    public ServerPlayer requirePlayer(UUID playerId) {
        ServerPlayer currentPlayer = requirePlayer();
        if (!currentPlayer.getId().equals(playerId)) {
            throw new BusinessStateException("Spieler passt nicht zur Sitzung.");
        }
        return currentPlayer;
    }

    public void requireNoPlayer() {
        if (player != null) {
            throw new BusinessStateException("Spieler bereits angemeldet.");
        }
    }

    public LobbyMember requireMembership() {
        if (membership == null) {
            throw new BusinessStateException("Spieler ist nicht in einer Lobby.");
        }
        return membership;
    }

    public LobbyMember requireMembership(UUID lobbyId) {
        if (membership == null || !membership.getLobbyId().equals(lobbyId)) {
            throw new BusinessStateException("Lobbymitgliedschaft ist nicht mehr gültig.");
        }
        return membership;
    }

    public void requireNoLobby() {
        if (membership != null) {
            throw new BusinessStateException("Spieler ist bereits in einer Lobby.");
        }
    }

    public MatchParticipant requireMatchParticipation() {
        if (matchParticipant == null) {
            throw new BusinessStateException("Spieler ist nicht in einem Match");
        }
        return matchParticipant;
    }

    public void bindPlayer(ServerPlayer value) {
        if (player != null) {
            throw new IllegalStateException("Spieler bereits angemeldet.");
        }
        player = Objects.requireNonNull(value, "player");
    }

    public void bindMembership(LobbyMember value) {
        if (player == null || membership != null || !value.getPlayer().getId().equals(player.getId())) {
            throw new IllegalStateException("Lobbymitgliedschaft passt nicht zur Sitzung.");
        }
        membership = value;
    }

    public void bindMatchParticipant(MatchParticipant value) {
        if (player == null || membership == null || matchParticipant != null || !value.getPlayer().getId().equals(player.getId())) {
            throw new IllegalStateException("Matchteilnahme passt nicht zur Sitzung.");
        }
        matchParticipant = value;
    }

    public void clearMatchParticipant() {
        matchParticipant = null;
    }

    public void clearMatchParticipant(UUID matchId) {
        if (matchParticipant != null && matchParticipant.getMatchId().equals(matchId)) {
            clearMatchParticipant();
        }
    }

    public void clearLobbyAndMatchBindings() {
        membership = null;
        matchParticipant = null;
    }

    public void clearDomainBindings() {
        clearLobbyAndMatchBindings();
        player = null;
    }
}
