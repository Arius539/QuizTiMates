package org.hsw.rpc.proxy;

import org.hsw.contract.IPlayerCallback;
import org.hsw.contract.model.*;
import org.hsw.protocol.io.ContentWriter;
import org.hsw.protocol.message.MethodId;
import org.hsw.protocol.rpc.RpcRequest;
import org.hsw.rpc.connection.ServerCallbackSender;
import org.hsw.transfer.*;
import org.hsw.transfer.codec.*;

import java.util.*;
import java.util.function.Consumer;

public class PlayerClientProxy implements IPlayerCallback {
    private final ServerCallbackSender sender;
    private final Consumer<UUID> onMatchStarted;
    private final Consumer<UUID> onMatchFinished;

    public PlayerClientProxy(ServerCallbackSender sender, Consumer<UUID> onMatchStarted, Consumer<UUID> onMatchFinished) {
        this.sender = Objects.requireNonNull(sender);
        this.onMatchStarted = Objects.requireNonNull(onMatchStarted);
        this.onMatchFinished = Objects.requireNonNull(onMatchFinished);
    }

    @Override
    public void receiveLobbyStateChanged(UUID lobbyId) {
        sender.enqueue(() -> {
            ContentWriter writer = new ContentWriter();
            writer.writeUUID(lobbyId);
            return new RpcRequest(MethodId.RECEIVE_LOBBY_STATE_CHANGED, writer.getFields());
        });
    }

    @Override
    public void receiveMatchStarted(UUID matchId) {
        sender.enqueue(() -> {
            ContentWriter writer = new ContentWriter();
            writer.writeUUID(matchId);
            return new RpcRequest(MethodId.RECEIVE_MATCH_STARTED, writer.getFields());
        }, () -> onMatchStarted.accept(matchId));
    }

    @Override
    public void receiveQuestion(UUID matchId, String question, QuestionType questionType, Optional<List<IOption>> options, final int roundNumber) {
        sender.enqueue(() -> {
            ContentWriter writer = new ContentWriter();
            Optional<List<QuestionView.OptionView>> optionViews = options.map(values -> values.stream().map(option -> new QuestionView.OptionView(option.index(), option.text())).toList());
            QuestionViewCodec.write(writer, new QuestionView(matchId, question, questionType, optionViews, roundNumber));
            return new RpcRequest(MethodId.RECEIVE_QUESTION, writer.getFields());
        });
    }

    @Override
    public void receiveResultForRound(UUID matchId, final IAnswerResult<?> result) {
        sender.enqueue(() -> {
            ContentWriter writer = new ContentWriter();
            RoundResultViewCodec.write(writer, new RoundResultView(matchId, result.correct(), result.points(), result.correctAnswer().toString()));
            return new RpcRequest(MethodId.RECEIVE_RESULT_FOR_ROUND, writer.getFields());
        });
    }

    @Override
    public void receiveMatchFinished(UUID matchId, List<IPlayerMatchResult> results) {
        sender.enqueue(() -> {
            ContentWriter writer = new ContentWriter();
            List<MatchResultView.PlayerResultView> players = results.stream().map(result -> new MatchResultView.PlayerResultView(new PlayerView(result.getPlayer().getId(), result.getPlayer().getName()), result.getRank(), result.getTotalAnswerTimeMillis(), result.getRoundPoints())).toList();
            MatchResultViewCodec.write(writer, new MatchResultView(matchId, players));
            return new RpcRequest(MethodId.RECEIVE_MATCH_FINISHED, writer.getFields());
        }, () -> onMatchFinished.accept(matchId));
    }
}
