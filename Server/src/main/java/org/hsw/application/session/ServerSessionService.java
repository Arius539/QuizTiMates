package org.hsw.application.session;

import org.hsw.contract.model.IPlayerInfo;
import org.hsw.domain.exception.BusinessStateException;
import org.hsw.domain.home.Home;
import org.hsw.domain.lobby.*;
import org.hsw.domain.match.*;
import org.hsw.domain.player.ServerPlayer;

import java.util.*;

public final class ServerSessionService {
    private final Home home;

    public ServerSessionService(Home home) {
        this.home = Objects.requireNonNull(home, "home");
    }

    public IPlayerInfo createPlayerInfo(ServerSession session, String name) {
        IPlayerInfo info = home.createPlayer(name);
        session.requireNoPlayer();
        return info;
    }

    public void registerPlayer(ServerSession session, ServerPlayer player) {
        session.requireNoPlayer();
        home.registerPlayer(player);
        try {
            session.bindPlayer(player);
        } catch (RuntimeException failure) {
            try {
                home.unregisterPlayer(player);
            } catch (RuntimeException cleanupFailure) {
                if (cleanupFailure != failure) failure.addSuppressed(cleanupFailure);
            }
            throw failure;
        }
    }

    public Lobby createLobby(ServerSession session, UUID playerId) {
        session.requireNoLobby();
        ServerPlayer player = session.requirePlayer(playerId);
        Lobby lobby = home.createLobby(player);
        session.bindMembership(lobby.getMembership(player.getId()));
        return lobby;
    }

    public Lobby joinLobby(ServerSession session, String code, UUID playerId) {
        session.requireNoLobby();
        ServerPlayer player = session.requirePlayer(playerId);
        Lobby lobby = home.joinLobby(code, player);
        session.bindMembership(lobby.getMembership(player.getId()));
        return lobby;
    }

    private static RuntimeException cleanupFailure(RuntimeException failures, RuntimeException failure) {
        if (failures == null) {
            return new RuntimeException("Sitzungszustand konnte nicht vollständig bereinigt werden.", failure);
        }
        failures.addSuppressed(failure);
        return failures;
    }

    public boolean lobbyExists(ServerSession session, String lobbyCode) {
        session.requireNoLobby();
        session.requirePlayer();
        return home.lobbyExists(lobbyCode);
    }

    public Lobby requireLobby(ServerSession session, UUID lobbyId) {
        session.requireMembership(lobbyId);
        return home.findLobby(lobbyId).orElseThrow(() -> new BusinessStateException("Lobby nicht mehr vorhanden."));
    }

    public Match requireRunningMatch(ServerSession session) {
        return requireRunningMatch(session, null);
    }

    public Match requireRunningMatch(ServerSession session, UUID expectedMatchId) {
        LobbyMember member = session.requireMembership();
        MatchParticipant participant = session.requireMatchParticipation();
        Match match = home.findLobby(member.getLobbyId()).filter(Lobby::isMatchRunning).flatMap(Lobby::getMatch).orElseThrow(() -> new BusinessStateException("Kein laufendes Match."));
        if (!participant.getMatchId().equals(match.getId())) {
            throw new BusinessStateException("Matchteilnahme ist nicht mehr gültig.");
        }
        if (expectedMatchId != null && !expectedMatchId.equals(match.getId())) {
            throw new BusinessStateException("Matchproxy ist nicht mehr gültig.");
        }
        return match;
    }

    public void leaveMatch(ServerSession session, UUID requestedMatchId, UUID playerId) {
        ServerPlayer player = session.requirePlayer(playerId);
        MatchParticipant participant = session.getMatchParticipant();
        if (participant == null) return;
        if (!participant.getMatchId().equals(requestedMatchId)) {
            throw new BusinessStateException("Matchproxy ist nicht mehr gültig.");
        }
        Lobby lobby = requireLobby(session, session.requireMembership().getLobbyId());
        Match match = lobby.getMatch().filter(currentMatch -> currentMatch.getId().equals(participant.getMatchId())).orElseThrow(() -> new BusinessStateException("Matchteilnahme ist nicht mehr gültig."));
        try {
            match.leave(player);
        } finally {
            session.clearMatchParticipant(match.getId());
        }
    }

    public void leaveLobby(ServerSession session) {
        try {
            cleanupLobbyMembership(session.getMembership());
        } finally {
            session.clearLobbyAndMatchBindings();
        }
    }

    public void cleanupSessionDomain(ServerSession session) {
        ServerPlayer departingPlayer = session.getPlayer();
        LobbyMember departingMembership = session.getMembership();
        RuntimeException failures = null;
        try {
            try {
                cleanupLobbyMembership(departingMembership);
            } catch (RuntimeException failure) {
                failures = cleanupFailure(failures, failure);
            }
            if (departingPlayer != null) {
                try {
                    home.unregisterPlayer(departingPlayer);
                } catch (RuntimeException failure) {
                    failures = cleanupFailure(failures, failure);
                }
            }
            if (failures != null) throw failures;
        } finally {
            session.clearDomainBindings();
        }
    }

    private void cleanupLobbyMembership(LobbyMember departingMembership) {
        if (departingMembership == null) return;
        Lobby lobby = home.findLobby(departingMembership.getLobbyId()).orElse(null);
        if (lobby == null) return;
        RuntimeException failures = null;
        try {
            lobby.leave(departingMembership.getPlayer());
        } catch (RuntimeException failure) {
            failures = cleanupFailure(failures, failure);
        }
        try {
            home.unregisterLobbyIfEmpty(lobby);
        } catch (RuntimeException failure) {
            failures = cleanupFailure(failures, failure);
        }
        if (failures != null) throw failures;
    }

    public void bindMatchParticipation(ServerSession session, UUID matchId) {
        LobbyMember membership = session.getMembership();
        if (membership == null) return;
        Optional<Lobby> optionalLobby = home.findLobby(membership.getLobbyId());
        if (optionalLobby.isEmpty()) return;
        Lobby lobby = optionalLobby.get();
        lobby.getMatch().ifPresent((match) -> {
            if (!match.getId().equals(matchId)) return;
            match.getParticipants().stream().filter(participant -> participant.getPlayer().getId().equals(session.getPlayer().getId())).findFirst().ifPresent(session::bindMatchParticipant);
        });
    }
}
