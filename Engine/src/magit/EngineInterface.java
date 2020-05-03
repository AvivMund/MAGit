package magit;

import magit.engine.*;
import magit.engine.xml.XmlLoader;

import java.util.List;
import java.util.Map;

public interface EngineInterface {
    void setUsername(String username);
    String getUsername();
    String createRepo(String repoName, String repoPath);
    String getCurrRepoName();
    String createCommit(String commitMessage);
    String getCurrRepoLocation();
    Map<ChangeType, List<Item>> getWCStatus();
    Map<String, Branch> getBranches();
    Branch getHeadBranch();
    String switchCurrentRepo(String fullPath);
    String createBranch(String branchName, String commitSha1);
    String deleteBranch(String branchName);
    String beforeCheckout(String branchName);
    void checkout(String branchName);
    List<Commit> getActiveBranchCommitsHistory();
    Folder getHeadTree();
    String loadRepoFromXml(XmlLoader xmlLoader, boolean isContentFilesToDelete);
    Map<String, Commit> getCommits();
    Folder getCommitTree(Commit selectedCommit);
    Map<ChangeType, List<Item>> getDelta(Folder treeRoot, String commit1Sha1);
    MergeData mergePartA(String selectedBranch);
    boolean isWCEmpty();
    void mergePartB(String commitMessage);
    String getFileContent(String sha1);
    boolean resolve(String path, String content);
    String clone(String remoteRepoName, String remoteRepoLocation, String localRepName, String localRepoLocation);
    String fetch();
    String reset(String commitSha1);
    String pull();
    String push();
    Branch getRBPointingCommit(String commitSha1);
}
