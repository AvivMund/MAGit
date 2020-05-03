package app.right;


import app.MainAppController;
import app.bottom.BottomController;
import app.dialogs.InformationDialog;
import app.dialogs.InputDialog;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import magit.engine.ChangeType;
import magit.engine.Item;
import magit.engine.RepoManager;
import magit.engine.Utils;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RightController {
    private Stage primaryStage;
    private MainAppController mainAppController;

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void setMainAppController(MainAppController mainAppController) {
        this.mainAppController = mainAppController;
    }

    @FXML
    void showWCStatus(ActionEvent event) {
        String currRepoName = mainAppController.getRepoName();
        if (currRepoName.equals("None")) {
            InformationDialog infoDialog = new InformationDialog("ERROR: There is no active repository in the system");
            infoDialog.showDialog();
            return;
        }
        String currRepoLocation = mainAppController.getRepoLocation();
        Map<ChangeType, List<Item>> wcStatus = mainAppController.getWCStatus();

        StringBuilder sbStatus = new StringBuilder();
        sbStatus.append("Current repository name: " + currRepoName + "\n");
        sbStatus.append("Current repository location: " + currRepoLocation + "\n");
        sbStatus.append("Deleted files compared to the current commit:" + "\n");
        sbStatus.append(BottomController.getItems(wcStatus.get(ChangeType.DELETED)));
        sbStatus.append("Updated files compared to the current commit:" + "\n");
        sbStatus.append(BottomController.getItems(wcStatus.get(ChangeType.UPDATED)));
        sbStatus.append("Created files compared to the current commit:" + "\n");
        sbStatus.append(BottomController.getItems(wcStatus.get(ChangeType.CREATED)));

        InformationDialog infoDialog = new InformationDialog(sbStatus.toString());
        infoDialog.showDialog();
    }

    @FXML
    void makeCommit(ActionEvent event) {
        String title = "Commit";
        String content = "Please enter commit message:";
        InputDialog dialog = new InputDialog(title, content);

        Optional<String> commitMessage = dialog.showDialog();
        String message;
        if (commitMessage.isPresent()) {
            message = mainAppController.makeACommit(commitMessage.get());
            InformationDialog infoDialog = new InformationDialog(message);
            infoDialog.showDialog();
            mainAppController.updateViewInformation();
        }
    }

    @FXML
    void clone(ActionEvent event) {
        InformationDialog info1 = new InformationDialog("Please choose local repository from the directory chooser that appears next");
        info1.showDialog();
        DirectoryChooser directoryChooser1 = new DirectoryChooser();
        File localRepository = directoryChooser1.showDialog(primaryStage);
        if(localRepository == null){
            //No Directory selected
            return;
        }
        InformationDialog info2 = new InformationDialog("Please choose remote repository from the directory chooser that appears next");
        info2.showDialog();
        DirectoryChooser directoryChooser2 = new DirectoryChooser();
        File remoteRepository = directoryChooser2.showDialog(primaryStage);
        if(localRepository == null){
            //No Directory selected
            return;
        }
        String remoteRepoName = Utils.readFromTextFile(remoteRepository.getAbsolutePath() + RepoManager.REPO_NAME_PATH);
        String remoteRepoLocation = remoteRepository.getAbsolutePath();

        String localRepName = localRepository.getName();
        String localRepoLocation = localRepository.getAbsolutePath();

        String message = mainAppController.makeClone(remoteRepoName, remoteRepoLocation, localRepName, localRepoLocation);

        //show the message
        InformationDialog informationDialog = new InformationDialog(message);
        informationDialog.showDialog();
        mainAppController.updateViewInformation();
    }

    @FXML
    void fetch(ActionEvent event) {
        String message = mainAppController.fetch();

        //show the message
        InformationDialog informationDialog = new InformationDialog(message);
        informationDialog.showDialog();
    }
}