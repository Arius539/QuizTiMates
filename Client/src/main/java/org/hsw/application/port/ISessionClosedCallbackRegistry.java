package org.hsw.application.port;

import org.hsw.rpc.session.ClientSessionCloseResult;

import java.util.UUID;
import java.util.function.Consumer;

public interface ISessionClosedCallbackRegistry {

    UUID subscribeSessionClosed(Consumer<ClientSessionCloseResult> callback);

    void unsubscribeSessionClosed(UUID id);
}
