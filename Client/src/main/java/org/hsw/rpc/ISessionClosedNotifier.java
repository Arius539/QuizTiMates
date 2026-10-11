package org.hsw.rpc;

import org.hsw.rpc.session.ClientSessionCloseResult;

@FunctionalInterface
public interface ISessionClosedNotifier {
    void notifySessionClosed(ClientSessionCloseResult result);
}
