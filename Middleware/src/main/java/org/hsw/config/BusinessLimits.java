package org.hsw.config;

public final class BusinessLimits {
    public static final int MAX_PARTICIPANTS = 116;
    public static final int MIN_PARTICIPANTS_TO_START = 1;
    public static final int MAX_ROUNDS = 30;
    public static final int ANSWER_TIME_SECONDS = 30;
    public static final int ANSWER_TIME_MILLIS = ANSWER_TIME_SECONDS * 1000;

    private BusinessLimits() {
    }
}
