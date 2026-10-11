package org.hsw.application.service;

import org.hsw.application.error.*;
import org.hsw.application.session.ClientContext;
import org.hsw.config.ConnectionConfig;
import org.hsw.contract.IHome;
import org.hsw.contract.model.IPlayerInfo;
import org.hsw.exceptions.RemoteActionException;
import org.hsw.rpc.ClientConnector;
import org.slf4j.*;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class PlayerSetupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(PlayerSetupService.class);
    private final ClientConnector connector;

    public PlayerSetupService(ClientConnector connector) {
        this.connector = Objects.requireNonNull(connector);
    }

    public ClientContext registerPlayer(String playerName) throws ServiceException {
        return registerPlayer(playerName, ConnectionConfig.SERVER_IP_ADDRESS, String.valueOf(ConnectionConfig.SERVER_PORT));
    }

    public ClientContext registerPlayer(String playerName, String serverAddress, String serverPortText) throws ServiceException {
        String name = playerName == null ? "" : playerName.trim();
        if (name.isEmpty() || name.length() > 256) {
            throw new ServiceException(ServiceErrorKind.VALIDATION, "Der Name muss 1 bis 256 Zeichen enthalten.");
        }
        String address = serverAddress == null ? "" : serverAddress.trim();
        if (address.isEmpty()) {
            throw new ServiceException(ServiceErrorKind.VALIDATION, "Die Serveradresse darf nicht leer sein.");
        }
        int serverPort = parseServerPort(serverPortText);

        UUID attemptId = UUID.randomUUID();
        IHome home = null;
        try {
            home = connector.connect(attemptId, address, serverPort);
            IPlayerInfo player = home.createPlayer(name);
            return new ClientContext(attemptId, home, player);
        } catch (ServiceException failure) {
            closeSession(home);
            throw failure;
        } catch (RemoteActionException rejection) {
            closeSession(home);
            throw RemoteRejectionMapper.serviceExceptionFromRemote(rejection);
        } catch (RuntimeException | Error failure) {
            closeSession(home);
            throw new ServiceException(ServiceErrorKind.SERVER_ERROR, StandardErrorMessages.TRY_AGAIN_LATER, failure);
        }
    }

    public void closeSession(IHome home) {
        if (!(home instanceof AutoCloseable closeable)) return;
        try {
            closeable.close();
        } catch (Exception | Error failure) {
            LOGGER.error("Clientverbindung konnte nicht geschlossen werden", failure);
        }
    }

    private static int parseServerPort(String serverPortText) throws ServiceException {
        if (serverPortText == null || serverPortText.isBlank()) {
            throw new ServiceException(ServiceErrorKind.VALIDATION, "Der Serverport muss zwischen 1 und 65535 liegen.");
        }
        final int serverPort;
        try {
            serverPort = Integer.parseInt(serverPortText.trim());
        } catch (NumberFormatException failure) {
            throw new ServiceException(ServiceErrorKind.VALIDATION, "Der Serverport muss zwischen 1 und 65535 liegen.", failure);
        }
        if (serverPort < 1 || serverPort > 65535) {
            throw new ServiceException(ServiceErrorKind.VALIDATION, "Der Serverport muss zwischen 1 und 65535 liegen.");
        }
        return serverPort;
    }
}
