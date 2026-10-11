package org.hsw.domain.match;

import org.hsw.config.BusinessLimits;
import org.hsw.contract.model.IPlayerInfo;
import org.hsw.contract.model.PlayerInfo;
import org.hsw.contract.model.PlayerMatchResult;

import java.util.ArrayList;
import java.util.List;

final class ParticipantScore {
    private final PlayerInfo player;
    private final int startOrder;
    private final List<RoundScore> rounds = new ArrayList<>();
    private int points;
    private int totalAnswerTimeMillis;

    ParticipantScore(IPlayerInfo player, int startOrder) {
        this.player = new PlayerInfo(player.getId(), player.getName());
        this.startOrder = startOrder;
    }

    int getPoints() {
        return points;
    }

    int getTotalAnswerTimeMillis() {
        return totalAnswerTimeMillis;
    }

    int getStartOrder() {
        return startOrder;
    }

    PlayerMatchResult snapshot(int rank) {
        return new PlayerMatchResult(player, rounds.stream().map(RoundScore::points).toList(), totalAnswerTimeMillis, rank);
    }

    void recordAnswer(int roundNumber, int earned, int answerTimeMillis) {
        if (roundNumber != rounds.size()) {
            throw new IllegalStateException("Rundenergebnis existiert bereits oder eine vorherige Runde fehlt.");
        }
        RoundScore score = new RoundScore(roundNumber, earned, answerTimeMillis);
        int nextPoints = Math.addExact(points, earned);
        int nextTime = Math.addExact(totalAnswerTimeMillis, answerTimeMillis);
        rounds.add(score);
        points = nextPoints;
        totalAnswerTimeMillis = nextTime;
    }

    void completeRound(int roundNumber) {
        if (rounds.size() == roundNumber) {
            recordAnswer(roundNumber, 0, BusinessLimits.ANSWER_TIME_MILLIS);
        } else if (rounds.size() != roundNumber + 1) {
            throw new IllegalStateException("Rundenhistorie passt nicht zur abzuschließenden Runde.");
        }
    }

    private record RoundScore(int roundNumber, int points, int answerTimeMillis) {
        private RoundScore {
            if (roundNumber < 0 || roundNumber >= BusinessLimits.MAX_ROUNDS || points < 0 || answerTimeMillis < 0 || answerTimeMillis > BusinessLimits.ANSWER_TIME_MILLIS) {
                throw new IllegalArgumentException("Ungültiges Rundenergebnis.");
            }
        }
    }
}
