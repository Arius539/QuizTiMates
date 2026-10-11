package org.hsw.rpc.proxy;

import org.hsw.contract.IMatch;
import org.hsw.contract.model.IPlayerInfo;
import org.hsw.protocol.io.ContentReader;
import org.hsw.protocol.io.ContentWriter;
import org.hsw.protocol.io.ProtocolException;
import org.hsw.protocol.message.MethodId;
import org.hsw.protocol.rpc.RpcRequest;
import org.hsw.protocol.rpc.RpcResponse;
import org.hsw.rpc.communication.ClientConnection;

import java.util.Objects;
import java.util.UUID;

public class MatchClientProxy implements IMatch {
    private final ClientConnection connection;
    private final UUID matchId;

    public MatchClientProxy(final ClientConnection connection, final UUID matchId) {
        this.connection = Objects.requireNonNull(connection);
        this.matchId = Objects.requireNonNull(matchId);
    }

    @Override
    public UUID getId() {
        return matchId;
    }

    @Override
    public void answer(final IPlayerInfo player, final String answerText, final int round) {
        if (answerText == null || answerText.isBlank()) {
            throw new IllegalArgumentException("Keine Antwort angegeben.");
        }

        ContentWriter writer = new ContentWriter();
        writer.writeUUID(matchId);
        writer.writeString(answerText);
        writer.writeInt(round);
        writer.writeUUID(player.getId());

        try {
            final RpcResponse response = connection.request(new RpcRequest(MethodId.ANSWER_QUESTION, writer.getFields()));
            ContentReader reader = new ContentReader(response.content());
            reader.requireEnd();
        } catch (ProtocolException exception) {
            connection.abort(exception);
            throw exception;
        }
    }

    @Override
    public void leave(IPlayerInfo playerInfo) {
        ContentWriter writer = new ContentWriter();
        writer.writeUUID(matchId);
        writer.writeUUID(playerInfo.getId());

        try {
            final RpcResponse response = connection.request(new RpcRequest(MethodId.LEAVE_MATCH, writer.getFields()));
            ContentReader reader = new ContentReader(response.content());
            reader.requireEnd();
        } catch (ProtocolException exception) {
            connection.abort(exception);
            throw exception;
        }
    }
}
