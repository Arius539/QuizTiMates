package org.hsw.rpc.session;

import org.hsw.application.StateExecutor;
import org.hsw.rpc.connection.ServerCallbackSender;
import org.slf4j.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

import static org.hsw.rpc.session.SessionCloseResult.Phase;

public final class SessionCloseCoordinator implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionCloseCoordinator.class);
    private static final Duration DISCONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration TOTAL_CLOSE_TIMEOUT = Duration.ofSeconds(10);

    private final SessionLifecycle lifecycle;
    private final ServerTransportSession transport;
    private final Consumer<Runnable> domainCleanupSubmitter;
    private final Runnable domainCleanupAction;
    private final long disconnectTimeoutNanos;
    private final long totalCloseTimeoutNanos;
    private final CompletableFuture<Void> transportClosePermission = new CompletableFuture<>();
    private final CompletableFuture<Void> domainCleanupCompletion = new CompletableFuture<>();
    private final CompletableFuture<Void> disconnectAckReady = new CompletableFuture<>();
    private final CompletableFuture<SessionCloseResult> completion = new CompletableFuture<>();
    private final CompletionStage<Void> disconnectAckReadyView = disconnectAckReady.minimalCompletionStage();
    private final CompletionStage<SessionCloseResult> completionView = completion.minimalCompletionStage();

    private CloseContext closeContext;
    private Throwable abortCause;
    private boolean closeResultFinalized;

    public SessionCloseCoordinator(SessionLifecycle lifecycle, ServerTransportSession transport, StateExecutor stateExecutor, Runnable domainCleanupAction) {
        this(lifecycle, transport, Objects.requireNonNull(stateExecutor)::submit, domainCleanupAction, DISCONNECT_TIMEOUT, TOTAL_CLOSE_TIMEOUT);
    }

    SessionCloseCoordinator(SessionLifecycle lifecycle, ServerTransportSession transport, Consumer<Runnable> domainCleanupSubmitter, Runnable domainCleanupAction, Duration disconnectTimeout, Duration totalCloseTimeout) {
        this.lifecycle = Objects.requireNonNull(lifecycle);
        this.transport = Objects.requireNonNull(transport);
        this.domainCleanupSubmitter = Objects.requireNonNull(domainCleanupSubmitter);
        this.domainCleanupAction = Objects.requireNonNull(domainCleanupAction);
        this.disconnectTimeoutNanos = Objects.requireNonNull(disconnectTimeout).toNanos();
        this.totalCloseTimeoutNanos = Objects.requireNonNull(totalCloseTimeout).toNanos();
        if (disconnectTimeoutNanos <= 0 || totalCloseTimeoutNanos <= disconnectTimeoutNanos) {
            throw new IllegalArgumentException("Abschlussfrist muss größer als die positive Disconnectfrist sein.");
        }
    }

    public CompletionStage<Void> prepareDisconnect() {
        initiateClose(true, null);
        return disconnectAckReadyView;
    }

    public void disconnectAckWritten() {
        if (lifecycle.locked(() -> closeContext == null || !closeContext.graceful())) {
            throw new IllegalStateException("Kein regulärer Disconnect vorbereitet.");
        }
        transportClosePermission.complete(null);
    }

    @Override
    public void close() {
        initiateClose(false, null);
    }

    public void abort(Throwable cause) {
        initiateClose(false, Objects.requireNonNull(cause));
    }

    public CompletionStage<SessionCloseResult> completion() {
        return completionView;
    }

    private void initiateClose(boolean graceful, Throwable cause) {
        CloseContext started = lifecycle.locked(() -> {
            if (closeResultFinalized) return null;
            if (cause != null && abortCause == null) abortCause = cause;
            if (!lifecycle.tryBeginClosing()) return null;
            closeContext = new CloseContext(transport.detachResourcesForClose(), System.nanoTime(), graceful);
            return closeContext;
        });

        if (!graceful) {
            transportClosePermission.complete(null);
            disconnectAckReady.completeExceptionally(new IllegalStateException("Sitzung wird geschlossen.", cause));
        }
        if (started != null) {
            LOGGER.info("Serversitzungsabschluss begonnen: graceful={}, cause={}", graceful, cause == null ? "NONE" : cause.getClass().getSimpleName());
            try {
                Thread.ofVirtual().name("Quiz-Server-Session-Close").start(() -> runCloseSequence(started));
            } catch (RuntimeException | Error failure) {
                List<Throwable> failures = new ArrayList<>(started.resources().closeAndCollectFailures());
                failures.add(failure);
                submitDomainCleanup();
                transportClosePermission.complete(null);
                disconnectAckReady.completeExceptionally(failure);
                publishResult(failures, EnumSet.allOf(Phase.class));
            }
        }
    }

    private void runCloseSequence(CloseContext started) {
        List<Throwable> failures = new ArrayList<>();
        EnumSet<Phase> incomplete = EnumSet.noneOf(Phase.class);
        if (started.graceful()) {
            submitDomainCleanup();
            awaitTransportClosePermission(started);
        }

        failures.addAll(started.resources().closeAndCollectFailures());
        if (!failures.isEmpty()) incomplete.add(Phase.TRANSPORT);
        if (!started.graceful()) submitDomainCleanup();

        awaitDomainCleanup(started, failures, incomplete);
        awaitConnectionThreadTermination(started, failures, incomplete);
        awaitCallbackSenderTermination(started, failures, incomplete);
        publishResult(failures, incomplete);
    }

    private void submitDomainCleanup() {
        try {
            domainCleanupSubmitter.accept(() -> {
                try {
                    domainCleanupAction.run();
                    domainCleanupCompletion.complete(null);
                    if (!transportClosePermission.isDone()) disconnectAckReady.complete(null);
                } catch (Throwable failure) {
                    handleDomainCleanupFailure(failure);
                }
            });
        } catch (Throwable failure) {
            handleDomainCleanupFailure(failure);
        }
    }

    private void awaitTransportClosePermission(CloseContext started) {
        try {
            transportClosePermission.get(remainingTimeoutNanos(started, disconnectTimeoutNanos), TimeUnit.NANOSECONDS);
        } catch (TimeoutException failure) {
            abort(new TimeoutException("Frist für Domainbereinigung und Disconnectbestätigung überschritten."));
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            abort(failure);
        } catch (ExecutionException failure) {
            abort(failure.getCause());
        }
    }

    private void awaitDomainCleanup(CloseContext started, List<Throwable> failures, EnumSet<Phase> incomplete) {
        try {
            domainCleanupCompletion.get(remainingTimeoutNanos(started, totalCloseTimeoutNanos), TimeUnit.NANOSECONDS);
        } catch (ExecutionException failure) {
            failures.add(failure.getCause());
            incomplete.add(Phase.DOMAIN);
        } catch (TimeoutException failure) {
            failures.add(new TimeoutException("Fachliches Cleanup innerhalb der Abschlussfrist nicht bestätigt."));
            incomplete.add(Phase.DOMAIN);
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            failures.add(failure);
            incomplete.add(Phase.DOMAIN);
        }
    }

    private void awaitConnectionThreadTermination(CloseContext started, List<Throwable> failures, EnumSet<Phase> incomplete) {
        Thread connectionThread = started.resources().connectionThread();
        if (connectionThread == null) return;
        try {
            long remainingNanos = remainingTimeoutNanos(started, totalCloseTimeoutNanos);
            if (connectionThread.isAlive() && remainingNanos > 0)
                TimeUnit.NANOSECONDS.timedJoin(connectionThread, remainingNanos);
            if (connectionThread.isAlive()) {
                incomplete.add(Phase.RUNNER);
                failures.add(new TimeoutException("Verbindungsthread innerhalb der Abschlussfrist nicht beendet."));
            }
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            failures.add(failure);
            incomplete.add(Phase.RUNNER);
        }
    }

    private void awaitCallbackSenderTermination(CloseContext started, List<Throwable> failures, EnumSet<Phase> incomplete) {
        ServerCallbackSender sender = started.resources().callbackSender();
        if (sender == null) return;
        try {
            if (!sender.awaitTermination(remainingTimeoutNanos(started, totalCloseTimeoutNanos), TimeUnit.NANOSECONDS)) {
                incomplete.add(Phase.CALLBACK);
                failures.add(new TimeoutException("Callbackworker innerhalb der Abschlussfrist nicht beendet."));
            }
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            failures.add(failure);
            incomplete.add(Phase.CALLBACK);
        }
    }

    private void publishResult(List<Throwable> failures, EnumSet<Phase> incomplete) {
        SessionCloseResult result = lifecycle.locked(() -> {
            SessionCloseResult value = new SessionCloseResult(Optional.ofNullable(abortCause), failures, incomplete);
            if (value.successful()) lifecycle.markClosed();
            closeResultFinalized = true;
            return value;
        });
        if (!result.successful()) {
            LOGGER.error("Sitzungsabschluss unvollständig: phases={}, failures={}", result.incompletePhases(), result.closeFailures().size());
        }
        completion.complete(result);
    }

    private void handleDomainCleanupFailure(Throwable failure) {
        LOGGER.error("Fachliche Sitzungsbereinigung fehlgeschlagen", failure);
        domainCleanupCompletion.completeExceptionally(failure);
        disconnectAckReady.completeExceptionally(failure);
        transportClosePermission.complete(null);
    }

    private long remainingTimeoutNanos(CloseContext started, long timeoutNanos) {
        return Math.max(0, timeoutNanos - (System.nanoTime() - started.startedAt()));
    }

    private record CloseContext(ServerTransportSession.DetachedResources resources, long startedAt, boolean graceful) {
    }
}
