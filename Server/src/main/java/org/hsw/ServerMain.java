package org.hsw;

import org.hsw.config.ConnectionConfig;
import org.hsw.rpc.connection.ServerSessionRunner;
import org.slf4j.*;

import java.io.IOException;
import java.net.*;

public class ServerMain {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerMain.class);

    static void main(String[] args) {
        try (ServerSocket serverSocket = new ServerSocket(ConnectionConfig.SERVER_PORT)) {
            LOGGER.info("Server started on port {}", ConnectionConfig.SERVER_PORT);
            LOGGER.info("Server IP-Address is: {}", InetAddress.getLocalHost().getHostAddress());
            do {
                final Socket socket = serverSocket.accept();
                socket.setSoTimeout(ConnectionConfig.SOCKET_TIMEOUT_TIME);
                LOGGER.info("Accepted connection from {}", socket.getRemoteSocketAddress());
                initializeClientServerRunner(socket);
            } while (true);
        } catch (IOException e) {
            LOGGER.error("Could not listen on port: {}", ConnectionConfig.SERVER_PORT, e);
        }
    }

    private static void initializeClientServerRunner(Socket socket) {
        ServerSessionRunner runner = null;
        try {
            runner = new ServerSessionRunner(socket);
            runner.start();
        } catch (IOException | RuntimeException exception) {
            if (runner != null) {
                runner.abort(exception);
            } else {
                try {
                    socket.close();
                } catch (IOException | RuntimeException closeFailure) {
                    LOGGER.error("Socket konnte nicht geschlossen werden: {}", socket, closeFailure);
                    if (closeFailure != exception) exception.addSuppressed(closeFailure);
                }
            }
            LOGGER.error("Angenommene Verbindung konnte nicht gestartet werden", exception);
        }
    }
}
