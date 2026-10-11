package org.hsw.rpc.handler;

import org.hsw.contract.IPlayerCallback;
import org.hsw.contract.model.IOption;
import org.hsw.contract.model.IPlayerMatchResult;
import org.hsw.contract.model.PlayerInfo;
import org.hsw.contract.model.PlayerMatchResult;
import org.hsw.domain.AnswerResult;
import org.hsw.domain.Option;
import org.hsw.exceptions.RemoteActionException;
import org.hsw.protocol.io.ContentReader;
import org.hsw.protocol.io.ContentWriter;
import org.hsw.protocol.message.ErrorCode;
import org.hsw.protocol.message.MethodId;
import org.hsw.rpc.communication.ClientRpcExchange;
import org.hsw.transfer.MatchResultView;
import org.hsw.transfer.QuestionView;
import org.hsw.transfer.RoundResultView;
import org.hsw.transfer.codec.MatchResultViewCodec;
import org.hsw.transfer.codec.QuestionViewCodec;
import org.hsw.transfer.codec.RoundResultViewCodec;

import java.util.*;

public final class PlayerServerProxy implements Runnable {
    private static final Set<MethodId> METHODS = Set.of(MethodId.RECEIVE_QUESTION, MethodId.RECEIVE_RESULT_FOR_ROUND, MethodId.RECEIVE_MATCH_STARTED, MethodId.RECEIVE_MATCH_FINISHED, MethodId.RECEIVE_LOBBY_STATE_CHANGED);
    private final IPlayerCallback target;
    private final ClientRpcExchange exchange;

    public PlayerServerProxy(IPlayerCallback target, ClientRpcExchange exchange) {
        this.target = Objects.requireNonNull(target);
        this.exchange = Objects.requireNonNull(exchange);
    }

    public boolean supports(MethodId method) {
        return METHODS.contains(method);
    }

    @Override
    public void run() {
        exchange.processNext(this::dispatch);
    }

    private void dispatch(MethodId method, ContentReader reader, ContentWriter writer) {
        switch (method) {
            case RECEIVE_QUESTION -> receiveQuestion(reader, writer);
            case RECEIVE_RESULT_FOR_ROUND -> receiveResultForRound(reader, writer);
            case RECEIVE_MATCH_STARTED -> receiveMatchStarted(reader);
            case RECEIVE_MATCH_FINISHED -> receiveMatchFinished(reader);
            case RECEIVE_LOBBY_STATE_CHANGED -> receiveLobbyStateChanged(reader);
            default ->
                    throw new RemoteActionException(ErrorCode.NOT_IMPLEMENTED, "Methode wird von diesem Handler nicht unterstützt.");
        }
    }

    private void receiveLobbyStateChanged(ContentReader reader) {
        UUID lobbyId = reader.readUUID();
        reader.requireEnd();
        target.receiveLobbyStateChanged(lobbyId);
    }

    private void receiveMatchStarted(ContentReader reader) {
        UUID matchId = reader.readUUID();
        reader.requireEnd();
        target.receiveMatchStarted(matchId);
    }

    private void receiveQuestion(ContentReader reader, ContentWriter writer) {
        QuestionView question = QuestionViewCodec.read(reader);
        reader.requireEnd();
        Optional<List<IOption>> options = question.options().map(values -> values.stream().<IOption>map(option -> new Option(option.index(), option.text())).toList());
        target.receiveQuestion(question.matchId(), question.text(), question.type(), options, question.roundNumber());
    }

    private void receiveResultForRound(ContentReader reader, ContentWriter writer) {
        RoundResultView result = RoundResultViewCodec.read(reader);
        reader.requireEnd();
        target.receiveResultForRound(result.matchId(), new AnswerResult(result.correct(), result.points(), result.correctAnswer()));
    }

    private void receiveMatchFinished(ContentReader reader) {
        MatchResultView result = MatchResultViewCodec.read(reader);
        List<IPlayerMatchResult> players = result.players().stream().<IPlayerMatchResult>map(player -> new PlayerMatchResult(new PlayerInfo(player.player().id(), player.player().name()), player.roundPoints(), player.totalAnswerTimeMillis(), player.rank())).toList();
        target.receiveMatchFinished(result.matchId(), players);
    }
}
