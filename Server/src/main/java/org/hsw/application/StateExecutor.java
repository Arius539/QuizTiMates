package org.hsw.application;

import java.util.concurrent.*;

public final class StateExecutor {
    private static final StateExecutor INSTANCE = new StateExecutor();
    private final ScheduledExecutorService executor;

    private StateExecutor() {
        this.executor = Executors.newSingleThreadScheduledExecutor(Thread.ofPlatform().daemon(true).name("Quiz-Server-Instanz").factory());
    }

    public static StateExecutor getInstance() {
        return INSTANCE;
    }

    public Future<?> submit(Runnable action) {
        return executor.submit(action);
    }

    public <T> T execute(Callable<T> action) {
        try {
            return executor.submit(action).get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Zustandsverarbeitung unterbrochen.", exception);
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof RuntimeException failure) {
                throw failure;
            }
            throw new IllegalStateException("Zustandsverarbeitung fehlgeschlagen.", exception.getCause());
        }
    }

    public ScheduledFuture<?> schedule(Runnable action, long delay, TimeUnit unit) {
        return executor.schedule(action, delay, unit);
    }
}
