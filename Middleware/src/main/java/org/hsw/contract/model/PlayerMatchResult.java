package org.hsw.contract.model;

import java.util.List;
import java.util.Objects;

public record PlayerMatchResult(IPlayerInfo player, List<Integer> roundPoints, int totalAnswerTimeMillis,
                                int rank) implements IPlayerMatchResult {
    public PlayerMatchResult {
        Objects.requireNonNull(player, "player");
        player = new PlayerInfo(player.getId(), player.getName());
        roundPoints = List.copyOf(roundPoints);
    }

    @Override
    public IPlayerInfo getPlayer() {
        return player;
    }

    @Override
    public List<Integer> getRoundPoints() {
        return roundPoints;
    }

    @Override
    public int getTotalPoints() {
        return roundPoints.stream().reduce(0, Math::addExact);
    }

    @Override
    public int getTotalAnswerTimeMillis() {
        return totalAnswerTimeMillis;
    }

    @Override
    public int getRank() {
        return rank;
    }
}
