package app.left;

import app.MainAppController;
import app.dialogs.ConfirmationDialog;
import app.dialogs.InformationDialog;
import app.dialogs.InputDialog;
import app.left.merge.MergeConflictsController;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ListView;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import magit.engine.Branch;
import magit.engine.MergeData;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

public class LeftController {
    private MergeData merge = null;
    private MainAppController mainAppController;
    @FXML private ListView<Text> branchList;

    public void setMainAppController(MainAppController mainAppController) {
        this.mainAppController = mainAppController;
    }

    public void loadBranchesList() {
        Map<String, Branch> branches = mainAppController.getBranches();
        Branch head = mainAppController.getHeadBranch();
        if (head != null && branches != null) {
            ObservableList<Text> items = FXCollections.observableArrayList();
            Text headText = new Text(head.getName());
            headText.setUnderline(true);
            items.add(headText);

            for (Branch b : branches.values()) {
                if (b != head) {
                    Text t = new Text(b.getName());
                    if (b.getName().indexOf('\\') > 0) {
                        t.setFill(Color.RED);
                    }
                    items.add(t);
                }
            }
            branchList.setItems(items);
        }
    }

    @FXML
    public void createNewBranch(ActionEvent actionEvent) {
        String title = "Create new Branch";
        String content1 = "Please enter branch name:";
        InputDialog dialog1 = new InputDialog(title, content1);
        Optional<String> branch = dialog1.showDialog();
        if (!branch.isPresent()){
            return;
        }
        String branchName = branch.get();
        String content2 = "Please enter Commit SHA1:";
        InputDialog dialog2 = new InputDialog(title, content2);
        Optional<String> commitSha1 = dialog2.showDialog();
        if (!commitSha1.isPresent()){
            return;
        }
        Branch rb = mainAppController.getRBPointingCommit(commitSha1.get());

        if (rb != null) {
            int index = rb.getName().indexOf('\\') + 1;
            String rbBranchName = rb.getName().substring(index);
            ConfirmationDialog cd = new ConfirmationDialog("The commit sha1 you entered is pointed by remote branch", "Do you want to create remote tracking branch? (would be named " + rbBranchName + ")");
            Optional<ButtonType> res = cd.showDialog();
            if (res.get() == ButtonType.OK) {
                branchName = rbBranchName;
            }
        }

        String message = mainAppController.createNewBranch(branchName, commitSha1.get());
        InformationDialog info = new InformationDialog(message);
        info.showDialog();
        mainAppController.updateViewInformation();
    }

    @FXML
    void deleteSelectedBranch(ActionEvent event) {
        if (branchList.getSelectionModel().isEmpty()) {
            InformationDialog info = new InformationDialog("ERROR: there is no branch name selected");
            info.showDialog();
            return;
        }
        String selectedBranch = branchList.getSelectionModel().getSelectedItem().textProperty().get();
        if (selectedBranch.indexOf('\\') > 0) {
            InformationDialog info = new InformationDialog("ERROR: the selected branch is a remote branch and can't be deleted!");
            info.showDialog();
            return;
        }
        String message = mainAppController.deleteBranch(selectedBranch);
        InformationDialog info = new InformationDialog(message);
        info.showDialog();
        mainAppController.updateViewInformation();

    }

    @FXML
    void checkout(ActionEvent event) {
        if (branchList.getSelectionModel().isEmpty()) {
            InformationDialog info = new InformationDialog("ERROR: there is no branch name selected");
            info.showDialog();
            return;
        }
        String selectedBranch = branchList.getSelectionModel().getSelectedItem().textProperty().get();
        if (selectedBranch.indexOf('\\') > 0) {
            ConfirmationDialog cd = new ConfirmationDialog("The selected branch is a remote branch and can't be checked out!", "Do you want to create and checkout a remote tracking branch?");
            Optional<ButtonType> res = cd.showDialog();
            if (res.get() == ButtonType.CANCEL) {
                return;
            }
            Branch b = mainAppController.getBranches().get(selectedBranch);
            int index = b.getName().indexOf('\\') + 1;
            selectedBranch = b.getName().substring(index);
            mainAppController.createNewBranch(selectedBranch, b.getCommitSha1());
        }
        mainAppController.checkout(selectedBranch);
        mainAppController.updateViewInformation();

    }

