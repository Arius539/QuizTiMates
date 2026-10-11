package org.hsw.display.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import org.hsw.application.error.ServiceException;
import org.hsw.application.error.StandardErrorMessages;
import org.hsw.application.service.PlayerSetupService;
import org.hsw.application.session.ClientContext;
import org.hsw.config.ConnectionConfig;
import org.hsw.display.helper.ValidationPopupService;
import org.hsw.display.navigation.IViewController;
import org.hsw.display.navigation.ViewNavigator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@Component
public class PlayerSetupController implements IViewController {
    private final PlayerSetupService playerSetupService;
    private final ViewNavigator viewNavigator;
    private final ValidationPopupService validationPopupService;

    @FXML
    private TextField usernameInput;
    @FXML
    private TextField serverAddressInput;
    @FXML
    private TextField serverPortInput;

    @Autowired
    public PlayerSetupController(PlayerSetupService playerSetupService, ViewNavigator viewNavigator, ValidationPopupService validationPopupService) {
        this.playerSetupService = playerSetupService;
        this.viewNavigator = viewNavigator;
        this.validationPopupService = validationPopupService;
    }

    @Override
    public void initilizeView() {
        serverAddressInput.setText(ConnectionConfig.SERVER_IP_ADDRESS);
        serverPortInput.setText(String.valueOf(ConnectionConfig.SERVER_PORT));
        Platform.runLater(() -> usernameInput.requestFocus());
    }

    @FXML
    private void submit() {
        ClientContext clientContext = null;
        try {
            clientContext = playerSetupService.registerPlayer(usernameInput.getText(), serverAddressInput.getText(), serverPortInput.getText());
            viewNavigator.loadHome(clientContext);
        } catch (ServiceException failure) {
            if (clientContext != null) {
                playerSetupService.closeSession(clientContext.home());
            }
            validationPopupService.showError(StandardErrorMessages.ERROR_OCCURRED, failure.kindMessage(), failure.userMessage());
        }
    }
}
