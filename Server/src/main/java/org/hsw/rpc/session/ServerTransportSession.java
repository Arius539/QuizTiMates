package org.hsw.rpc.session;

import org.hsw.rpc.connection.ServerCallbackSender;

import java.net.Socket;
import java.util.*;

public final class ServerTransportSession {
    private final SessionLifecycle lifecycle;
    private Socket mainSocket;
    private Socket callbackSocket;
    private ServerCallbackSender callbackSender;
    private Thread connectionThread;

    public ServerTransportSession(SessionLifecycle lifecycle) {
        this.lifecycle = Objects.requireNonNull(lifecycle);
    }

    public void attachMainSocket(Socket mainSocket) {
        Objects.requireNonNull(mainSocket);
        String rejectionReason = lifecycle.locked(() -> {
            if (!lifecycle.isOpen()) return "Sitzung wird bereits geschlossen.";
            if (this.mainSocket != null && this.mainSocket != mainSocket) return "Hauptsocket bereits zugeordnet.";
            this.mainSocket = mainSocket;
            return null;
        });
        closeAndRejectIfNecessary(mainSocket, rejectionReason);
    }

    public void attachCallbackSocket(Socket callbackSocket) {
        Objects.requireNonNull(callbackSocket);
        String rejectionReason = lifecycle.locked(() -> {
            if (!lifecycle.isOpen()) return "Sitzung wird bereits geschlossen.";
            if (this.callbackSocket != null && this.callbackSocket != callbackSocket)
                return "Callbacksocket bereits zugeordnet.";
            this.callbackSocket = callbackSocket;
            return null;
        });
        closeAndRejectIfNecessary(callbackSocket, rejectionReason);
    }

    public void attachCallbackSender(ServerCallbackSender callbackSender) {
        Objects.requireNonNull(callbackSender);
        String rejectionReason = lifecycle.locked(() -> {
            if (!lifecycle.isOpen()) return "Sitzung wird bereits geschlossen.";
            if (this.callbackSender != null && this.callbackSender != callbackSender)
                return "Callbacksender bereits zugeordnet.";
            this.callbackSender = callbackSender;
            return null;
        });
        closeAndRejectIfNecessary(callbackSender, rejectionReason);
    }

    public void registerAndStartConnectionThread(Thread connectionThread) {
        Objects.requireNonNull(connectionThread);
        lifecycle.publishWhileOpen(() -> {
            if (this.connectionThread != null || connectionThread.getState() != Thread.State.NEW) {
                throw new IllegalStateException("Runner wurde bereits gestartet oder zugeordnet.");
            }
            this.connectionThread = connectionThread;
            connectionThread.start();
        });
    }

    DetachedResources detachResourcesForClose() {
        return lifecycle.locked(() -> {
            if (lifecycle.getState() != LifecycleState.CLOSING) {
                throw new IllegalStateException("Ressourcenentnahme erfordert CLOSING.");
            }
            DetachedResources resources = new DetachedResources(mainSocket, callbackSocket, callbackSender, connectionThread);
            mainSocket = null;
            callbackSocket = null;
            callbackSender = null;
            connectionThread = null;
            return resources;
        });
    }

    private void closeAndRejectIfNecessary(AutoCloseable resource, String rejectionReason) {
        if (rejectionReason == null) return;
        IllegalStateException failure = new IllegalStateException(rejectionReason);
        try {
            resource.close();
        } catch (Throwable closeFailure) {
            failure.addSuppressed(closeFailure);
        }
        throw failure;
    }

    record DetachedResources(Socket mainSocket, Socket callbackSocket, ServerCallbackSender callbackSender,
                             Thread connectionThread) {
        List<Throwable> closeAndCollectFailures() {
            List<Throwable> failures = new ArrayList<>();
            closeResource(callbackSender, failures);
            closeResource(mainSocket, failures);
            closeResource(callbackSocket, failures);
            return failures;
        }

        private static void closeResource(AutoCloseable resource, List<Throwable> failures) {
            if (resource == null) return;
            try {
                resource.close();
            } catch (Throwable failure) {
                failures.add(failure);
            }
        }
    }
}
