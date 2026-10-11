package org.hsw.application.adapter;

import org.hsw.contract.model.*;
import org.hsw.domain.ClientQuestion;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Consumer;

@Component
public final class MatchEventCoordinator {

    private final Map<UUID, ClientQuestion> latestQuestions = new HashMap<>();
    private final Map<UUID, Consumer<ClientQuestion>> questionCallbacks = new HashMap<>();
    private final Map<UUID, RoundResultSnapshot> latestResults = new HashMap<>();
    private final Map<UUID, Consumer<RoundResultSnapshot>> resultCallbacks = new HashMap<>();
    private final Set<UUID> finishedMatches = new HashSet<>();
    private final Map<UUID, Consumer<List<IPlayerMatchResult>>> matchFinishedCallbacks = new HashMap<>();
    private UUID latestFinishedMatchId;
    private List<IPlayerMatchResult> latestFinishedResults;

    public synchronized void notifyQuestion(UUID matchId, String questionText, QuestionType questionType, Optional<List<IOption>> options, int roundNumber) {
        if (finishedMatches.contains(matchId)) return;
        ClientQuestion question = new ClientQuestion(questionText, questionType, options, roundNumber);

        ClientQuestion previous = latestQuestions.get(matchId);
        if (previous != null && previous.roundNumber() >= roundNumber) {
            return;
        }

        latestQuestions.put(matchId, question);

        Consumer<ClientQuestion> callback = questionCallbacks.get(matchId);
        if (callback != null) {
            callback.accept(question);
        }
    }

    public synchronized void subscribeQuestion(UUID matchId, Consumer<ClientQuestion> callback) {
        questionCallbacks.put(matchId, Objects.requireNonNull(callback));

        ClientQuestion latestQuestion = latestQuestions.get(matchId);
        if (latestQuestion != null) {
            callback.accept(latestQuestion);
        }
    }

    public synchronized void unsubscribeQuestion(UUID matchId, Consumer<ClientQuestion> callback) {
        questionCallbacks.remove(matchId, callback);
    }

    public synchronized void notifyResultForRound(UUID matchId, IAnswerResult<?> result) {
        if (finishedMatches.contains(matchId)) return;
        ClientQuestion latestQuestion = latestQuestions.get(matchId);
        if (latestQuestion == null) return;
        IAnswerResult<?> currentResult = Objects.requireNonNull(result, "result");
        RoundResultSnapshot snapshot = new RoundResultSnapshot(latestQuestion.roundNumber(), currentResult);
        latestResults.put(matchId, snapshot);

        Consumer<RoundResultSnapshot> callback = resultCallbacks.get(matchId);
        if (callback != null) {
            callback.accept(snapshot);
        }
    }

    public synchronized void subscribeResultForRound(UUID matchId, Consumer<RoundResultSnapshot> callback) {
        Consumer<RoundResultSnapshot> currentCallback = Objects.requireNonNull(callback, "callback");
        resultCallbacks.put(matchId, currentCallback);

        RoundResultSnapshot latestResult = latestResults.get(matchId);
        if (latestResult != null) {
            currentCallback.accept(latestResult);
        }
    }

    public synchronized void unsubscribeResultForRound(UUID matchId, Consumer<RoundResultSnapshot> callback) {
        resultCallbacks.remove(matchId, callback);
    }

    public synchronized void notifyMatchFinished(UUID matchId, List<IPlayerMatchResult> results) {
        Objects.requireNonNull(matchId, "matchId");
        if (finishedMatches.contains(matchId)) return;
        List<IPlayerMatchResult> snapshot = List.copyOf(results);
        latestFinishedMatchId = matchId;
        latestFinishedResults = snapshot;
        finishedMatches.add(matchId);
        Consumer<List<IPlayerMatchResult>> callback = matchFinishedCallbacks.get(matchId);
        if (callback != null) {
            callback.accept(snapshot);
        }
    }

    public synchronized void subscribeMatchFinished(UUID matchId, Consumer<List<IPlayerMatchResult>> callback) {
        matchFinishedCallbacks.put(matchId, Objects.requireNonNull(callback));
        getFinishedResults(matchId).ifPresent(callback);
    }

    public synchronized Optional<List<IPlayerMatchResult>> getFinishedResults(UUID matchId) {
        return Objects.requireNonNull(matchId, "matchId").equals(latestFinishedMatchId) ? Optional.of(latestFinishedResults) : Optional.empty();
    }

    public synchronized Optional<UUID> getLatestFinishedMatchId() {
        return Optional.ofNullable(latestFinishedMatchId);
    }

    public synchronized boolean isMatchFinished(UUID matchId) {
        return finishedMatches.contains(matchId);
    }

    public synchronized void clearMatch(UUID matchId) {
        questionCallbacks.remove(matchId);
        latestQuestions.remove(matchId);
        resultCallbacks.remove(matchId);
        latestResults.remove(matchId);
        matchFinishedCallbacks.remove(matchId);
        finishedMatches.add(matchId);
    }

    public synchronized void resetSession() {
        questionCallbacks.clear();
        latestQuestions.clear();
        resultCallbacks.clear();
        latestResults.clear();
        matchFinishedCallbacks.clear();
        finishedMatches.clear();
        latestFinishedMatchId = null;
        latestFinishedResults = null;
    }

    public record RoundResultSnapshot(int roundNumber, IAnswerResult<?> result) {
        public RoundResultSnapshot {
            Objects.requireNonNull(result, "result");
        }
    }
}
