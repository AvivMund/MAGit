package app.left.merge;

import app.dialogs.InformationDialog;
import app.left.LeftController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import magit.engine.Change;

import java.util.Map;

public class MergeConflictsController {
    private Stage stage;
    private LeftController leftController;
    private Map<String, Change> changes;
    @FXML private TextArea oursArea;
    @FXML private TextArea sourceArea;
    @FXML private TextArea theirsArea;
    @FXML private ListView<String> conflictList;
    @FXML private TextArea finalArea;
    @FXML private Label oursChangeType;
    @FXML private Label sourceExistence;
    @FXML private Label theirsChangeType;
    @FXML private Label handledConflict;
    private Change handledChange;

    private void performSelectedConflictResolution(String content) {
        boolean allDone = leftController.resolve(handledChange.path, content);

        removeHandledChangeFromListView();
        handledConflict.textProperty().setValue("");
        clearChangeTypesLabels();
        clearTextAreas();

        if (allDone) {
            leftController.mergePartB();
            stage.close();
        }
    }

    private void clearChangeTypesLabels() {
        oursChangeType.textProperty().setValue("");
        sourceArea.textProperty().setValue("");
        theirsChangeType.textProperty().setValue("");
    }

    private void updateConflictsList() {
        ObservableList<String> conflicts = FXCollections.observableArrayList();
        for (Change c : changes.values()) {
            conflicts.add(c.path);
        }
        conflictList.setItems(conflicts);
    }

    @FXML
    void chooseOurs(ActionEvent event) {
        if (handledChange == null) {
            errorNoConflictDialog();
            return;
        }
        String content = handledChange.ours != null ? leftController.getFileContent(handledChange.ours.getSha1()) : null;
        performSelectedConflictResolution(content);
    }

    @FXML
    void chooseFinal(ActionEvent event) {
        if (handledChange == null) {
            errorNoConflictDialog();
            return;
        }
        performSelectedConflictResolution(finalArea.textProperty().get());
    }

    @FXML
    void chooseSource(ActionEvent event) {
        if (handledChange == null) {
            errorNoConflictDialog();
            return;
        }
        String content = handledChange.sha1Common != null ? leftController.getFileContent(handledChange.sha1Common) : null;
        performSelectedConflictResolution(content);
    }

    @FXML
    void chooseTheirs(ActionEvent event) {
        if (handledChange == null) {
            errorNoConflictDialog();
            return;
        }
        String content = handledChange.theirs != null ? leftController.getFileContent(handledChange.theirs.getSha1()) : null;
        performSelectedConflictResolution(content);
    }

    private void clearTextAreas() {
        sourceArea.clear();
        theirsArea.clear();
        oursArea.clear();
        finalArea.clear();
    }

    private void removeHandledChangeFromListView() {
        String path = handledChange.path;
        ObservableList<String> list = conflictList.getItems();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).equals(path)) {
                list.remove(i);
                break;
            }
        }
        conflictList.setItems(list);
        handledChange = null;
    }

    @FXML
    void chooseConflict(ActionEvent event) {
        if (conflictList.getSelectionModel().isEmpty()) {
            errorNoConflictDialog();
            return;
        }
        String path = conflictList.getSelectionModel().getSelectedItem();
        handledConflict.textProperty().setValue(path);
        handledChange = changes.get(path);
        if (handledChange != null) {
            if (handledChange.ours != null) loadFileToTextArea(oursArea, handledChange.ours.getSha1());
            if (handledChange.sha1Common != null) loadFileToTextArea(sourceArea, handledChange.sha1Common);
            if (handledChange.theirs != null) loadFileToTextArea(theirsArea, handledChange.theirs.getSha1());

            if (handledChange.theirsType != null) {
                theirsChangeType.textProperty().setValue(String.valueOf(handledChange.theirsType));
            }
            if (handledChange.oursType != null) {
                oursChangeType.textProperty().setValue(String.valueOf(handledChange.oursType));
            }
            if (handledChange.sha1Common != null) {
                sourceExistence.textProperty().setValue("EXIST");
            } else {
                sourceExistence.textProperty().setValue("NOT EXIST");
            }

        }
    }

    private void loadFileToTextArea(TextArea textArea, String sha1) {
        String content = leftController.getFileContent(sha1);
        textArea.textProperty().setValue(content);
    }

    private void errorNoConflictDialog() {
        InformationDialog info = new InformationDialog("ERROR: no conflict selected!");
        info.showDialog();
    }


    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public void setConflictList(Map<String, Change> changes) {
        this.changes = changes;
        updateConflictsList();
    }

    public void setLeftController(LeftController leftController) {
        this.leftController = leftController;
    }
}