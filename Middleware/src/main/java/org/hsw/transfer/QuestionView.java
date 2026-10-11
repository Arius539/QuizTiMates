package org.hsw.transfer;

import org.hsw.contract.model.QuestionType;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public record QuestionView(UUID matchId, String text, QuestionType type, Optional<List<OptionView>> options,
                           int roundNumber) {
    public QuestionView {
        Objects.requireNonNull(matchId, "matchId");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(type, "type");
        options = Objects.requireNonNull(options, "options").map(List::copyOf);
    }

    public record OptionView(char index, String text) {
        public OptionView {
            Objects.requireNonNull(text, "text");
        }
    }
}
