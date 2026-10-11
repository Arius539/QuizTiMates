package org.hsw.rpc.session;

import java.util.*;

public record ClientSessionCloseResult(UUID sessionId, Optional<Throwable> abortCause, List<Throwable> closeFailures,
                                       Set<Phase> incompletePhases) {
    public ClientSessionCloseResult {
        Objects.requireNonNull(sessionId);
        Objects.requireNonNull(abortCause);
        closeFailures = List.copyOf(closeFailures);
        incompletePhases = Set.copyOf(incompletePhases);
    }

    public boolean successful() {
        return closeFailures.isEmpty() && incompletePhases.isEmpty();
    }

    public enum Phase {TRANSPORT, CALLBACK_THREAD, SESSION_STATE}
}
