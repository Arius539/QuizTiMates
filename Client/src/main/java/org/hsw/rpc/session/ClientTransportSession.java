package org.hsw.rpc.session;

import org.hsw.protocol.io.TransportException;
import org.hsw.rpc.communication.ClientConnection;

import java.io.IOException;
import java.net.*;
import java.util.*;

public final class ClientTransportSession {
    private final ClientSessionLifecycle lifecycle;
    private Socket mainSocket;
    private ClientConnection mainConnection;
    private ServerSocket registrationListener;
    private Socket callbackSocket;
    private Thread callbackThread;

    public ClientTransportSession(ClientSessionLifecycle lifecycle) {
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

    public void attachMainConnection(ClientConnection mainConnection) {
        Objects.requireNonNull(mainConnection);
        String rejectionReason = lifecycle.locked(() -> {
            if (!lifecycle.isOpen()) return "Sitzung wird bereits geschlossen.";
            if (this.mainConnection != null && this.mainConnection != mainConnection)
                return "Hauptverbindung bereits zugeordnet.";
            this.mainConnection = mainConnection;
            return null;
        });
        closeAndRejectIfNecessary(mainConnection, rejectionReason);
    }

    public void attachRegistrationListener(ServerSocket registrationListener) {
        Objects.requireNonNull(registrationListener);
        String rejectionReason = lifecycle.locked(() -> {
            if (!lifecycle.isOpen()) return "Sitzung wird bereits geschlossen.";
            if (this.registrationListener == registrationListener) return null;
            if (this.registrationListener != null || callbackSocket != null || callbackThread != null) {
                return "Registrierung oder Rückrufkanal bereits vorhanden.";
            }
            this.registrationListener = registrationListener;
            return null;
        });
        closeAndRejectIfNecessary(registrationListener, rejectionReason);
    }

    public void attachCallbackSocket(Socket callbackSocket) {
        Objects.requireNonNull(callbackSocket);
        String rejectionReason = lifecycle.locked(() -> {
            if (!lifecycle.isOpen()) return "Sitzung wird bereits geschlossen.";
            if (this.callbackSocket != null && this.callbackSocket != callbackSocket)
                return "Rückrufsocket bereits zugeordnet.";
            this.callbackSocket = callbackSocket;
            return null;
        });
        closeAndRejectIfNecessary(callbackSocket, rejectionReason);
    }

    public void registerAndStartCallbackThread(Thread callbackThread, Runnable publication) {
        Objects.requireNonNull(callbackThread);
        Objects.requireNonNull(publication);
        lifecycle.publishWhileOpen(() -> {
            if (callbackSocket == null || this.callbackThread != null || callbackThread.getState() != Thread.State.NEW) {
                throw new IllegalStateException("Rückrufthread oder Rückrufsocket passt nicht zur Sitzung.");
            }
            this.callbackThread = callbackThread;
            publication.run();
            callbackThread.start();
        });
    }

    public void releaseRegistrationListener(ServerSocket listener) {
        Objects.requireNonNull(listener);
        try {
            listener.close();
        } catch (IOException failure) {
            throw new TransportException("Registrierungslistener konnte nicht geschlossen werden.", failure);
        }
        lifecycle.locked(() -> {
            if (registrationListener == listener) registrationListener = null;
            return null;
        });
    }

    DetachedResources detachResourcesForClose() {
        return lifecycle.locked(() -> {
            if (lifecycle.getState() != LifecycleState.CLOSING) {
                throw new IllegalStateException("Ressourcenentnahme erfordert CLOSING.");
            }
            DetachedResources resources = new DetachedResources(mainConnection, mainSocket, registrationListener, callbackSocket, callbackThread);
            mainConnection = null;
            mainSocket = null;
            registrationListener = null;
            callbackSocket = null;
            callbackThread = null;
            return resources;
        });
    }

    private void closeAndRejectIfNecessary(AutoCloseable resource, String rejectionReason) {
        if (rejectionReason == null) return;
        IllegalStateException rejection = new IllegalStateException(rejectionReason);
        try {
            resource.close();
        } catch (Throwable closeFailure) {
            rejection.addSuppressed(closeFailure);
        }
        throw rejection;
    }

    record DetachedResources(ClientConnection mainConnection, Socket mainSocket, ServerSocket registrationListener,
                             Socket callbackSocket, Thread callbackThread) {
        List<Throwable> closeAndCollectFailures() {
            List<Throwable> failures = new ArrayList<>();
            closeResource(mainConnection, failures);
            closeResource(mainSocket, failures);
            closeResource(registrationListener, failures);
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
