package app.center;

import app.dialogs.InformationDialog;
import app.MainAppController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import magit.engine.Commit;

import java.util.Map;

public class CenterController {
    private MainAppController mainAppController;
    @FXML private TableView<Commit> commitTable;
    @FXML private TableColumn<Commit, String> sha1;
    @FXML private TableColumn<Commit, String> author;
    @FXML private TableColumn<Commit, String> date;
    @FXML private TableColumn<Commit, String> message;
    @FXML private Button showBtn;

    public void setMainAppController(MainAppController mainAppController) {
        this.mainAppController = mainAppController;
    }

    public void loadCommits () {
        Map<String, Commit> commits = mainAppController.getCommits();
        //Collection<Commit> sortedCommits = commits.values().;
        ObservableList<Commit> items = FXCollections.observableArrayList();
        for (Commit c : commits.values()) {
            items.add(c);
        }

        sha1.setCellValueFactory(new PropertyValueFactory<Commit, String>("sha1"));
        author.setCellValueFactory(new PropertyValueFactory<Commit, String>("author"));
        date.setCellValueFactory(new PropertyValueFactory<Commit, String>("date"));
        message.setCellValueFactory(new PropertyValueFactory<Commit, String>("message"));
        commitTable.setItems(items);
    }

    @FXML
    void showCommit(ActionEvent event) {
        if (commitTable.getSelectionModel().isEmpty()){
            InformationDialog info = new InformationDialog("ERROR: there is no commit selected");
            info.showDialog();
        } else {
            Commit selectedCommit = commitTable.getSelectionModel().getSelectedItem();
            mainAppController.showCommit(selectedCommit);
        }
    }

}

