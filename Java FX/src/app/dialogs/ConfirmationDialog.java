package app.dialogs;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

public class ConfirmationDialog {
    private Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
    private String headerText;
    private String contentText;

    public ConfirmationDialog(String headerText, String contentText) {
        this.headerText = headerText;
        this.contentText = contentText;
    }

    public Optional<ButtonType> showDialog() {
        alert.setTitle("Confirmation Dialog");
        alert.setHeaderText(headerText);
        alert.setContentText(contentText);

        return alert.showAndWait();
    }
}
