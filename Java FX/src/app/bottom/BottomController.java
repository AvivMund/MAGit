package app.bottom;

import app.dialogs.InformationDialog;
import app.MainAppController;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import magit.engine.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class BottomController {
    private MainAppController mainAppController;
    @FXML private AnchorPane bottom;
    @FXML private TextArea commitInfoText;
    @FXML private TreeView<Item> commitTreeView;
    @FXML private TextArea textArea;
    private Image folderIcon = new Image("\\app\\bottom\\icons\\folderIcon2.png");
    private Image fileIcon = new Image("\\app\\bottom\\icons\\fileIcon2.png");

    public void setMainAppController(MainAppController mainAppController) {
        this.mainAppController = mainAppController;
    }

    @FXML
    void showFileContent(ActionEvent event) {
        if (commitTreeView.getSelectionModel().isEmpty()){
            InformationDialog info = new InformationDialog("ERROR: there is no file selected");
            info.showDialog();
        } else {
            TreeItem<Item> selectedfile = commitTreeView.getSelectionModel().getSelectedItem();
            if (selectedfile.getValue().getType() == ItemType.BLOB) {
                Blob blob = (Blob)selectedfile.getValue();
                try {
                    textArea.setText(Blob.getContent(mainAppController.getRepoLocation(), blob.getSha1()));
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("ERROR: problem with fetch file content");
                }
            } else { //FOLDER
                InformationDialog info = new InformationDialog("ERROR: there is no file selected, you selected folder");
                info.showDialog();
            }
        }
    }

    public void showCommit(Commit selectedCommit, Folder treeRoot) {
        StringBuilder sb = getCommitInfoText(selectedCommit, treeRoot);
        commitInfoText.setText(sb.toString());
        TreeItem<Item> root = new TreeItem<>(treeRoot, new ImageView(folderIcon));
        buildTreeView(root, treeRoot);
        commitTreeView.setRoot(root);
    }

    private void buildTreeView(TreeItem rootTreeItem, Folder rootFolder) {
        for (Item item : rootFolder.getItems()) {
            if (item.getType() == ItemType.BLOB) {
                rootTreeItem.getChildren().add(new TreeItem<>(item, new ImageView(fileIcon)));
            } else { // FOLDER
                TreeItem<Item> folderTreeItem = new TreeItem<>(item, new ImageView(folderIcon));
                rootTreeItem.getChildren().add(folderTreeItem);
                buildTreeView(folderTreeItem, (Folder)item);
            }
        }
    }

    private StringBuilder getCommitInfoText(Commit selectedCommit, Folder treeRoot) {
        StringBuilder sb = new StringBuilder();
        sb.append("Commit Sha-1: " + selectedCommit.calculateSha1() + "\n");
        sb.append("Message: " + selectedCommit.getMessage() + "\n");
        sb.append("Author: " + selectedCommit.getAuthor() + "\n");
        sb.append("Date: " + selectedCommit.getDate() + "\n");
        sb.append("\n");
        String prevCommit1Sha1 = selectedCommit.getPreviousCommit1Sha1();
        sb.append(getPrevCommitDeltaText(prevCommit1Sha1, treeRoot));
        sb.append("\n");
        String prevCommit2Sha1 = selectedCommit.getPreviousCommit2Sha1();
        sb.append(getPrevCommitDeltaText(prevCommit2Sha1, treeRoot));

        return sb;
    }

    private StringBuilder getPrevCommitDeltaText(String prevCommitSha1, Folder treeRoot) {
        StringBuilder sb = new StringBuilder();
        Map<ChangeType, List<Item>> wcStatus = null;
        if (prevCommitSha1 != null && !prevCommitSha1.equals("")) {
            sb.append("Previous Commit Sha-1: " + prevCommitSha1 + "\n");
            wcStatus = mainAppController.getDelta(treeRoot, prevCommitSha1);
            sb.append("\n");
            sb.append(commitDeltaStringBuilder(prevCommitSha1, wcStatus));
        } else {
            sb.append("\n");
            sb.append("Previous Commit Sha-1: None\n");
        }
        return sb;
    }

    private StringBuilder commitDeltaStringBuilder(String prevCommitSha1, Map<ChangeType, List<Item>> wcStatus) {
        StringBuilder sb = new StringBuilder();
        sb.append("Commit DELTA with previous commit " + prevCommitSha1 + ":\n");
        sb.append("Deleted files:" + "\n");
        sb.append(getItems(wcStatus.get(ChangeType.DELETED)));
        sb.append("Updated files:" + "\n");
        sb.append(getItems(wcStatus.get(ChangeType.UPDATED)));
        sb.append("Created files:" + "\n");
        sb.append(getItems(wcStatus.get(ChangeType.CREATED)));

        return sb;
    }

    public static StringBuilder getItems(List<Item> items) {
        StringBuilder sb = new StringBuilder();
        for (Item item : items) {
            sb.append(item.getFullPath() + System.lineSeparator());
        }
        return sb;
    }
}

