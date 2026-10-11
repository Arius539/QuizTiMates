package org.hsw.domain.lobby;

import lombok.Getter;
import org.hsw.application.QuestionLoader;
import org.hsw.config.BusinessLimits;
import org.hsw.contract.*;
import org.hsw.contract.model.*;
import org.hsw.domain.exception.*;
import org.hsw.domain.match.Match;
import org.hsw.domain.player.ServerPlayer;
import org.hsw.domain.question.Question;

import java.util.*;

public final class Lobby implements ILobby {
    private final UUID id;
    private final String code;
    private final Map<UUID, LobbyMember> membersByPlayerId;
    @Getter
    private UUID hostPlayerId;
    @Getter
    private Optional<Match> match;

    public Lobby(String code, ServerPlayer host) {
        this.id = UUID.randomUUID();
        this.membersByPlayerId = new LinkedHashMap<>();
        this.code = Objects.requireNonNull(code);
        this.hostPlayerId = Objects.requireNonNull(host).getId();
        match = Optional.empty();
        join(host);
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public IPlayerInfo getHost() {
        if (isEmpty()) {
            throw new BusinessStateException("Lobby hat keinen Gastgeber.");
        }
        return membersByPlayerId.get(hostPlayerId).getPlayer();
    }

    @Override
    public List<IPlayerInfo> getPlayers() {
        return membersByPlayerId.values().stream().<IPlayerInfo>map(LobbyMember::getPlayer).toList();
    }

    @Override
    public boolean isMatchRunning() {
        return match.filter(currentMatch -> !currentMatch.isFinished()).isPresent();
    }

    @Override
    public Optional<IMatch> getRunningMatch() {
        return match.filter(currentMatch -> !currentMatch.isFinished()).map(currentMatch -> (IMatch) currentMatch);
    }

    @Override
    public void startMatch(IPlayerInfo player, int roundCount, Set<Topic> topics) {
        if (!Objects.equals(hostPlayerId, player.getId())) {
            throw new BusinessStateException("Nur der Gastgeber kann den Match starten.");
        }
        if (isMatchRunning()) {
            throw new BusinessStateException("Match bereits gestartet.");
        }
        if (membersByPlayerId.size() < BusinessLimits.MIN_PARTICIPANTS_TO_START) {
            throw new BusinessStateException("Mindestens " + BusinessLimits.MIN_PARTICIPANTS_TO_START + " Spieler sind für einen Matchstart erforderlich.");
        }
        if (roundCount < 1 || roundCount > BusinessLimits.MAX_ROUNDS) {
            throw new BusinessOperationException("Die Rundenanzahl muss zwischen 1 und " + BusinessLimits.MAX_ROUNDS + " liegen.");
        }
        if (topics == null || topics.isEmpty() || topics.stream().anyMatch(Objects::isNull)) {
            throw new BusinessOperationException("Mindestens ein gültiges Thema muss ausgewählt sein.");
        }
        List<Question> selectedQuestions = QuestionLoader.getRandomQuestions(roundCount, topics);
        if (selectedQuestions.isEmpty()) {
            throw new BusinessOperationException("Für die ausgewählten Themen sind keine Fragen verfügbar.");
        }
        Match match = new Match(membersByPlayerId.values().stream().toList(), new LinkedList<>(selectedQuestions));
        this.match = Optional.of(match);
        match.start();
    }

    @Override
    public void leave(IPlayerInfo player) {
        UUID playerId = player.getId();
        if (!membersByPlayerId.containsKey(playerId)) {
            throw new BusinessStateException("Lobbymitgliedschaft existiert nicht.");
        }
        removeMemberForCleanup(playerId);
        informPlayersLobbyStateChanged();
    }

    public List<LobbyMember> getMembers() {
        return List.copyOf(membersByPlayerId.values());
    }

    public boolean isEmpty() {
        return membersByPlayerId.isEmpty();
    }

    public LobbyMember getMembership(UUID playerId) {
        LobbyMember member = membersByPlayerId.get(playerId);
        if (member == null) {
            throw new IllegalStateException("Lobbymitgliedschaft existiert nicht.");
        }
        return member;
    }

    public LobbyMember join(ServerPlayer player) {
        if (membersByPlayerId.containsKey(player.getId())) {
            throw new BusinessStateException("Spieler ist bereits in dieser Lobby.");
        }
        if (membersByPlayerId.size() >= BusinessLimits.MAX_PARTICIPANTS) {
            throw new BusinessStateException("Lobby ist voll.");
        }
        LobbyMember member = new LobbyMember(id, player);
        membersByPlayerId.put(player.getId(), member);
        informPlayersLobbyStateChanged();
        return member;
    }

    private void informPlayersLobbyStateChanged() {
        for (LobbyMember member : getMembers()) {
            member.getPlayer().receiveLobbyStateChanged(this.id);
        }
    }

    private boolean removeMemberForCleanup(UUID playerId) {
        RuntimeException failure = null;
        try {
            match.ifPresent(currentMatch -> currentMatch.removeParticipantForCleanup(playerId));
        } catch (RuntimeException cause) {
            failure = new RuntimeException("Lobbymitglied konnte nicht vollständig bereinigt werden.", cause);
        }
        LobbyMember removed = membersByPlayerId.remove(playerId);
        if (Objects.equals(playerId, hostPlayerId)) {
            hostPlayerId = membersByPlayerId.isEmpty() ? null : membersByPlayerId.keySet().iterator().next();
        }
        if (failure != null) throw failure;
        return removed != null;
    }
}
