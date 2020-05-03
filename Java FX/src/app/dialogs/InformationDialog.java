package app.dialogs;

import javafx.scene.control.Alert;

public class InformationDialog {
    private Alert alert = new Alert(Alert.AlertType.INFORMATION);
    private String message;

    public InformationDialog(String message) {
        this.message = message;
    }

    public InformationDialog() {
        message = "Something went wrong....";
    }

    public void showDialog() {
        alert.setTitle("Information Dialog");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
