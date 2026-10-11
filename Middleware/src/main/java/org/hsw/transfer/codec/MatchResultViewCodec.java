package org.hsw.transfer.codec;

import org.hsw.config.BusinessLimits;
import org.hsw.protocol.io.ContentFormatException;
import org.hsw.protocol.io.ContentReader;
import org.hsw.protocol.io.ContentWriter;
import org.hsw.transfer.MatchResultView;
import org.hsw.transfer.MatchResultView.PlayerResultView;
import org.hsw.transfer.PlayerView;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class MatchResultViewCodec {
    private MatchResultViewCodec() {
    }

    public static void write(ContentWriter writer, MatchResultView value) {
        writer.writeUUID(value.matchId());
        writer.writeInt(value.players().size());
        for (PlayerResultView player : value.players()) {
            PlayerViewCodec.write(writer, player.player());
            writer.writeInt(player.rank());
            writer.writeInt(player.totalAnswerTimeMillis());
            writer.writeInt(player.roundPoints().size());
            for (int points : player.roundPoints()) writer.writeInt(points);
        }
    }

    public static MatchResultView read(ContentReader reader) {
        UUID matchId = reader.readUUID();
        int count = boundedCount(reader.readInt(), BusinessLimits.MAX_PARTICIPANTS);
        List<PlayerResultView> players = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            PlayerView player = PlayerViewCodec.read(reader);
            int rank = reader.readInt();
            int time = reader.readInt();
            int rounds = boundedCount(reader.readInt(), BusinessLimits.MAX_ROUNDS);
            List<Integer> points = new ArrayList<>(rounds);
            for (int round = 0; round < rounds; round++) points.add(reader.readInt());
            players.add(new PlayerResultView(player, rank, time, points));
        }
        reader.requireEnd();
        return new MatchResultView(matchId, players);
    }

    private static int boundedCount(int count, int maximum) {
        if (count < 0 || count > maximum) throw new ContentFormatException("Ungültige Ergebnisanzahl.");
        return count;
    }
}