    @FXML
    void merge(ActionEvent event) {
        if (branchList.getSelectionModel().isEmpty()) {
            InformationDialog info = new InformationDialog("ERROR: there is no branch name selected");
            info.showDialog();
            return;
        }
        String selectedBranch = branchList.getSelectionModel().getSelectedItem().textProperty().get();
        if (selectedBranch.indexOf('\\') > 0) {
            InformationDialog info = new InformationDialog("ERROR: the selected branch is a remote branch and can't be merged!");
            info.showDialog();
            return;
        }
        if (mainAppController.getHeadBranch().getName().equals(selectedBranch)) {
            InformationDialog info = new InformationDialog("The branch you select is the head branch, so it already merged with the head :)");
            info.showDialog();
        } else {
            performMerge(selectedBranch);
        }

    }

    private void performMerge(String selectedBranch) {
        if (!mainAppController.isWCEmpty()) {
            String message = "ERROR: WC status is 'open changes'!";
            InformationDialog info = new InformationDialog(message);
            info.showDialog();
        } else {
            // start mergePartA
            merge = mainAppController.mergePartA(selectedBranch);
            InformationDialog info = new InformationDialog(merge.getMessage());
            info.showDialog();

            //show conflicts
            if(merge.getChanges().size() > 0) {
                showAndResolveConflicts(merge);
            } else {
                mergePartB();
            }
        }
    }

    private void showAndResolveConflicts(MergeData merge) {
        try{
            FXMLLoader fxmlLoader = new FXMLLoader();
            URL url = getClass().getResource("/app/left/merge/merge.fxml");
            fxmlLoader.setLocation(url);
            GridPane root = fxmlLoader.load(url.openStream());

            Stage stage = new Stage();
            MergeConflictsController mcc = fxmlLoader.getController();
            mcc.setStage(stage);
            mcc.setConflictList(merge.getChanges());
            mcc.setLeftController(this);

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public boolean resolve(String path, String content) {
        return mainAppController.resolve(path, content);
    }

    public String getFileContent(String sha1) {
        return mainAppController.getFileContent(sha1);
    }

    public void mergePartB() {
        String title = "Commit";
        String content = "Please enter commit message:";
        InputDialog dialog = new InputDialog(title, content);

        Optional<String> commitMessage = dialog.showDialog();
        if (commitMessage.isPresent()) {
            mainAppController.mergePartB(commitMessage.get());
            InformationDialog infoDialog = new InformationDialog("Merge Done and commited succesfully!");
            infoDialog.showDialog();
        }

        mainAppController.updateViewInformation();
    }

    @FXML
    void reset(ActionEvent event) {
        if (mainAppController.getRepoName().equals("None")) {
            InformationDialog info = new InformationDialog("ERROR: There is no active repository in the system!");
            info.showDialog();
            return;
        }
        if (!mainAppController.isWCEmpty()) {
            ConfirmationDialog cd = new ConfirmationDialog("WC is open changes and you going to lose information", "Do you still want to continue?");
            Optional<ButtonType> ans = cd.showDialog();
            if (ans.get() == ButtonType.CANCEL) {
                return;
            }
        }

        InputDialog in = new InputDialog("Reset", "Enter commit sha1: ");
        Optional<String> commitSha1 = in.showDialog();

        if (commitSha1.isPresent()) {
            String message = mainAppController.reset(commitSha1.get());
            InformationDialog infoDialog = new InformationDialog(message);
            infoDialog.showDialog();
        }

        mainAppController.updateViewInformation();
    }

    @FXML
    void pull(ActionEvent event) {
        String message = mainAppController.pull();
        InformationDialog info = new InformationDialog(message);
        info.showDialog();

        mainAppController.updateViewInformation();
    }

    @FXML
    void push(ActionEvent event) {
        String message = mainAppController.push();
        InformationDialog info = new InformationDialog(message);
        info.showDialog();

        mainAppController.updateViewInformation();
    }
}

