package org.hsw.application;

import java.security.SecureRandom;

public final class LobbyCodeGenerator {
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int DEFAULT_LENGTH = 3;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static String generateRandomString() {
        return generateRandomString(DEFAULT_LENGTH);
    }

    public static String generateRandomString(int length) {
        char[] text = new char[length];
        for (int i = 0; i < length; i++) {
            text[i] = CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length()));
        }
        return new String(text);
    }

}
