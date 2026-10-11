package org.hsw.rpc.communication;

import org.hsw.exceptions.RemoteActionException;
import org.hsw.protocol.io.ProtocolException;
import org.hsw.protocol.io.TransportException;
import org.hsw.protocol.rpc.RpcChannel;
import org.hsw.protocol.rpc.RpcRequest;
import org.hsw.protocol.rpc.RpcResponse;
import org.hsw.rpc.session.ClientSessionLifecycle;

import java.io.IOException;
import java.net.Socket;
import java.util.Objects;
import java.util.function.Consumer;

public final class ClientConnection implements AutoCloseable {
    private final Socket socket;
    private final RpcChannel channel;
    private final Runnable onClose;
    private final Consumer<Throwable> onFailure;
    private final ClientSessionLifecycle lifecycle;
    private final Object dialogLock = new Object();

    public ClientConnection(Socket socket, RpcChannel channel, Runnable onClose, Consumer<Throwable> onFailure) throws IOException {
        this(socket, channel, onClose, onFailure, new ClientSessionLifecycle());
    }

    public ClientConnection(Socket socket, RpcChannel channel, Runnable onClose, Consumer<Throwable> onFailure, ClientSessionLifecycle lifecycle) throws IOException {
        this.socket = Objects.requireNonNull(socket);
        this.channel = Objects.requireNonNull(channel);
        this.onClose = Objects.requireNonNull(onClose);
        this.onFailure = Objects.requireNonNull(onFailure);
        this.lifecycle = Objects.requireNonNull(lifecycle);
    }

    public RpcResponse request(RpcRequest request) {
        synchronized (dialogLock) {
            lifecycle.requireOpen();
            try {
                RpcResponse response = channel.invoke(request);
                if (response.isError()) {
                    throw new RemoteActionException(response.errorCode(), response.errorMessage());
                }
                if (!response.isSuccess()) {
                    throw new ProtocolException("Antwortstatus muss OK oder ERROR sein.");
                }
                return response;
            } catch (RemoteActionException exception) {
                throw exception;
            } catch (RuntimeException | Error exception) {
                if (lifecycle.isOpen()) abort(exception);
                throw exception;
            }
        }
    }

    public void abort(Throwable cause) {
        Objects.requireNonNull(cause);
        try {
            if (lifecycle.isOpen()) onFailure.accept(cause);
        } catch (RuntimeException | Error failure) {
            if (failure != cause) {
                cause.addSuppressed(failure);
            }
        }
        closeAfterFailure(cause);
    }

    @Override
    public void close() {
        Throwable failure = null;
        try {
            socket.close();
        } catch (IOException exception) {
            failure = new TransportException("Socket konnte nicht geschlossen werden.", exception);
        } catch (RuntimeException | Error exception) {
            failure = exception;
        }
        try {
            onClose.run();
        } catch (RuntimeException | Error exception) {
            if (failure == null) {
                failure = exception;
            } else if (failure != exception) {
                failure.addSuppressed(exception);
            }
        }
        if (failure instanceof RuntimeException exception) {
            throw exception;
        }
        if (failure instanceof Error error) {
            throw error;
        }
    }

    private void closeAfterFailure(Throwable cause) {
        try {
            close();
        } catch (RuntimeException | Error failure) {
            if (failure != cause) {
                cause.addSuppressed(failure);
            }
        }
    }
}
