package org.hsw.protocol.io;

public final class TextValueCodec {
    private TextValueCodec() {
    }

    public static String encode(String value) {
        if (value == null) {
            throw new ProtocolException("Textfeld darf nicht null sein.");
        }
        return value.replace("\\", "\\\\").replace("\r", "\\r").replace("\n", "\\n");
    }

    public static String decode(String value) {
        StringBuilder decoded = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            if (current != '\\') {
                decoded.append(current);
                continue;
            }
            if (++index >= value.length()) {
                throw new ProtocolException("Unvollständige Textmaskierung.");
            }
            switch (value.charAt(index)) {
                case '\\' -> decoded.append('\\');
                case 'r' -> decoded.append('\r');
                case 'n' -> decoded.append('\n');
                default -> throw new ProtocolException("Ungültige Textmaskierung.");
            }
        }
        return decoded.toString();
    }
}
