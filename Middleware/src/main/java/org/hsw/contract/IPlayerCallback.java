package org.hsw.contract;

import org.hsw.contract.model.IAnswerResult;
import org.hsw.contract.model.IOption;
import org.hsw.contract.model.IPlayerMatchResult;
import org.hsw.contract.model.QuestionType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IPlayerCallback {
    void receiveLobbyStateChanged(UUID lobbyId);

    void receiveMatchStarted(UUID matchId);

    void receiveQuestion(UUID matchId, String question, QuestionType questionType, Optional<List<IOption>> options, int roundNumber);

    void receiveResultForRound(UUID matchId, IAnswerResult<?> result);

    void receiveMatchFinished(UUID matchId, List<IPlayerMatchResult> results);
}
