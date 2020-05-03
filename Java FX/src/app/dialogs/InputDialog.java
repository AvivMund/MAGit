package app.dialogs;

import javafx.scene.control.TextInputDialog;

import java.util.Optional;

public class InputDialog {
    private String title;
    private String content;
    private TextInputDialog dialog;

    public InputDialog(String title, String content) {
        this.title = title;
        this.content = content;
        dialog = new TextInputDialog();
    }

    public Optional<String> showDialog() {
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText(content);

        return dialog.showAndWait();
    }
}
