package magit.engine;

import magit.EngineInterface;
import magit.engine.xml.XmlLoader;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

public class Engine implements EngineInterface {
    private RepoManager repoManager = new RepoManager();
    
    @Override
    public void setUsername(String username) { repoManager.setCurrentUserName(username); }

    @Override
    public String getUsername() { return repoManager.getCurrentUserName(); }

    @Override
    public String getCurrRepoLocation() {
        return repoManager.getCurrRepoLocation();
    }

    @Override
    public Map<ChangeType, List<Item>> getWCStatus() {
        return repoManager.getWCStatus();
    }

    @Override
    public Map<String, Branch> getBranches() {
        return repoManager.getBranches();
    }

    @Override
    public Branch getHeadBranch() {
        return repoManager.getHeadBranch();
    }

    @Override
    public String switchCurrentRepo(String fullPath) {
        String message = isTheRepoIsLegal(fullPath);
        if (message == null) {
            message = "ERROR: there is no repository in this location";
        } else if (message.equals("This location has already repository")) {
            if (repoManager.getCurrRepoLocation() != null &&  repoManager.getCurrRepoLocation().equals(fullPath)) {
                message = "The path you entered is already the active repository!";
            } else { //Repository is exist and not the active repo
                repoManager.setCurrentRepo(fullPath);
                message = "The active repository switched successfully!";
            }
        }

        return message;
    }

    @Override
    public String createBranch(String branchName, String commitSha1) {
        if (repoManager.getCurrRepoName().equals("None")) {
            return "ERROR: There is no active repository";
        }

        return repoManager.createBranch(branchName, commitSha1);
    }

    @Override
    public String deleteBranch(String branchName) {
        String message;
        BranchExistence status = repoManager.deleteBranch(branchName);
        if (status == BranchExistence.EXIST_AND_NOT_HEAD) {
            message = "Branch - " + branchName + " deleted successfully!";
        } else { //(status == BranchExistence.HEAD)
            message = "ERROR: Branch - '" + branchName + "' is the head branch and you can not delete it!";
        }
//        else { // status == BranchExistence.NOT_EXIST
//            message = "Unfortunately, Branch - " + branchName + " is not exist :(";
//        }

        return message;
    }

    @Override
    public String beforeCheckout(String branchName) {
        BranchExistence status = repoManager.checkBranchExistence(branchName);
        String message = null;
        if (status == BranchExistence.HEAD) {
            message = "Branch - " + branchName + " is already the head branch!";
        } else if (status == BranchExistence.EXIST_AND_NOT_HEAD) {
            if (!repoManager.isWCEmpty()) {
                message = "The current WC is with open changes, you are going to lose information!";
            }
        }

        return message;
    }

    @Override
    public void checkout(String branchName) {
        repoManager.checkout(branchName);
    }

    @Override
    public List<Commit> getActiveBranchCommitsHistory() {
        return repoManager.getActiveBranchCommitsHistory();
    }

    @Override
    public Folder getHeadTree() {
        return repoManager.getHeadTree();
    }

    @Override
    public String loadRepoFromXml(XmlLoader xmlLoader, boolean isContentFilesToDelete) {
        repoManager.loadXmlRepo(xmlLoader, isContentFilesToDelete);
        return "Repository loaded from xml successfully";
    }

    @Override
    public Map<String, Commit> getCommits() {
        return repoManager.getCommits();
    }

    @Override
    public Folder getCommitTree(Commit selectedCommit) {
        return repoManager.getCommitTree(selectedCommit);
    }

    @Override
    public Map<ChangeType, List<Item>> getDelta(Folder treeRoot, String commit1Sha1) {
        return repoManager.getDelta(treeRoot, commit1Sha1);
    }

    @Override
    public MergeData mergePartA(String selectedBranch) {
        return repoManager.mergePartA(selectedBranch);
    }

    @Override
    public boolean isWCEmpty() {
        return repoManager.isWCEmpty();
    }

    @Override
    public void mergePartB(String commitMessage) {
        repoManager.mergePartB(commitMessage);
    }

    @Override
    public String getFileContent(String sha1) {
        try {
            return Blob.getContent(repoManager.getCurrRepoLocation(), sha1);
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed getting Content";
        }
    }

    @Override
    public boolean resolve(String path, String content) {
        return repoManager.resolve(path, content);
    }

    @Override
    public String clone(String remoteRepoName, String remoteRepoLocation, String localRepName, String localRepoLocation) {
        File dir = new File(localRepoLocation);
        if (dir.list().length == 0) {
            if (Files.exists(Paths.get(remoteRepoLocation + RepoManager.MAGIT_PATH))) {
                return repoManager.clone(remoteRepoName, remoteRepoLocation, localRepName, localRepoLocation);

            } else {
                return "ERROR: The remote directory is not a repository";
            }
        } else {
            return "ERROR: The local directory is not an empty directory";
        }
    }

    @Override
    public String fetch() {
        return repoManager.fetch();
    }

    @Override
    public String reset(String commitSha1) {
        return repoManager.reset(commitSha1);
    }

    @Override
    public String pull() {
        return repoManager.pull();
    }

    @Override
    public String push() {
        return repoManager.push();
    }

    @Override
    public Branch getRBPointingCommit(String commitSha1) {
        return repoManager.getRBPointingCommit(commitSha1);
    }


    @Override
    public String getCurrRepoName() {
        return repoManager.getCurrRepoName();
    }

    @Override
    public String createRepo(String repoName, String repoPath) {
        String message = isTheRepoIsLegal(repoPath);

        // repo can created
        if (message == null) {
            message = "Repository is created successfully!";
            repoManager.createNewRepo(repoName, repoPath);
        }

        return message;
    }

    private String isTheRepoIsLegal(String locationPath) {
//        Path path = Paths.get(locationPath);
//        if (!Files.exists(path)) {
//            return "Unfortunately, this location is not exist";
//        }
//        if (!Files.isDirectory(path)) {
//            return "Unfortunately, this location is not directory";
//        }

//        if (!(Paths.get(locationPath).isAbsolute())) {
//            return "ERROR, you should enter full path of the repository";
//        }

        String temp = locationPath + "\\.magit";
        Path magitPath = Paths.get(temp);
        if (Files.exists(magitPath)) {
            return "This location has already repository";
        }

        return null; // The repository can created
    }

    @Override
    public String createCommit(String commitMessage) {
        if (repoManager.getCurrRepoName().equals("none")) {
            return "ERROR: There is no active repository in the system";
        }

         return repoManager.createCommit(commitMessage);
    }
}
