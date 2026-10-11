package org.hsw.application.service;

import org.hsw.application.error.RemoteRejectionMapper;
import org.hsw.application.error.ServiceErrorKind;
import org.hsw.application.error.ServiceException;
import org.hsw.application.error.StandardErrorMessages;
import org.hsw.application.session.ClientContext;
import org.hsw.contract.ILobby;
import org.hsw.exceptions.RemoteActionException;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class HomeService {
    private ClientContext clientContext;

    public void setSession(ClientContext clientContext) {
        this.clientContext = Objects.requireNonNull(clientContext, "clientContext");
    }

    public ILobby createLobby() throws ServiceException {
        try {
            requireSession();
            return clientContext.home().createLobby(clientContext.player());
        } catch (ServiceException failure) {
            throw failure;
        } catch (RemoteActionException rejection) {
            throw RemoteRejectionMapper.serviceExceptionFromRemote(rejection);
        } catch (RuntimeException | Error failure) {
            throw new ServiceException(ServiceErrorKind.SERVER_ERROR, StandardErrorMessages.TRY_AGAIN_LATER, failure);
        }
    }

    public ILobby joinLobby(String lobbyCode) throws ServiceException {

        try {
            requireSession();
            if (lobbyCode == null || lobbyCode.isBlank()) {
                throw new ServiceException(ServiceErrorKind.VALIDATION, "Bitte gib einen Lobbycode ein.");
            }
            lobbyCode = lobbyCode.trim().toUpperCase();
            if (!clientContext.home().lobbyExists(lobbyCode)) {
                throw new ServiceException(ServiceErrorKind.VALIDATION, "Der angegebene Lobbycode existiert nicht.");
            }
            return clientContext.home().joinLobby(lobbyCode, clientContext.player());
        } catch (ServiceException failure) {
            throw failure;
        } catch (RemoteActionException rejection) {
            throw RemoteRejectionMapper.serviceExceptionFromRemote(rejection);
        } catch (RuntimeException | Error failure) {
            throw new ServiceException(ServiceErrorKind.SERVER_ERROR, StandardErrorMessages.TRY_AGAIN_LATER, failure);
        }
    }

    public void resetSession() {
        clientContext = null;
    }

    private void requireSession() throws ServiceException {
        if (clientContext == null) {
            throw new ServiceException(ServiceErrorKind.INVALID_STATE, StandardErrorMessages.SIGN_IN_AGAIN);
        }
    }
}
