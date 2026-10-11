package org.hsw.rpc.session;

import java.util.Objects;
import java.util.function.Supplier;

public final class SessionLifecycle {
    private final Object lock = new Object();
    private LifecycleState state = LifecycleState.OPEN;

    public LifecycleState getState() {
        return locked(() -> state);
    }

    public boolean isOpen() {
        return getState() == LifecycleState.OPEN;
    }

    public void requireOpen() {
        if (!isOpen()) throw new IllegalStateException("Sitzung wird bereits geschlossen.");
    }

    public boolean tryRunWhileOpen(Runnable action) {
        Objects.requireNonNull(action);
        return locked(() -> {
            if (state != LifecycleState.OPEN) return false;
            action.run();
            return true;
        });
    }

    public void publishWhileOpen(Runnable publication) {
        if (!tryRunWhileOpen(publication)) {
            throw new IllegalStateException("Sitzung wird bereits geschlossen.");
        }
    }

    boolean tryBeginClosing() {
        return locked(() -> {
            if (state != LifecycleState.OPEN) return false;
            state = LifecycleState.CLOSING;
            return true;
        });
    }

    void markClosed() {
        locked(() -> {
            if (state != LifecycleState.CLOSING) {
                throw new IllegalStateException("Sitzungsabschluss wurde nicht begonnen.");
            }
            state = LifecycleState.CLOSED;
            return null;
        });
    }

    <T> T locked(Supplier<T> action) {
        synchronized (lock) {
            return action.get();
        }
    }
}
