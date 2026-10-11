package org.hsw.transfer;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public record MatchResultView(UUID matchId, List<PlayerResultView> players) {
    public MatchResultView {
        Objects.requireNonNull(matchId, "matchId");
        players = List.copyOf(players);
    }

    public record PlayerResultView(PlayerView player, int rank, int totalAnswerTimeMillis, List<Integer> roundPoints) {
        public PlayerResultView {
            Objects.requireNonNull(player, "player");
            roundPoints = List.copyOf(roundPoints);
        }
    }
}
