package app.top;

import app.MainAppController;
import app.dialogs.InformationDialog;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;

public class TopController {
    private MainAppController mainAppController;
    @FXML private Label username;
    @FXML private AnchorPane top;
    @FXML private TextField newUsername;
    @FXML private Button changeUsername;
    @FXML private Label repoName;
    @FXML private Label repoLocation;
    @FXML private Button createNewRepo;
    @FXML private Button loadRepoFromXml;
    @FXML private Button switchRepo;

    private SimpleStringProperty currUsername;
    private SimpleStringProperty currReponame;
    private SimpleStringProperty currRepoLocation;

    private Stage primaryStage;


    public TopController() {
        currUsername = new SimpleStringProperty("Username: Administrator");
        currReponame = new SimpleStringProperty("Repository name: None");
        currRepoLocation = new SimpleStringProperty("Location: None");
    }
    @FXML
    public void initialize() {
        username.textProperty().bind(currUsername);
        repoName.textProperty().bind(currReponame);
        repoLocation.textProperty().bind(currRepoLocation);
    }

    public void setMainAppController(MainAppController mainAppController) {
        this.mainAppController = mainAppController;
    }

    public void setPrimaryStage(Stage primaryStage) {
        this.primaryStage = primaryStage;
    }

    @FXML
    void createNewRepository(MouseEvent event) {
        handleRepoDirectoryChooser(true);
    }

    @FXML
    void loadRepoFromXmlFile(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select xml file");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("xml files", "*.xml"));
        File selectedFile = fileChooser.showOpenDialog(primaryStage);
        if (selectedFile == null) {
            return;
        }

        String absolutePath = selectedFile.getAbsolutePath();
        Platform.runLater(() -> runLoadXml(absolutePath));
    }

    private void runLoadXml(String absolutePath) {
        try {
            mainAppController.loadRepoFromXmlFile(absolutePath);
        } catch (IOException e) {
            e.printStackTrace();
            InformationDialog informationDialog = new InformationDialog();
            informationDialog.showDialog();
        }
        updateRepoView();
        mainAppController.updateViewInformation();
    }

    public void updateRepoView() {
        currReponame.set("Repository name: " + mainAppController.getRepoName());
        currRepoLocation.set("Location: " + mainAppController.getRepoLocation());
    }

    void handleRepoDirectoryChooser(boolean isNewRepo) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        File selectedDirectory = directoryChooser.showDialog(primaryStage);

        if(selectedDirectory == null){
            //No Directory selected
            return;
        }
        String repoName = selectedDirectory.getName();
        String location = selectedDirectory.getAbsolutePath();

        String message;
        if(isNewRepo) {
            message = mainAppController.createNewRepo(repoName, location);
        } else {
            message = mainAppController.switchRepo(location);
        }

        if (message.endsWith("successfully!")){
            mainAppController.updateViewInformation();
        }

        //show the message
        InformationDialog informationDialog = new InformationDialog(message);
        informationDialog.showDialog();

        updateRepoView();
        mainAppController.updateViewInformation();
    }

    @FXML
    void switchRepo(MouseEvent event) {
        handleRepoDirectoryChooser(false);
    }

    @FXML
    public void changeUsername(ActionEvent actionEvent) {
        String username = newUsername.textProperty().get();
        if (!username.isEmpty()) {
            mainAppController.changeUsername(username);
            currUsername.set("Username: " + username);
        }
    }
}
