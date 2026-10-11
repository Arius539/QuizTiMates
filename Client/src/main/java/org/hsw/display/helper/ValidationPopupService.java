package org.hsw.display.helper;

import javafx.scene.control.Alert;
import org.hsw.application.error.StandardErrorMessages;
import org.springframework.stereotype.Component;

@Component
public class ValidationPopupService {
    public void showError(String title, String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title == null || title.isBlank() ? "Hinweis" : title);
        alert.setHeaderText(header == null || header.isBlank() ? null : header);
        alert.setContentText(message == null || message.isBlank() ? StandardErrorMessages.CHECK_INPUT : message);
        alert.showAndWait();
    }
}
