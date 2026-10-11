package org.hsw.domain.match;

import lombok.Getter;
import org.hsw.application.*;
import org.hsw.config.BusinessLimits;
import org.hsw.contract.IMatch;
import org.hsw.contract.model.*;
import org.hsw.domain.exception.*;
import org.hsw.domain.lobby.LobbyMember;
import org.hsw.domain.question.Question;
import org.slf4j.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public final class Match implements IMatch {

    private static final Logger LOGGER = LoggerFactory.getLogger(Match.class);

    private final TimeService timeService;
    private final RoundScheduler roundScheduler;
    @Getter
    private final UUID id;
    private final Queue<Question> questionLineup;
    private final Map<UUID, MatchParticipant> participants;
    private final List<ParticipantScore> participantScores;
    private List<IPlayerMatchResult> finishedResults;
    private Round currentRound;
    @Getter
    private boolean finished;
    private ScheduledFuture<?> nextRoundTask;

    public Match(List<LobbyMember> members, Queue<Question> questionLineup) {
        this(members, questionLineup, new TimeService(), StateExecutor.getInstance()::schedule);
    }

    Match(List<LobbyMember> members, Queue<Question> questionLineup, TimeService timeService, RoundScheduler roundScheduler) {
        this.id = UUID.randomUUID();
        this.participants = new LinkedHashMap<>();
        this.participantScores = new ArrayList<>();
        for (LobbyMember lobbyMember : members) {
            ParticipantScore score = new ParticipantScore(lobbyMember.getPlayer(), participantScores.size());
            participantScores.add(score);
            final MatchParticipant participant = new MatchParticipant(id, lobbyMember.getPlayer(), score);
            participants.put(lobbyMember.getPlayer().getId(), participant);
        }
        this.finished = false;
        this.questionLineup = questionLineup;
        this.currentRound = null;
        this.timeService = Objects.requireNonNull(timeService);
        this.roundScheduler = Objects.requireNonNull(roundScheduler);
    }

    private static RuntimeException finishingFailure(RuntimeException failures, RuntimeException failure) {
        if (failures == null) return new RuntimeException("Match konnte nicht vollständig beendet werden.", failure);
        failures.addSuppressed(failure);
        return failures;
    }

    /**
     * Delegiert die Antwort an die aktuelle Runde; deren Timer steuert den Rundenwechsel.
     *
     * @param player     der beantwortende Spieler
     * @param answerText das Optionszeichen oder die numerische Antwort als Text
     * @param round      die Rundennummer, zu der die Antwort gehört
     */
    @Override
    public void answer(final IPlayerInfo player, final String answerText, final int round) {
        try {
            if (finished || currentRound == null) {
                throw new BusinessStateException("Kein laufendes Match.");
            }
            if (answerText == null || answerText.isBlank()) {
                throw new BusinessOperationException("Keine Antwort angegeben.");
            }
            if (round != currentRound.getRoundNumber()) {
                throw new BusinessStateException("Antwort passt nicht zur aktuellen Frage");
            }
            final MatchParticipant participant = participants.get(player.getId());
            if (participant == null) {
                throw new BusinessStateException("Spieler nimmt nicht am Match teil.");
            }
            currentRound.answer(participant, answerText, BusinessLimits.ANSWER_TIME_MILLIS);
        } catch (BusinessOperationException | BusinessStateException expected) {
            throw expected;
        } catch (RuntimeException failure) {
            throw finishAfterFailure("Antwortverarbeitung fehlgeschlagen.", failure);
        }
    }

    @Override
    public void leave(IPlayerInfo playerInfo) {
        removeParticipantForCleanup(playerInfo.getId());
    }

    public Optional<List<IPlayerMatchResult>> getFinishedResults() {
        return Optional.ofNullable(finishedResults);
    }

    public void start() {
        if (currentRound != null || finished) {
            throw new BusinessStateException("Match bereits gestartet.");
        }
        try {
            for (MatchParticipant participant : participants.values()) {
                participant.getActingPlayer().receiveMatchStarted(this.id);
            }
        } catch (RuntimeException failure) {
            throw finishAfterFailure("Matchstart fehlgeschlagen.", failure);
        }
        onNextRound();
    }

    public List<MatchParticipant> getParticipants() {
        return List.copyOf(participants.values());
    }

    private void onNextRound() {
        if (finished) return;
        try {
            advanceRound();
        } catch (RuntimeException failure) {
            throw finishAfterFailure("Rundenwechsel fehlgeschlagen.", failure);
        }
    }

    private void advanceRound() {
        cancelRoundTimer();
        if (currentRound != null) {
            completeCurrentRound();
            currentRound.publishResults();
        }
        int nextRoundNumber = currentRound == null ? 0 : currentRound.getRoundNumber() + 1;
        if (participants.isEmpty() || nextRoundNumber >= BusinessLimits.MAX_ROUNDS || questionLineup.isEmpty()) {
            finish();
            return;
        }

        long roundStart = timeService.nowNanos();
        currentRound = new Round(questionLineup.poll(), participants, roundStart, timeService.add(roundStart, Duration.ofMillis(BusinessLimits.ANSWER_TIME_MILLIS)), nextRoundNumber, timeService, this::onNextRound);
        currentRound.publishQuestion();
        Round expectedRound = currentRound;
        nextRoundTask = roundScheduler.schedule(() -> onRoundDeadline(expectedRound), BusinessLimits.ANSWER_TIME_MILLIS, TimeUnit.MILLISECONDS);
    }

    private void onRoundDeadline(Round expectedRound) {
        if (finished || currentRound != expectedRound) return;
        try {
            onNextRound();
        } catch (RuntimeException failure) {
            LOGGER.error("Rundenwechsel fehlgeschlagen: matchId={}, round={}", id, expectedRound.getRoundNumber(), failure);
        }
    }

    private void cancelRoundTimer() {
        if (nextRoundTask != null) {
            ScheduledFuture<?> task = nextRoundTask;
            nextRoundTask = null;
            task.cancel(false);
        }
    }

    private void finish() {
        if (finished) return;
        completeCurrentRound();
        List<ParticipantScore> rankedScores = participantScores.stream().sorted(Comparator.comparingInt(ParticipantScore::getPoints).reversed().thenComparingInt(ParticipantScore::getTotalAnswerTimeMillis).thenComparingInt(ParticipantScore::getStartOrder)).toList();
        List<IPlayerMatchResult> results = new ArrayList<>(rankedScores.size());
        for (int index = 0; index < rankedScores.size(); index++) {
            results.add(rankedScores.get(index).snapshot(index + 1));
        }
        finishedResults = List.copyOf(results);
        finished = true;
        RuntimeException failures = null;
        try {
            cancelRoundTimer();
        } catch (RuntimeException failure) {
            failures = finishingFailure(failures, failure);
        }
        if (currentRound != null) {
            try {
                currentRound.publishResults();
            } catch (RuntimeException failure) {
                failures = finishingFailure(failures, failure);
            }
        }
        for (MatchParticipant participant : List.copyOf(participants.values())) {
            try {
                participant.getActingPlayer().receiveMatchFinished(id, finishedResults);
            } catch (RuntimeException failure) {
                failures = finishingFailure(failures, failure);
            }
        }
        if (failures != null) throw failures;
    }

    private void completeCurrentRound() {
        if (currentRound != null) {
            for (ParticipantScore score : participantScores) {
                score.completeRound(currentRound.getRoundNumber());
            }
        }
    }

    private RuntimeException finishAfterFailure(String message, RuntimeException cause) {
        RuntimeException failure = new RuntimeException(message, cause);
        try {
            finish();
        } catch (RuntimeException finishingFailure) {
            failure.addSuppressed(finishingFailure);
        }
        return failure;
    }

    public void removeParticipantForCleanup(UUID playerId) {
        MatchParticipant removed = participants.remove(playerId);
        if (currentRound != null) currentRound.removePlayerAnswerResults(playerId);
        if (removed == null || finished) return;
        if (participants.isEmpty()) {
            finish();
        } else if (currentRound != null && currentRound.allRemainingParticipantsAnswered()) {
            onNextRound();
        }
    }

    @FunctionalInterface
    interface RoundScheduler {
        ScheduledFuture<?> schedule(Runnable action, long delay, TimeUnit unit);
    }
}
