package org.hsw.application.port;

import org.hsw.contract.model.IAnswerResult;
import org.hsw.contract.model.IOption;
import org.hsw.contract.model.IPlayerMatchResult;
import org.hsw.contract.model.QuestionType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public interface IPlayerCallbackEventRegister {
    void subscribeLobbyStateChanged(Consumer<UUID> action);

    void unsubscribeLobbyStateChanged();

    void subscribeMatchStarted(Consumer<UUID> action);

    void unsubscribeMatchStarted();

    void subscribeQuestion(QuestionCallback action);

    void unsubscribeQuestion();

    void subscribeResultForRound(ResultCallback action);

    void unsubscribeResultForRound();

    void subscribeMatchFinished(MatchFinishedCallback action);

    void unsubscribeMatchFinished();

    @FunctionalInterface
    interface QuestionCallback {
        void accept(UUID matchId, String question, QuestionType questionType, Optional<List<IOption>> options, int roundNumber);
    }

    @FunctionalInterface
    interface ResultCallback {
        void accept(UUID matchId, IAnswerResult<?> result);
    }

    @FunctionalInterface
    interface MatchFinishedCallback {
        void accept(UUID matchId, List<IPlayerMatchResult> results);
    }
}
