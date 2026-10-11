package org.hsw.transfer;

import java.util.Objects;
import java.util.UUID;

public record RoundResultView(UUID matchId, boolean correct, int points, String correctAnswer) {
    public RoundResultView {
        Objects.requireNonNull(matchId, "matchId");
        Objects.requireNonNull(correctAnswer, "correctAnswer");
    }
}
