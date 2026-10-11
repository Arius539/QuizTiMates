package org.hsw.transfer.codec;

import org.hsw.protocol.io.ContentReader;
import org.hsw.protocol.io.ContentWriter;
import org.hsw.transfer.PlayerView;

public final class PlayerViewCodec {
    private PlayerViewCodec() {
    }

    public static void write(ContentWriter writer, PlayerView value) {
        writer.writeUUID(value.id());
        writer.writeString(value.name());
    }

    public static PlayerView read(ContentReader reader) {
        return new PlayerView(reader.readUUID(), reader.readString());
    }
}
