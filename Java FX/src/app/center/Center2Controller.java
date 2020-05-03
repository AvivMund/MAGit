package app.center;

import app.MainAppController;
import app.center.layout.CommitTreeLayout;
import app.center.node.CommitNode;
import app.center.node.CommitNodeController;
import app.center.node.SelectionListener;
import com.fxgraph.edges.Edge;
import com.fxgraph.graph.Graph;
import com.fxgraph.graph.ICell;
import com.fxgraph.graph.Model;
import com.fxgraph.graph.PannableCanvas;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ScrollPane;
import magit.engine.Branch;
import magit.engine.Commit;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Center2Controller implements SelectionListener {
    @FXML
    private ScrollPane scrollCommitsPane;
    private MainAppController mainAppController;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss:SSS");
    private Graph tree;
    private Model model;
    private static CommitNodeController selectedCommit = null;


    public void setMainAppController(MainAppController mainAppController) {
        this.mainAppController = mainAppController;
    }

    @FXML
    public void initialize() {
        tree = new Graph();
        createEmptyCommits();
        PannableCanvas canvas = tree.getCanvas();
        scrollCommitsPane.setContent(canvas);

        Platform.runLater(() -> {
            tree.getUseViewportGestures().set(false);
            tree.getUseNodeGestures().set(false);
        });
    }

    public void loadCommits() {
        if (mainAppController.getRepoName().equals("None")) {
            return;
        }
        Map<String, Branch> branchesByName = mainAppController.getBranches();
        Map<String, Commit> commits = mainAppController.getCommits();
        List<Commit> sortedCommits = new ArrayList<>(commits.values());
        sortedCommits.sort((o1, o2) -> {
            try {
                return dateFormat.parse(o2.getDate()).compareTo(dateFormat.parse(o1.getDate()));
            } catch (ParseException e) {
                e.printStackTrace();
                System.out.println("problem with commit date");
                return 0;
            }
        });

        initialize();
        model = tree.getModel();
        tree.beginUpdate();
        Map<String, CommitNode> commitNodesHash = new HashMap<>();

        for (Commit c : sortedCommits) {
            ICell cell = new CommitNode(c, mainAppController, this);
            //add to 'no edge commits'
            commitNodesHash.put(c.calculateSha1(), (CommitNode) cell);
            //add to model
            model.addCell(cell);
        }
        for (CommitNode cn : commitNodesHash.values()) {
            Commit cSrc = cn.getCommit();
            if (null != cSrc.getPreviousCommit1Sha1()) {
                CommitNode cnDst = commitNodesHash.get(cSrc.getPreviousCommit1Sha1());
                if (cnDst != null) {
                    final Edge edge = new Edge(cn, cnDst);
                    model.addEdge(edge);
                }
            }
            if (null != cSrc.getPreviousCommit2Sha1()) {
                CommitNode cnDst = commitNodesHash.get(cSrc.getPreviousCommit2Sha1());
                if (cnDst != null) {
                    final Edge edge = new Edge(cn, cnDst);
                    model.addEdge(edge);
                }
            }
        }
        for (Branch b : branchesByName.values()) {
            CommitNode commitNode = commitNodesHash.get(b.getCommitSha1());
            commitNode.addBranch(b.getName());
        }


        tree.endUpdate();
        tree.layout(new CommitTreeLayout());
    }

    private void createEmptyCommits() {
        model = tree.getModel();
        tree.beginUpdate();
        tree.endUpdate();
        tree.layout(new CommitTreeLayout());

    }

    @Override
    public void onSelect(CommitNodeController cnc) {
        if (selectedCommit != null) {
            selectedCommit.setSelected(false);
        }
        cnc.setSelected(true);
        selectedCommit = cnc;
    }
}
