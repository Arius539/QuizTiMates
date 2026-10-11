package org.hsw.application;

import java.time.Duration;
import java.util.Objects;
import java.util.function.LongSupplier;

public final class TimeService {
    private final LongSupplier nanoClock;

    public TimeService() {
        this(System::nanoTime);
    }

    public TimeService(LongSupplier nanoClock) {
        this.nanoClock = Objects.requireNonNull(nanoClock, "nanoClock");
    }

    public long nowNanos() {
        return nanoClock.getAsLong();
    }

    public long add(long startNanos, Duration duration) {
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("Dauer darf nicht negativ sein.");
        }
        return startNanos + duration.toNanos();
    }

    public double elapsedSeconds(long startNanos) {
        return (nowNanos() - startNanos) / 1_000_000_000.0;
    }

    public long elapsedMilliseconds(long startNanos) {
        return (nowNanos() - startNanos) / 1_000_000;
    }

    public boolean isExpired(long deadlineNanos) {
        return nowNanos() - deadlineNanos >= 0;
    }

    public Duration remaining(long deadlineNanos) {
        return Duration.ofNanos(Math.max(0L, deadlineNanos - nowNanos()));
    }
}
