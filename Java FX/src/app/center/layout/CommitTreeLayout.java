package app.center.layout;

import app.center.node.CommitNode;
import com.fxgraph.graph.Graph;
import com.fxgraph.graph.ICell;
import com.fxgraph.layout.Layout;

import java.util.ArrayList;
import java.util.List;

// simple test for scattering commits in imaginary tree, where every 3rd node is in a new 'branch' (moved to the right)
public class CommitTreeLayout implements Layout {
    final static int deltaX = 40;

    @Override
    public void execute(Graph graph) {
        final List<ICell> cells = graph.getModel().getAllCells();
        int startX = 10;
        int y = 10;
        List<CommitNode> openCol = new ArrayList<>();

        for (ICell cell : cells) {
            CommitNode c = (CommitNode) cell;
            String sha1 = c.getCommit().calculateSha1();
            boolean found = false;
            for (int i = 0; i < openCol.size(); i++) {
                if (openCol.get(i).getCommit().getPreviousCommit1Sha1().equals(sha1)) {
                    graph.getGraphic(c).relocate(startX + deltaX * i, y);
                    openCol.set(i, c);
                    found = true;
                    break;
                }
            }
            if (!found) {
                openCol.add(c);
                graph.getGraphic(c).relocate(startX + deltaX * (openCol.size() - 1), y);
            }
            y += 40;
        }
    }
}

