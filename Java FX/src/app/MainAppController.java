package app;

import app.bottom.BottomController;
import app.center.Center2Controller;
import app.dialogs.ConfirmationDialog;
import app.dialogs.InformationDialog;
import app.left.LeftController;
import app.right.RightController;
import app.top.TopController;
import javafx.fxml.FXML;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import magit.EngineInterface;
import magit.engine.*;
import magit.engine.xml.XmlLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MainAppController {
    private EngineInterface engine;
    @FXML private AnchorPane top;
    @FXML private TopController topController;
    @FXML private LeftController leftController;
    //@FXML private CenterController centerController;
    @FXML private Center2Controller center2Controller;
    @FXML private BottomController bottomController;
    @FXML private RightController rightController;
    private Stage primaryStage;

    @FXML
    public void initialize() {
        engine = new Engine();
        topController.setMainAppController(this);
        leftController.setMainAppController(this);
        //centerController.setMainAppController(this);
        center2Controller.setMainAppController(this);
        bottomController.setMainAppController(this);
        rightController.setMainAppController(this);
        rightController.setPrimaryStage(primaryStage);
        topController.setPrimaryStage(primaryStage);
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    public void changeUsername(String name) {
        engine.setUsername(name);
    }

    public String createNewRepo(String repoName, String location) {
        return engine.createRepo(repoName, location);
    }

    public String getRepoLocation() {
        return engine.getCurrRepoLocation();
    }

    public String getRepoName() {
        return engine.getCurrRepoName();
    }

    public void loadRepoFromXmlFile(String fullPath) throws IOException {
        String message;
        XmlLoader xmlLoader = new XmlLoader(fullPath);
        if (xmlLoader.isValid()) {
            if (!Files.exists(Paths.get(xmlLoader.getRepo().getLocation()))) {
                Files.createDirectory(Paths.get(xmlLoader.getRepo().getLocation()));
            }
            if (Files.exists(Paths.get(xmlLoader.getRepo().getLocation() + RepoManager.MAGIT_PATH))) {
                String headerText = "There is already other repository in the xml repo location";
                String contentText = "Do you want to continue and lose it?";
                ConfirmationDialog cd = new ConfirmationDialog(headerText, contentText);
                Optional<ButtonType> result = cd.showDialog();
                if (result.get() == ButtonType.OK) {
                    message = engine.loadRepoFromXml(xmlLoader, true);
                } else {
                    return;
                }
            } else if (Files.list(Paths.get(xmlLoader.getRepo().getLocation())).count() != 0) {
                message = "ERROR: the operation canceled because the repository location contains other data";
            } else {
                message = engine.loadRepoFromXml(xmlLoader, false);
            }
        } else {
            message = xmlLoader.getErrorMessage();
        }

        InformationDialog informationDialog = new InformationDialog(message);
        informationDialog.showDialog();

        if (message.equals("Repository loaded from xml successfully")) {
            updateViewInformation();
        }
    }

    public String switchRepo(String location) {
        return engine.switchCurrentRepo(location);
    }

    public Map<String, Branch> getBranches() {
        if (engine.getCurrRepoName().equals("None")){
            return null;
        }
        return engine.getBranches();
    }
//TODO: NOT FINISH YET
    public void updateViewInformation() {
        topController.updateRepoView();
        leftController.loadBranchesList();
        //centerController.loadCommits();
        center2Controller.loadCommits();
    }

    public Branch getHeadBranch() {
        return engine.getHeadBranch();
    }

    public String createNewBranch(String branchName, String commitSha1) {
        return engine.createBranch(branchName, commitSha1);
    }

    public String deleteBranch(String branchName) {
        return engine.deleteBranch(branchName);
    }

    public void checkout(String branchName) {
        String message = engine.beforeCheckout(branchName);
        if (message != null) {
            if (message.endsWith("information!")) {
                String contentText = "Do you still want to continue?";
                ConfirmationDialog cd = new ConfirmationDialog(message, contentText);
                Optional<ButtonType> result = cd.showDialog();
                if (result.get() == ButtonType.CANCEL) {
                    return;
                }
            } else {
                InformationDialog info = new InformationDialog(message);
                info.showDialog();
                return;
            }
        }
        engine.checkout(branchName);
        InformationDialog info = new InformationDialog("Checkout performed successfully!");
        info.showDialog();
    }

    public Map<String, Commit> getCommits() {
        return engine.getCommits();
    }

    public void showCommit(Commit selectedCommit) {
        Folder root = engine.getCommitTree(selectedCommit);
        bottomController.showCommit(selectedCommit, root);
    }

    public Map<ChangeType, List<Item>> getDelta(Folder treeRoot, String commit1Sha1) {
        return engine.getDelta(treeRoot, commit1Sha1);
    }

    public String makeACommit(String commitMessage) {
        return engine.createCommit(commitMessage);
    }

    public Map<ChangeType, List<Item>> getWCStatus() {
        return engine.getWCStatus();
    }

    public MergeData mergePartA(String selectedBranch) {
         return engine.mergePartA(selectedBranch);
    }

    public boolean isWCEmpty() {
        return engine.isWCEmpty();
    }

    public void mergePartB(String commitMessage) {
        engine.mergePartB(commitMessage);
    }

    public String getFileContent(String sha1) {
        return engine.getFileContent(sha1);
    }

    public boolean resolve(String path, String content) {
        return engine.resolve(path, content);
    }


    public String makeClone(String remoteRepoName, String remoteRepoLocation, String localRepName, String localRepoLocation) {
        return engine.clone(remoteRepoName, remoteRepoLocation, localRepName, localRepoLocation);
    }

    public String fetch() {
        return engine.fetch();
    }

    public String reset(String commitSha1) {
        return engine.reset(commitSha1);
    }

    public String pull() {
        return engine.pull();
    }

    public String push() {
        return engine.push();
    }

    public Branch getRBPointingCommit(String commitSha1) {
        return engine.getRBPointingCommit(commitSha1);
    }
}
