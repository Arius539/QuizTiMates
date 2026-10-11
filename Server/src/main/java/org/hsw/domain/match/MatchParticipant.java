package org.hsw.domain.match;

import lombok.Getter;
import org.hsw.contract.IPlayerCallback;
import org.hsw.domain.player.ServerPlayer;

import java.util.Objects;
import java.util.UUID;

public final class MatchParticipant {
    @Getter
    private final UUID matchId;
    @Getter
    private final ServerPlayer player;
    private final ParticipantScore score;

    public MatchParticipant(UUID matchId, ServerPlayer player) {
        this(matchId, player, new ParticipantScore(player, 0));
    }

    MatchParticipant(UUID matchId, ServerPlayer player, ParticipantScore score) {
        this.matchId = Objects.requireNonNull(matchId);
        this.player = Objects.requireNonNull(player);
        this.score = Objects.requireNonNull(score);
    }

    public IPlayerCallback getActingPlayer() {
        return player;
    }

    public int getPoints() {
        return score.getPoints();
    }

    void recordAnswer(int roundNumber, int points, int answerTimeMillis) {
        score.recordAnswer(roundNumber, points, answerTimeMillis);
    }
}
