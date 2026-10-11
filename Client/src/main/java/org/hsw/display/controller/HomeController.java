package org.hsw.display.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import org.hsw.application.error.ServiceException;
import org.hsw.application.error.StandardErrorMessages;
import org.hsw.application.port.ISessionClosedCallbackRegistry;
import org.hsw.application.service.HomeService;
import org.hsw.application.service.PlayerSetupService;
import org.hsw.application.session.ClientContext;
import org.hsw.contract.ILobby;
import org.hsw.display.helper.ValidationPopupService;
import org.hsw.display.navigation.IViewController;
import org.hsw.display.navigation.ViewNavigator;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class HomeController implements IViewController {
    private final ViewNavigator viewNavigator;
    private final HomeService homeService;
    private final PlayerSetupService playerSetupService;
    private final ValidationPopupService validationPopupService;
    private final ISessionClosedCallbackRegistry sessionclosedRegistry;
    private ClientContext clientContext;
    private UUID registerId;
    @FXML
    private Label playerName;
    @FXML
    private TextField lobbyCodeInput;

    public HomeController(ViewNavigator viewNavigator, HomeService homeService, PlayerSetupService playerSetupService, ValidationPopupService validationPopupService, ISessionClosedCallbackRegistry sessionclosedRegistry) {
        this.viewNavigator = viewNavigator;
        this.homeService = homeService;
        this.playerSetupService = playerSetupService;
        this.validationPopupService = validationPopupService;
        this.sessionclosedRegistry = sessionclosedRegistry;
    }

    public void setSession(ClientContext clientContext) {
        this.clientContext = Objects.requireNonNull(clientContext, "clientContext");
    }

    @Override
    public void initilizeView() {
        homeService.setSession(clientContext);
        playerName.setText(clientContext.player().getName());
        if (registerId != null) {
            sessionclosedRegistry.unsubscribeSessionClosed(registerId);
        }
        registerId = sessionclosedRegistry.subscribeSessionClosed(result -> {
            if (clientContext == null || !result.sessionId().equals(clientContext.sessionId())) return;
            resetSession();
            toPlayerSetup();
        });

        Platform.runLater(() -> lobbyCodeInput.requestFocus());
    }

    public void createLobby() {
        if (clientContext == null) return;
        final ILobby lobby;
        try {
            lobby = homeService.createLobby();
        } catch (ServiceException failure) {
            validationPopupService.showError(StandardErrorMessages.ERROR_OCCURRED, failure.kindMessage(), failure.userMessage());
            return;
        }
        openLobby(lobby);
    }

    public void joinLobby() {
        if (clientContext == null) return;
        final ILobby lobby;
        try {
            lobby = homeService.joinLobby(lobbyCodeInput.getText());
        } catch (ServiceException failure) {
            validationPopupService.showError(StandardErrorMessages.ERROR_OCCURRED, failure.kindMessage(), failure.userMessage());
            return;
        }
        openLobby(lobby);
    }

    private void openLobby(ILobby lobby) {
        try {
            viewNavigator.loadLobby(clientContext, lobby);
            resetSession();
        } catch (ServiceException failure) {
            toPlayerSetup();
            validationPopupService.showError(StandardErrorMessages.ERROR_OCCURRED, failure.kindMessage(), failure.userMessage());
        }
    }

    @Override
    public void discardView() {
        resetSession();
    }

    @FXML
    private void toPlayerSetup() {
        if (clientContext != null) {
            ClientContext departingContext = clientContext;
            resetSession();
            playerSetupService.closeSession(departingContext.home());
        }
        try {
            viewNavigator.loadPlayerSetup();
        } catch (ServiceException failure) {
            validationPopupService.showError(StandardErrorMessages.ERROR_OCCURRED, failure.kindMessage(), failure.userMessage());
        }
    }

    private void resetSession() {
        if (registerId != null) {
            sessionclosedRegistry.unsubscribeSessionClosed(registerId);
            registerId = null;
        }
        homeService.resetSession();
        clientContext = null;
    }
}
