package org.hsw.display.helper;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ConfirmationDialogSupport {
    public boolean confirm(String title, String headerText, String contentText) {
        Alert confirmationAlert = createConfirmationAlert(title, headerText, contentText);
        return isConfirmed(confirmationAlert.showAndWait());
    }

    public boolean confirm(String title, String headerText, String contentText, String confirmText, String cancelText) {
        Alert confirmationAlert = createConfirmationAlert(title, headerText, contentText);
        ButtonType confirmButton = new ButtonType(confirmText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType(cancelText, ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmationAlert.getButtonTypes().setAll(confirmButton, cancelButton);
        return isConfirmed(confirmationAlert.showAndWait(), confirmButton);
    }

    boolean isConfirmed(Optional<ButtonType> result) {
        return result.filter(buttonType -> buttonType.getButtonData().isDefaultButton()).isPresent();
    }

    boolean isConfirmed(Optional<ButtonType> result, ButtonType confirmButton) {
        return result.filter(confirmButton::equals).isPresent();
    }

    private Alert createConfirmationAlert(String title, String headerText, String contentText) {
        Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmationAlert.setTitle(title);
        confirmationAlert.setHeaderText(headerText);
        confirmationAlert.setContentText(contentText);
        return confirmationAlert;
    }
}
