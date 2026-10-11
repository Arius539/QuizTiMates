package org.hsw.rpc.connection;

import org.hsw.protocol.io.*;
import org.hsw.protocol.rpc.*;
import org.hsw.rpc.session.SessionLifecycle;
import org.slf4j.*;

import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.*;

public final class ServerCallbackSender implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerCallbackSender.class);
    private final ServerConnection connection;
    private final SessionLifecycle lifecycle;
    private final Consumer<Throwable> onFailure;
    private final ExecutorService callbackExecutor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(64), Thread.ofPlatform().daemon(true).name("player-callback-sender").factory(), new ThreadPoolExecutor.AbortPolicy());

    public ServerCallbackSender(ServerConnection connection, SessionLifecycle lifecycle, Consumer<Throwable> onFailure) {
        this.connection = Objects.requireNonNull(connection);
        this.lifecycle = Objects.requireNonNull(lifecycle);
        this.onFailure = Objects.requireNonNull(onFailure);
    }

    public void enqueue(Supplier<RpcRequest> requestFactory) {
        enqueue(requestFactory, () -> {
        });
    }

    public void enqueue(Supplier<RpcRequest> requestFactory, Runnable beforeEnqueue) {
        if (!lifecycle.isOpen()) return;
        RpcRequest request = Objects.requireNonNull(requestFactory.get());
        Objects.requireNonNull(beforeEnqueue);
        try {
            lifecycle.tryRunWhileOpen(() -> {
                beforeEnqueue.run();
                callbackExecutor.execute(() -> deliver(request));
            });
        } catch (RejectedExecutionException exception) {
            if (lifecycle.isOpen()) {
                LOGGER.warn("Callbackqueue voll: method={}", request.method());
                onFailure.accept(exception);
            }
        }
    }

    @Override
    public void close() {
        Throwable failure = null;
        try {
            callbackExecutor.shutdownNow();
        } catch (RuntimeException | Error exception) {
            failure = exception;
        }
        try {
            connection.close();
        } catch (RuntimeException | Error exception) {
            if (failure == null) failure = exception;
            else if (failure != exception) failure.addSuppressed(exception);
        }
        if (failure instanceof RuntimeException exception) throw exception;
        if (failure instanceof Error error) throw error;
    }

    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return callbackExecutor.awaitTermination(timeout, unit);
    }

    public boolean isTerminated() {
        return callbackExecutor.isTerminated();
    }

    private void deliver(RpcRequest request) {
        if (!lifecycle.isOpen()) return;
        try {
            RpcResponse response = connection.request(request);
            if (!response.isSuccess()) {
                throw new ProtocolException("Erfolgreiche Callbackbestätigung erwartet.");
            }
            new ContentReader(response.content()).requireEnd();
            LOGGER.info("Callback erfolgreich bestätigt: method={}", request.method());
        } catch (RuntimeException exception) {
            if (lifecycle.isOpen()) {
                LOGGER.warn("Callback fehlgeschlagen: method={}, cause={}, ioCause={}", request.method(), exception.getClass().getSimpleName(), exception.getCause() == null ? "NONE" : exception.getCause().getClass().getSimpleName());
                onFailure.accept(exception);
            }
        }
    }
}
