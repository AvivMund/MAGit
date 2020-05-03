package app.center.node;

import app.MainAppController;
import com.fxgraph.cells.AbstractCell;
import com.fxgraph.graph.Graph;
import com.fxgraph.graph.IEdge;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Region;
import magit.engine.Commit;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommitNode extends AbstractCell implements SelectionListener {
    private CommitNodeController commitNodeController;
    private Commit commit;
    private MainAppController mainAppController;
    private List<String> branches = new ArrayList<>();
    private SelectionListener markListener;

    public CommitNode(Commit commit, MainAppController mainAppController, SelectionListener listener) {
        this.commit = commit;
        this.mainAppController = mainAppController;
        markListener = listener;
    }

    public void addBranch(String branchName) {
        branches.add(branchName);
    }

    public Commit getCommit() {
        return commit;
    }

    @Override
    public Region getGraphic(Graph graph) {

        try {

            FXMLLoader fxmlLoader = new FXMLLoader();
            URL url = getClass().getResource("commitNode.fxml");
            fxmlLoader.setLocation(url);
            GridPane root = fxmlLoader.load(url.openStream());

            commitNodeController = fxmlLoader.getController();
            commitNodeController.setCommitMessage(commit.getMessage());
            commitNodeController.setCommitter(commit.getAuthor());
            commitNodeController.setCommitTimeStamp(commit.getDate());
            commitNodeController.addListener(this);
            commitNodeController.addListener(markListener);
            String txt = Arrays.toString(branches.toArray());
            commitNodeController.setBranchesLabel(txt);

            return root;
        } catch (IOException e) {
            return new Label("Error when tried to create graphic node !");
        }
    }

    @Override
    public DoubleBinding getXAnchor(Graph graph, IEdge edge) {
        final Region graphic = graph.getGraphic(this);
        return graphic.layoutXProperty().add(commitNodeController.getCircleRadius());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CommitNode that = (CommitNode) o;

        return commit.getDate() != null ? commit.getDate().equals(that.commit.getDate()) : that.commit.getDate() == null;
    }

    @Override
    public int hashCode() {
        return commit.getDate() != null ? commit.getDate().hashCode() : 0;
    }

    @Override
    public void onSelect(CommitNodeController cnc) {
        mainAppController.showCommit(commit);
    }
}
