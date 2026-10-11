package org.hsw.rpc.handler;

import org.hsw.application.session.ServerSession;
import org.hsw.application.session.ServerSessionService;
import org.hsw.domain.match.Match;
import org.hsw.domain.player.ServerPlayer;
import org.hsw.protocol.io.ContentReader;
import org.hsw.protocol.io.ContentWriter;
import org.hsw.protocol.message.MethodId;
import org.hsw.rpc.connection.ServerRpcExchange;

import java.util.Set;
import java.util.UUID;

public class MatchServerProxy implements Runnable {

    private static final Set<MethodId> METHODS = Set.of(MethodId.GET_MATCH_ID, MethodId.ANSWER_QUESTION, MethodId.LEAVE_MATCH);
    private final ServerRpcExchange exchange;
    private final ServerSessionService sessionService;
    private final ServerSession session;

    public MatchServerProxy(ServerRpcExchange exchange, ServerSession session, ServerSessionService sessionService) {
        this.exchange = exchange;
        this.session = session;
        this.sessionService = sessionService;
    }

    public boolean supports(MethodId method) {
        return METHODS.contains(method);
    }

    @Override
    public void run() {
        exchange.processNext(this::dispatch);
    }

    private void dispatch(final MethodId method, final ContentReader reader, final ContentWriter writer) {
        switch (method) {
            case GET_MATCH_ID -> getId(sessionService.requireRunningMatch(session), reader, writer);
            case ANSWER_QUESTION -> answer(reader);
            case LEAVE_MATCH -> leaveMatch(reader);
            default -> throw new IllegalArgumentException("Keine Match-Methode.");
        }
    }

    private void getId(final Match match, final ContentReader reader, final ContentWriter writer) {
        reader.requireEnd();
        writer.writeUUID(match.getId());
    }

    private void answer(final ContentReader reader) {
        UUID requestedMatchId = reader.readUUID();
        final String answer = reader.readString();
        final int round = reader.readInt();
        final UUID playerId = reader.readUUID();
        reader.requireEnd();
        final Match match = sessionService.requireRunningMatch(session, requestedMatchId);
        final ServerPlayer player = session.requirePlayer(playerId);
        match.answer(player, answer, round);
    }

    private void leaveMatch(final ContentReader reader) {
        UUID requestedMatchId = reader.readUUID();
        UUID playerId = reader.readUUID();
        reader.requireEnd();
        sessionService.leaveMatch(session, requestedMatchId, playerId);
    }
}
