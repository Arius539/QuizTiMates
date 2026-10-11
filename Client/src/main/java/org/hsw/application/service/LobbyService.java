package org.hsw.application.service;

import org.hsw.application.error.RemoteRejectionMapper;
import org.hsw.application.error.ServiceErrorKind;
import org.hsw.application.error.ServiceException;
import org.hsw.application.error.StandardErrorMessages;
import org.hsw.application.session.ClientContext;
import org.hsw.config.BusinessLimits;
import org.hsw.contract.ILobby;
import org.hsw.contract.IMatch;
import org.hsw.contract.model.IPlayerInfo;
import org.hsw.contract.model.Topic;
import org.hsw.exceptions.RemoteActionException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Component
public class LobbyService {
    private ILobby lobby;
    private ClientContext clientContext;

    public void setSession(ClientContext clientContext, ILobby lobby) {
        this.clientContext = clientContext;
        this.lobby = lobby;
    }

    public void startMatch(int roundCount, Set<Topic> topics) throws ServiceException {
        try {
            requireSession();
            if (roundCount < 1 || roundCount > BusinessLimits.MAX_ROUNDS) {
                throw new ServiceException(ServiceErrorKind.VALIDATION, "Die Rundenanzahl muss zwischen 1 und " + BusinessLimits.MAX_ROUNDS + " liegen.");
            }
            if (topics == null || topics.isEmpty() || topics.stream().anyMatch(Objects::isNull)) {
                throw new ServiceException(ServiceErrorKind.VALIDATION, "Bitte wähle mindestens ein gültiges Thema aus.");
            }
            if (!isCurrentPlayerHost()) {
                throw new ServiceException(ServiceErrorKind.INVALID_STATE, "Du bist kein Host.");
            }
            if (lobby.getPlayers().size() < BusinessLimits.MIN_PARTICIPANTS_TO_START) {
                throw new ServiceException(ServiceErrorKind.INVALID_STATE, "Es sind nicht genug Spieler in der Lobby.");
            }
            if (lobby.isMatchRunning()) {
                throw new ServiceException(ServiceErrorKind.INVALID_STATE, "Es läuft bereits ein Match.");
            }
            lobby.startMatch(clientContext.player(), roundCount, topics);
        } catch (ServiceException failure) {
            throw failure;
        } catch (RemoteActionException rejection) {
            throw RemoteRejectionMapper.serviceExceptionFromRemote(rejection);
        } catch (RuntimeException | Error failure) {
            throw new ServiceException(ServiceErrorKind.SERVER_ERROR, StandardErrorMessages.TRY_AGAIN_LATER, failure);
        }
    }

    public void leaveLobby() throws ServiceException {
        try {
            requireSession();
            if (!isPlayerInLobby()) {
                throw new ServiceException(ServiceErrorKind.INVALID_STATE, "Du befindest dich nicht in dieser Lobby.");
            }
            lobby.leave(clientContext.player());
        } catch (ServiceException failure) {
            throw failure;
        } catch (RemoteActionException rejection) {
            throw RemoteRejectionMapper.serviceExceptionFromRemote(rejection);
        } catch (RuntimeException | Error failure) {
            throw new ServiceException(ServiceErrorKind.SERVER_ERROR, StandardErrorMessages.TRY_AGAIN_LATER, failure);
        }
    }

    public List<String> getLobbyPlayers() throws ServiceException {
        try {
            requireSession();
            List<IPlayerInfo> players = lobby.getPlayers();
            UUID hostId = lobby.getHost().getId();
            UUID playerId = clientContext.player().getId();
            return players.stream().map(player -> {
                String baseName = player.getName();
                String youTag = player.getId().equals(playerId) ? " (Du)" : "";
                String hostTag = player.getId().equals(hostId) ? " (Host)" : "";
                return baseName + youTag + hostTag;
            }).toList();
        } catch (ServiceException failure) {
            throw failure;
        } catch (RemoteActionException rejection) {
            throw RemoteRejectionMapper.serviceExceptionFromRemote(rejection);
        } catch (RuntimeException | Error failure) {
            throw new ServiceException(ServiceErrorKind.SERVER_ERROR, StandardErrorMessages.TRY_AGAIN_LATER, failure);
        }
    }

    public IPlayerInfo getHost() throws ServiceException {
        try {
            requireSession();
            return lobby.getHost();
        } catch (ServiceException failure) {
            throw failure;
        } catch (RemoteActionException rejection) {
            throw RemoteRejectionMapper.serviceExceptionFromRemote(rejection);
        } catch (RuntimeException | Error failure) {
            throw new ServiceException(ServiceErrorKind.SERVER_ERROR, StandardErrorMessages.TRY_AGAIN_LATER, failure);
        }
    }

    public String getLobbyCode() throws ServiceException {
        try {
            requireSession();
            return lobby.getCode();
        } catch (ServiceException failure) {
            throw failure;
        } catch (RemoteActionException rejection) {
            throw RemoteRejectionMapper.serviceExceptionFromRemote(rejection);
        } catch (RuntimeException | Error failure) {
            throw new ServiceException(ServiceErrorKind.SERVER_ERROR, StandardErrorMessages.TRY_AGAIN_LATER, failure);
        }
    }

    public boolean isPlayerInLobby() throws ServiceException {
        try {
            requireSession();
            return lobby.getPlayers().stream().anyMatch(player -> player.getId().equals(clientContext.player().getId()));
        } catch (ServiceException failure) {
            throw failure;
        } catch (RemoteActionException rejection) {
            throw RemoteRejectionMapper.serviceExceptionFromRemote(rejection);
        } catch (RuntimeException | Error failure) {
            throw new ServiceException(ServiceErrorKind.SERVER_ERROR, StandardErrorMessages.TRY_AGAIN_LATER, failure);
        }
    }

    public boolean isCurrentPlayerHost() throws ServiceException {
        try {
            requireSession();
            return getHost().getId().equals(clientContext.player().getId());
        } catch (ServiceException failure) {
            throw failure;
        } catch (RemoteActionException rejection) {
            throw RemoteRejectionMapper.serviceExceptionFromRemote(rejection);
        } catch (RuntimeException | Error failure) {
            throw new ServiceException(ServiceErrorKind.SERVER_ERROR, StandardErrorMessages.TRY_AGAIN_LATER, failure);
        }
    }

    public Optional<IMatch> getRunningMatch() throws ServiceException {
        try {
            requireSession();
            return lobby.getRunningMatch();
        } catch (ServiceException failure) {
            throw failure;
        } catch (RemoteActionException rejection) {
            throw RemoteRejectionMapper.serviceExceptionFromRemote(rejection);
        } catch (RuntimeException | Error failure) {
            throw new ServiceException(ServiceErrorKind.SERVER_ERROR, StandardErrorMessages.TRY_AGAIN_LATER, failure);
        }
    }

    public IMatch getRunningMatch(UUID matchId) throws ServiceException {
        return getRunningMatch().filter(runningMatch -> runningMatch.getId().equals(matchId)).orElseThrow(() -> new ServiceException(ServiceErrorKind.INVALID_STATE, "Das angekündigte Match ist nicht mehr verfügbar."));
    }

    public void resetSession() {
        lobby = null;
        clientContext = null;
    }

    private void requireSession() throws ServiceException {
        if (clientContext == null) {
            throw new ServiceException(ServiceErrorKind.INVALID_STATE, StandardErrorMessages.SIGN_IN_AGAIN);
        }
        if (lobby == null) {
            throw new ServiceException(ServiceErrorKind.INVALID_STATE, "Du befindest dich nicht in einer Lobby.");
        }
    }
}
