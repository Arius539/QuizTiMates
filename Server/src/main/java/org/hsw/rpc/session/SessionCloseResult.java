package org.hsw.rpc.session;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record SessionCloseResult(Optional<Throwable> abortCause, List<Throwable> closeFailures,
                                 Set<Phase> incompletePhases) {
    public SessionCloseResult {
        Objects.requireNonNull(abortCause);
        closeFailures = List.copyOf(closeFailures);
        incompletePhases = Set.copyOf(incompletePhases);
    }

    public boolean successful() {
        return closeFailures.isEmpty() && incompletePhases.isEmpty();
    }

    public enum Phase {TRANSPORT, DOMAIN, RUNNER, CALLBACK}
}
