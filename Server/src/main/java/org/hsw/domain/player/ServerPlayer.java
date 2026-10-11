package org.hsw.domain.player;

import org.hsw.contract.IPlayerCallback;
import org.hsw.contract.model.*;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class ServerPlayer implements IPlayerInfo, IPlayerCallback {
    private final IPlayerCallback callback;
    private final IPlayerInfo info;

    public ServerPlayer(IPlayerInfo info, IPlayerCallback callback) {
        this.info = Objects.requireNonNull(info, "info");
        this.callback = Objects.requireNonNull(callback, "callback");
    }

    @Override
    public UUID getId() {
        return info.getId();
    }

    @Override
    public String getName() {
        return info.getName();
    }

    @Override
    public void receiveLobbyStateChanged(UUID lobbyId) {
        this.callback.receiveLobbyStateChanged(lobbyId);
    }

    @Override
    public void receiveMatchStarted(UUID matchId) {
        callback.receiveMatchStarted(matchId);
    }

    @Override
    public void receiveQuestion(UUID matchId, String question, QuestionType questionType, Optional<List<IOption>> options, int roundNumber) {
        callback.receiveQuestion(matchId, question, questionType, options, roundNumber);
    }

    @Override
    public void receiveResultForRound(UUID matchId, IAnswerResult<?> result) {
        callback.receiveResultForRound(matchId, result);
    }

    @Override
    public void receiveMatchFinished(UUID matchId, List<IPlayerMatchResult> results) {
        callback.receiveMatchFinished(matchId, results);
    }
}
