package org.hsw.application.adapter;

import javafx.application.Platform;
import org.hsw.application.port.ISessionClosedCallbackRegistry;
import org.hsw.rpc.ISessionClosedNotifier;
import org.hsw.rpc.session.ClientSessionCloseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Consumer;

@Component
public final class SessionClosedAdapter implements ISessionClosedNotifier, ISessionClosedCallbackRegistry {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionClosedAdapter.class);
    private final SequencedMap<UUID, Consumer<ClientSessionCloseResult>> onClosedCallbacks = new LinkedHashMap<>();

    @Override
    public void notifySessionClosed(ClientSessionCloseResult result) {
        Objects.requireNonNull(result);
        Platform.runLater(() -> {
            List<Consumer<ClientSessionCloseResult>> callbacks = List.copyOf(onClosedCallbacks.reversed().values());
            for (Consumer<ClientSessionCloseResult> callback : callbacks) {
                try {
                    callback.accept(result);
                } catch (Throwable failure) {
                    LOGGER.error("GUI-Sitzungsabschluss fehlgeschlagen", failure);
                }
            }
        });
    }

    @Override
    public UUID subscribeSessionClosed(Consumer<ClientSessionCloseResult> callback) {
        Objects.requireNonNull(callback);
        UUID id = UUID.randomUUID();
        onClosedCallbacks.put(id, callback);
        return id;
    }

    @Override
    public void unsubscribeSessionClosed(UUID id) {
        onClosedCallbacks.remove(id);
    }
}
