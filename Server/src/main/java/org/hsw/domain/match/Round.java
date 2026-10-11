package org.hsw.domain.match;

import lombok.Getter;
import org.hsw.application.TimeService;
import org.hsw.contract.model.IOption;
import org.hsw.domain.exception.*;
import org.hsw.domain.question.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Round {
    private final TimeService timeService;
    private final Runnable earlyRoundEndCallback;
    private final Question question;
    private final long roundStart;
    private final long roundEnd;
    @Getter
    private final int roundNumber;
    private final Map<UUID, MatchParticipant> participants;
    private final ConcurrentHashMap<MatchParticipant, AnswerResult> answerResults;
    private boolean resultsPublished;

    public Round(Question question, Map<UUID, MatchParticipant> participants, long roundStart, long roundEnd, int roundNumber, TimeService timeService, Runnable earlyRoundEndCallback) {
        this.question = question;
        this.participants = participants;
        this.roundEnd = roundEnd;
        this.earlyRoundEndCallback = earlyRoundEndCallback;
        this.answerResults = new ConcurrentHashMap<>();
        this.roundStart = roundStart;
        this.roundNumber = roundNumber;
        this.timeService = Objects.requireNonNull(timeService);
    }

    public void publishQuestion() {
        final Optional<List<IOption>> optionsOpt;
        if (question instanceof SingleChoiceQuestion singleChoiceQuestion) {
            final List<IOption> oList = List.copyOf(singleChoiceQuestion.getOptions());
            optionsOpt = Optional.of(oList);
        } else if (question instanceof TrueOrFalseQuestion trueOrFalseQuestion) {
            final List<IOption> oList = List.copyOf(trueOrFalseQuestion.getOptions());
            optionsOpt = Optional.of(oList);
        } else {
            optionsOpt = Optional.empty();
        }
        for (MatchParticipant participant : participants.values()) {
            participant.getActingPlayer().receiveQuestion(participant.getMatchId(), question.getText(), question.getType(), optionsOpt, roundNumber);
        }
    }

    public void answer(final MatchParticipant participant, final String answerText, final long maxAnswerTimeMillis) {
        if (answerResults.containsKey(participant)) {
            throw new BusinessStateException("Participant already answered.");
        }
        long answerNanos = timeService.nowNanos();
        if (answerNanos - roundEnd >= 0) {
            throw new BusinessStateException("Round is over.");
        }
        if (answerText == null || answerText.isBlank()) {
            throw new BusinessOperationException("Keine Antwort angegeben.");
        }
        long elapsedNanos = answerNanos - roundStart;
        long answerTimeMillis = elapsedNanos / 1_000_000;
        Answer<?> answer;
        switch (question) {
            case SingleChoiceQuestion e -> {
                final char selectedOptionIndex = requireOptionIndex(answerText);
                final Option chosen = e.getOptions().stream().filter(o -> o.index() == selectedOptionIndex).findFirst().orElseThrow(() -> new BusinessOperationException("Gewählte Antwortmöglichkeit existiert nicht."));
                answer = new Answer<>(participant, answerTimeMillis, e.getType(), chosen);
            }
            case TrueOrFalseQuestion e -> {
                final char selectedOptionIndex = requireOptionIndex(answerText);
                final Option chosen = e.getOptions().stream().filter(o -> o.index() == selectedOptionIndex).findFirst().orElseThrow(() -> new BusinessOperationException("Gewählte Antwortmöglichkeit existiert nicht."));
                answer = new Answer<>(participant, answerTimeMillis, e.getType(), chosen);
            }
            case GuessingQuestion e -> {
                answer = new Answer<>(participant, answerTimeMillis, e.getType(), requireFiniteNumber(answerText));
            }
            case NumericQuestion e -> {
                answer = new Answer<>(participant, answerTimeMillis, e.getType(), requireFiniteNumber(answerText));
            }
            default -> {
                throw new RuntimeException("Unbekannte Frageart.", new IllegalStateException("Unexpected value: " + question));
            }
        }

        try {
            AnswerResult<?> result = question.getResult(answer, maxAnswerTimeMillis);
            participant.recordAnswer(roundNumber, result.points(), Math.toIntExact(answerTimeMillis));
            answerResults.put(participant, result);
        } catch (RuntimeException failure) {
            throw new RuntimeException("Antwortauswertung fehlgeschlagen.", failure);
        }

        if (answerResults.size() == participants.size()) {
            earlyRoundEndCallback.run();
        }
    }

    private static double requireFiniteNumber(String answerText) {
        final double answer;
        try {
            answer = Double.parseDouble(answerText);
        } catch (NumberFormatException cause) {
            throw new BusinessOperationException("Antwort muss eine Zahl sein.", cause);
        }
        if (!Double.isFinite(answer)) {
            throw new BusinessOperationException("Antwort muss eine endliche Zahl sein.");
        }
        return answer;
    }

    boolean allRemainingParticipantsAnswered() {
        return !participants.isEmpty() && answerResults.size() == participants.size();
    }

    private static char requireOptionIndex(String answerText) {
        if (answerText.length() != 1) {
            throw new BusinessOperationException("Genau ein Optionszeichen ist erforderlich.");
        }
        return answerText.charAt(0);
    }

    public void publishResults() {
        if (resultsPublished) return;
        resultsPublished = true;
        RuntimeException failures = null;
        for (MatchParticipant participant : participants.values()) {
            AnswerResult<?> result = answerResults.get(participant);

            if (result == null) {
                result = this.question.getResult(null, 0);
            }
            try {
                participant.getActingPlayer().receiveResultForRound(participant.getMatchId(), result);
            } catch (RuntimeException failure) {
                if (failures == null)
                    failures = new RuntimeException("Rundenergebnisse konnten nicht vollständig veröffentlicht werden.", failure);
                else failures.addSuppressed(failure);
            }
        }
        if (failures != null) throw failures;
    }

    void removePlayerAnswerResults(UUID playerId) {
        answerResults.keySet().removeIf(participant -> participant.getPlayer().getId().equals(playerId));
    }

}
