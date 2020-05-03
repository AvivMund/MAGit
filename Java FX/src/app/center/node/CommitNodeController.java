package app.center.node;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.ArrayList;
import java.util.List;

public class CommitNodeController {
    @FXML private GridPane gridPane;
    @FXML private Label commitTimeStampLabel;
    @FXML private Label messageLabel;
    @FXML private Label committerLabel;
    @FXML private Label branchesLabel;
    @FXML private Circle CommitCircle;
    private List<SelectionListener> listeners = new ArrayList<>();

    @FXML
    void showCommit(MouseEvent event) {
        System.out.println("mouse clicked" + event.toString());
        for (SelectionListener listener: listeners) {
            listener.onSelect(this);
        }
    }

    public void addListener(SelectionListener listener) {
        this.listeners.add(listener);

    }

    public void setCommitTimeStamp(String timeStamp) {
        commitTimeStampLabel.setText(timeStamp);
        commitTimeStampLabel.setTooltip(new Tooltip(timeStamp));
    }

    public void setCommitter(String committerName) {
        committerLabel.setText(committerName);
        committerLabel.setTooltip(new Tooltip(committerName));
    }

    public void setCommitMessage(String commitMessage) {
        messageLabel.setText(commitMessage);
        messageLabel.setTooltip(new Tooltip(commitMessage));
    }

    public void setBranchesLabel(String branchesTxt) {
        branchesLabel.setText(branchesTxt);
        branchesLabel.setTooltip(new Tooltip(branchesTxt));    }

    public int getCircleRadius() {
        return (int)CommitCircle.getRadius();
    }

    public void setSelected(boolean b) {
        if (b) {
            CommitCircle.fillProperty().setValue(Color.RED);
        } else {
            CommitCircle.fillProperty().setValue(Color.BLUE);
        }
    }
}
