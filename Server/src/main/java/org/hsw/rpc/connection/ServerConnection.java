package org.hsw.rpc.connection;

import org.hsw.exceptions.RemoteActionException;
import org.hsw.protocol.io.*;
import org.hsw.protocol.message.MethodId;
import org.hsw.protocol.rpc.*;

import java.io.IOException;
import java.net.Socket;
import java.util.*;

public final class ServerConnection implements AutoCloseable {
    private final Socket socket;
    private final RpcChannel channel;
    private final Runnable onClose;
    private final Object dialogLock = new Object();
    private final Object lifecycleLock = new Object();
    private boolean closed;

    public ServerConnection(Socket socket, RpcChannel channel, Runnable onClose) throws IOException {
        this.socket = Objects.requireNonNull(socket);
        this.channel = Objects.requireNonNull(channel);
        this.onClose = Objects.requireNonNull(onClose);
        this.closed = false;
    }

    public RpcResponse request(RpcRequest request) {
        synchronized (dialogLock) {
            if (isClosed()) {
                throw new IllegalStateException("Verbindung ist geschlossen.");
            }
            try {
                RpcResponse response = channel.invoke(request);
                if (response.isError()) {
                    throw new RemoteActionException(response.errorCode(), response.errorMessage());
                }
                return response;
            } catch (RpcException | ProtocolException | TransportException | IllegalArgumentException exception) {
                closeAfterFailure(exception);
                throw exception;
            }
        }
    }

    public Optional<RpcResponse> disconnect() {
        synchronized (dialogLock) {
            if (isClosed()) {
                return Optional.empty();
            }
            RuntimeException failure = null;
            try {
                return Optional.of(request(new RpcRequest(MethodId.DISCONNECT, List.of())));
            } catch (RuntimeException exception) {
                failure = exception;
                throw exception;
            } finally {
                if (failure == null) {
                    close();
                } else {
                    closeAfterFailure(failure);
                }
            }
        }
    }

    @Override
    public void close() {
        synchronized (lifecycleLock) {
            if (closed) {
                return;
            }
            closed = true;
        }
        RuntimeException failure = null;
        try {
            socket.close();
        } catch (IOException exception) {
            failure = new TransportException("Socket konnte nicht geschlossen werden.", exception);
        } catch (RuntimeException exception) {
            failure = exception;
        }
        try {
            onClose.run();
        } catch (RuntimeException exception) {
            if (failure == null) {
                failure = exception;
            } else if (failure != exception) {
                failure.addSuppressed(exception);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    public boolean isClosed() {
        synchronized (lifecycleLock) {
            return closed;
        }
    }

    private void closeAfterFailure(RuntimeException failure) {
        try {
            close();
        } catch (RuntimeException closeFailure) {
            if (closeFailure != failure) {
                failure.addSuppressed(closeFailure);
            }
        }
    }
}
