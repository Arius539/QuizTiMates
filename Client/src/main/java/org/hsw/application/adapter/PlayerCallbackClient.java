package org.hsw.application.adapter;

import org.hsw.application.port.IPlayerCallbackEventRegister;
import org.hsw.contract.IPlayerCallback;
import org.hsw.contract.model.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Consumer;

@Component
public class PlayerCallbackClient implements IPlayerCallback, IPlayerCallbackEventRegister {
    private static final Consumer<UUID> NO_UUID_CALLBACK = ignored -> {
    };
    private static final QuestionCallback NO_QUESTION_CALLBACK = (matchId, question, questionType, options, roundNumber) -> {
    };
    private static final ResultCallback NO_RESULT_CALLBACK = (matchId, result) -> {
    };
    private static final MatchFinishedCallback NO_MATCH_FINISHED_CALLBACK = (matchId, results) -> {
    };

    private volatile Consumer<UUID> matchStartedCallback = NO_UUID_CALLBACK;
    private volatile MatchFinishedCallback matchFinishedCallback = NO_MATCH_FINISHED_CALLBACK;
    private volatile Consumer<UUID> lobbyStateChangedCallback = NO_UUID_CALLBACK;
    private volatile QuestionCallback questionCallback = NO_QUESTION_CALLBACK;
    private volatile ResultCallback resultForRoundCallback = NO_RESULT_CALLBACK;

    @Override
    public void receiveLobbyStateChanged(UUID lobbyId) {
        lobbyStateChangedCallback.accept(lobbyId);
    }

    @Override
    public void subscribeLobbyStateChanged(Consumer<UUID> action) {
        lobbyStateChangedCallback = Objects.requireNonNull(action, "action");
    }

    @Override
    public void unsubscribeLobbyStateChanged() {
        lobbyStateChangedCallback = NO_UUID_CALLBACK;
    }

    @Override
    public void receiveMatchStarted(UUID matchId) {
        matchStartedCallback.accept(matchId);
    }

    @Override
    public void subscribeMatchStarted(Consumer<UUID> action) {
        matchStartedCallback = Objects.requireNonNull(action, "action");
    }

    @Override
    public void unsubscribeMatchStarted() {
        matchStartedCallback = NO_UUID_CALLBACK;
    }

    @Override
    public void receiveQuestion(UUID matchId, String question, QuestionType questionType, Optional<List<IOption>> options, int roundNumber) {
        questionCallback.accept(matchId, question, questionType, options, roundNumber);
    }

    @Override
    public void subscribeQuestion(QuestionCallback action) {
        questionCallback = Objects.requireNonNull(action, "action");
    }

    @Override
    public void unsubscribeQuestion() {
        questionCallback = NO_QUESTION_CALLBACK;
    }

    @Override
    public void receiveResultForRound(UUID matchId, IAnswerResult<?> result) {
        resultForRoundCallback.accept(matchId, result);
    }

    @Override
    public void subscribeResultForRound(ResultCallback action) {
        resultForRoundCallback = Objects.requireNonNull(action, "action");
    }

    @Override
    public void unsubscribeResultForRound() {
        resultForRoundCallback = NO_RESULT_CALLBACK;
    }

    @Override
    public void receiveMatchFinished(UUID matchId, List<IPlayerMatchResult> results) {
        matchFinishedCallback.accept(matchId, results);
    }

    @Override
    public void subscribeMatchFinished(MatchFinishedCallback action) {
        matchFinishedCallback = Objects.requireNonNull(action, "action");
    }

    @Override
    public void unsubscribeMatchFinished() {
        matchFinishedCallback = NO_MATCH_FINISHED_CALLBACK;
    }
}
