package org.hsw.transfer.codec;

import org.hsw.contract.model.QuestionType;
import org.hsw.protocol.io.ContentFormatException;
import org.hsw.protocol.io.ContentReader;
import org.hsw.protocol.io.ContentWriter;
import org.hsw.protocol.io.ProtocolException;
import org.hsw.transfer.QuestionView;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class QuestionViewCodec {
    private QuestionViewCodec() {
    }

    public static void write(ContentWriter writer, QuestionView value) {
        writer.writeUUID(value.matchId());
        writer.writeString(value.text());
        writer.writeEnum(value.type());

        if (hasOptions(value.type())) {
            List<QuestionView.OptionView> options = value.options().orElseThrow(() -> new ProtocolException("Es wurden keine Antwortmöglichkeiten übergeben."));
            if (value.type() == QuestionType.TRUE_OR_FALSE && options.size() != 2) {
                throw new ProtocolException("True-or-False-Fragen benötigen genau zwei Antwortmöglichkeiten.");
            }
            writer.writeInt(options.size());
            for (QuestionView.OptionView option : options) {
                writer.writeChar(option.index());
                writer.writeString(option.text());
            }
        }

        writer.writeInt(value.roundNumber());
    }

    public static QuestionView read(ContentReader reader) {
        UUID matchId = reader.readUUID();
        String text = reader.readString();
        QuestionType type = reader.readEnum(QuestionType.class);
        Optional<List<QuestionView.OptionView>> options = Optional.empty();

        if (hasOptions(type)) {
            int optionCount = reader.readInt();
            if (optionCount < 1 || type == QuestionType.TRUE_OR_FALSE && optionCount != 2) {
                throw new ContentFormatException("Ungültige Anzahl von Antwortmöglichkeiten.");
            }
            List<QuestionView.OptionView> optionViews = new ArrayList<>();
            for (int i = 0; i < optionCount; i++) {
                optionViews.add(new QuestionView.OptionView(reader.readChar(), reader.readString()));
            }
            options = Optional.of(optionViews);
        }

        return new QuestionView(matchId, text, type, options, reader.readInt());
    }

    private static boolean hasOptions(QuestionType type) {
        return type == QuestionType.SINGLE_CHOICE || type == QuestionType.TRUE_OR_FALSE;
    }
}
