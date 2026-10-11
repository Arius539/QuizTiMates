package org.hsw.rpc.session;

import org.hsw.application.session.ClientSession;
import org.hsw.rpc.ISessionClosedNotifier;
import org.slf4j.*;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

import static org.hsw.rpc.session.ClientSessionCloseResult.Phase;

public final class ClientSessionCloseCoordinator implements AutoCloseable {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientSessionCloseCoordinator.class);
    private static final Duration TOTAL_CLOSE_TIMEOUT = Duration.ofSeconds(10);

    private final ClientSessionLifecycle lifecycle;
    private final ClientTransportSession transport;
    private final Runnable resetSessionState;
    private final ISessionClosedNotifier sessionClosedNotifier;
    private final UUID sessionId;
    private final long totalCloseTimeoutNanos;
    private final CompletableFuture<ClientSessionCloseResult> completion = new CompletableFuture<>();
    private final CompletionStage<ClientSessionCloseResult> completionView = completion.minimalCompletionStage();

    public ClientSessionCloseCoordinator(ClientSessionLifecycle lifecycle, ClientTransportSession transport, ClientSession session, ISessionClosedNotifier sessionClosedNotifier) {
        this(lifecycle, transport, session, sessionClosedNotifier, TOTAL_CLOSE_TIMEOUT);
    }

    ClientSessionCloseCoordinator(ClientSessionLifecycle lifecycle, ClientTransportSession transport, ClientSession session, ISessionClosedNotifier sessionClosedNotifier, Duration totalCloseTimeout) {
        this(lifecycle, transport, session, sessionClosedNotifier, totalCloseTimeout, Objects.requireNonNull(session)::reset);
    }

    ClientSessionCloseCoordinator(ClientSessionLifecycle lifecycle, ClientTransportSession transport, ClientSession session, ISessionClosedNotifier sessionClosedNotifier, Duration totalCloseTimeout, Runnable resetSessionState) {
        this.lifecycle = Objects.requireNonNull(lifecycle);
        this.transport = Objects.requireNonNull(transport);
        this.sessionId = Objects.requireNonNull(session).id();
        this.resetSessionState = Objects.requireNonNull(resetSessionState);
        this.sessionClosedNotifier = Objects.requireNonNull(sessionClosedNotifier);
        this.totalCloseTimeoutNanos = Objects.requireNonNull(totalCloseTimeout).toNanos();
        if (totalCloseTimeoutNanos <= 0) {
            throw new IllegalArgumentException("Abschlussfrist muss positiv sein.");
        }
    }

    @Override
    public void close() {
        initiateClose(null);
    }

    public void abort(Throwable cause) {
        initiateClose(Objects.requireNonNull(cause));
    }

    public CompletionStage<ClientSessionCloseResult> completion() {
        return completionView;
    }

    private void initiateClose(Throwable cause) {
        CloseContext started = lifecycle.locked(() -> {
            if (!lifecycle.tryBeginClosing()) return null;
            return new CloseContext(transport.detachResourcesForClose(), System.nanoTime(), cause);
        });
        if (started == null) return;
        LOGGER.info("Clientsitzungsabschluss begonnen: sessionId={}, outcome={}, cause={}",
                sessionId,
                cause == null ? "CLOSE" : "ABORT",
                cause == null ? "NONE" : cause.getClass().getSimpleName());

        try {
            Thread.ofVirtual().name("Quiz-Client-Session-Close").start(() -> runCloseSequence(started, new ArrayList<>(), true));
        } catch (RuntimeException | Error failure) {
            List<Throwable> failures = new ArrayList<>();
            failures.add(failure);
            runCloseSequence(started, failures, false);
        }
    }

    private void runCloseSequence(CloseContext started, List<Throwable> failures, boolean awaitCallbackThread) {
        EnumSet<Phase> incompletePhases = EnumSet.noneOf(Phase.class);
        List<Throwable> transportFailures = started.resources().closeAndCollectFailures();
        failures.addAll(transportFailures);
        if (!transportFailures.isEmpty()) incompletePhases.add(Phase.TRANSPORT);

        Thread callbackThread = started.resources().callbackThread();
        if (callbackThread != null && callbackThread != Thread.currentThread()) {
            try {
                callbackThread.interrupt();
            } catch (Throwable failure) {
                failures.add(failure);
                incompletePhases.add(Phase.CALLBACK_THREAD);
            }
        }
        try {
            resetSessionState.run();
        } catch (Throwable failure) {
            failures.add(failure);
            incompletePhases.add(Phase.SESSION_STATE);
        }

        awaitCallbackThreadTermination(started, awaitCallbackThread, failures, incompletePhases);
        publishResult(started, failures, incompletePhases);
    }

    private void awaitCallbackThreadTermination(CloseContext started, boolean awaitCallbackThread, List<Throwable> failures, EnumSet<Phase> incompletePhases) {
        Thread callbackThread = started.resources().callbackThread();
        if (callbackThread == null) return;
        try {
            long remainingNanos = Math.max(0, totalCloseTimeoutNanos - (System.nanoTime() - started.startedAtNanos()));
            if (awaitCallbackThread && callbackThread.isAlive() && remainingNanos > 0) {
                TimeUnit.NANOSECONDS.timedJoin(callbackThread, remainingNanos);
            }
            if (callbackThread.isAlive()) {
                failures.add(new TimeoutException("Ende des Rückrufthreads innerhalb der Abschlussfrist nicht bestätigt."));
                incompletePhases.add(Phase.CALLBACK_THREAD);
            }
        } catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            failures.add(failure);
            incompletePhases.add(Phase.CALLBACK_THREAD);
        }
    }

    private void publishResult(CloseContext started, List<Throwable> failures, EnumSet<Phase> incompletePhases) {
        ClientSessionCloseResult result = new ClientSessionCloseResult(sessionId, Optional.ofNullable(started.abortCause()), failures, incompletePhases);
        if (result.successful()) lifecycle.markClosed();
        if (!result.successful()) {
            LOGGER.error("Clientsitzungsabschluss unvollständig: sessionId={}, phases={}, failures={}", sessionId, result.incompletePhases(), result.closeFailures().size());
        } else {
            LOGGER.info("Clientsitzung vollständig bereinigt: sessionId={}, outcome={}, durationMs={}", sessionId, started.abortCause() == null ? "CLOSE" : "ABORT", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started.startedAtNanos()));
        }
        completion.complete(result);
        try {
            sessionClosedNotifier.notifySessionClosed(result);
        } catch (Throwable failure) {
            LOGGER.error("Benachrichtigung über Clientsitzungsabschluss fehlgeschlagen", failure);
        }
    }

    private record CloseContext(ClientTransportSession.DetachedResources resources, long startedAtNanos,
                                Throwable abortCause) {
    }
}
