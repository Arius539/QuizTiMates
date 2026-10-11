package org.hsw.transfer.codec;

import org.hsw.protocol.io.ContentReader;
import org.hsw.protocol.io.ContentWriter;
import org.hsw.transfer.RoundResultView;

public final class RoundResultViewCodec {
    private RoundResultViewCodec() {
    }

    public static void write(ContentWriter writer, RoundResultView value) {
        writer.writeUUID(value.matchId());
        writer.writeBoolean(value.correct());
        writer.writeInt(value.points());
        writer.writeString(value.correctAnswer());
    }

    public static RoundResultView read(ContentReader reader) {
        return new RoundResultView(reader.readUUID(), reader.readBoolean(), reader.readInt(), reader.readString());
    }
}
