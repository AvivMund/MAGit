package magit.engine;


import magit.engine.xml.XmlLoader;
import magit.engine.xml.XmlRepoBuilder;
import magit.xml.MagitBlob;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static magit.engine.Utils.readFromTextFile;
import static magit.engine.Utils.writeToTextFile;

public class RepoManager {
    private String currentUserName = "Administrator";
    private Repository currentRepo = null;

    public static final String MAGIT_PATH = File.separator + ".magit";
    public static final String REMOTE_REPO_NAME_PATH = MAGIT_PATH + File.separator + "remote name";
    public static final String REPO_NAME_PATH = MAGIT_PATH + File.separator + "repository name";
    public static final String REMOTE_PATH = MAGIT_PATH + File.separator + "remote location";
    public static  final String OBJECTS_PATH = MAGIT_PATH + File.separator + "objects";
    public static  final String BRANCHES_PATH = MAGIT_PATH + File.separator + "branches";
    public static  final String HEAD_PATH =  BRANCHES_PATH + File.separator + "HEAD";
    public static  final String MASTER_PATH = BRANCHES_PATH + File.separator + "master";

    public String getCurrentUserName() {
        return currentUserName;
    }

    public String getCurrRepoLocation() {
        if (currentRepo == null){
            return "None";
        } else {
            return currentRepo.getRepoLocation();
        }
    }

    public void setCurrentUserName(String currentUserName) {
        this.currentUserName = currentUserName;
    }

    public String getRemoteRepoLocation() {
        return currentRepo.getRemoteRepoLocation();
    }

    public void setRemoteRepoLocation(String remoteRepoLocation) {
        currentRepo.setRemoteRepoLocation(remoteRepoLocation);
    }

    public String getRemoteRepoName() {
        return currentRepo.getRemoteRepoName();
    }

    public void setRemoteRepoName(String remoteRepoName) {
        currentRepo.setRemoteRepoName(remoteRepoName);
    }
    protected void setCurrentRepo(String fullPath) {
        String repoName = readFromTextFile(fullPath + REPO_NAME_PATH);
        String headBranchName = readFromTextFile(fullPath + HEAD_PATH);
        File remote = new File(fullPath + REMOTE_REPO_NAME_PATH);
        String remoteRepoName = null;
        String remoteRepoLocation = null;
        if (remote.exists()) {
            remoteRepoName = readFromTextFile(fullPath + REMOTE_REPO_NAME_PATH);
            remoteRepoLocation = readFromTextFile(fullPath + REMOTE_PATH);
        }
        Map<String, Branch> branches = loadBranches(fullPath, remoteRepoName);
        Branch headBranch = branches.get(headBranchName);
        Repository newRepo = new Repository(repoName, fullPath, headBranch, branches);
        currentRepo = newRepo;
        currentRepo.setRemoteRepoName(remoteRepoName);
        currentRepo.setRemoteRepoLocation(remoteRepoLocation);
        buildAndInitHeadTree();
    }

    private Map<String, Branch> loadBranches(String fullPath, String remoteRepoName) {
        Map<String, Branch> branches = new HashMap<>();
        try (Stream<Path> paths = Files.list(Paths.get(fullPath + BRANCHES_PATH + File.separator))) {
            paths.filter(f -> !f.getFileName().startsWith("HEAD") && !f.toFile().isDirectory()).forEach(curr -> branches.put(curr.getFileName().toString(), loadBranch(curr, curr.getFileName().toString(), fullPath)));
        } catch (IOException e) {
            System.out.println("Failed to load branches");
        }
        if (null != remoteRepoName) {
            try (Stream<Path> paths = Files.list(Paths.get(fullPath + BRANCHES_PATH + File.separator + remoteRepoName + File.separator))) {
                paths.filter(f -> !f.getFileName().startsWith("HEAD") && !f.toFile().isDirectory()).forEach(curr -> branches.put(remoteRepoName + File.separator + curr.getFileName().toString(), loadBranch(curr, remoteRepoName + File.separator + curr.getFileName().toString(), fullPath)));
            } catch (IOException e) {
                System.out.println("Failed to load branches");
            }
        }
        return branches;
    }

    public String getCurrRepoName() {
        return currentRepo == null ? "None" : currentRepo.getRepoName();
    }

    public void createNewRepo(String repoName, String repoPath) {
        currentRepo = new Repository(repoName, repoPath, currentUserName);
        createRepoInComputerLibrary(repoPath, repoName);
    }

    private void createRepoInComputerLibrary(String path, String repoName) {
        try {
            Files.createDirectory(Paths.get(path + MAGIT_PATH));
            Files.createDirectory(Paths.get(path + OBJECTS_PATH));
            Files.createDirectory(Paths.get(path + BRANCHES_PATH));

            writeToTextFile(path + HEAD_PATH, "master");
            writeToTextFile(path + MASTER_PATH, "null");
            writeToTextFile(path + REPO_NAME_PATH, repoName);

        } catch (IOException e) {
            System.out.println("Failed to create repo");
        }
    }

    public String createCommit(String commitMessage) {
        if (currentRepo == null) {
            return null;
        }
        Folder wcFullTree = (Folder)buildWCTree(currentRepo.getRepoLocation());
        Folder headFullTree = currentRepo.getRepoHeadTree();
        WCStatus wcStatus = new WCStatus(wcFullTree,headFullTree);
        if (wcStatus.isEmpty()) {
            return "ERROR: You cant create commit because there is no changes in the WC";
        }
        wcStatus.saveChanges(currentRepo.getRepoLocation() + OBJECTS_PATH);
        Commit commit;
        if (currentRepo.getMerge() == null) {
            String prevCommitSha1 = currentRepo.getHead().getCommitSha1();
            commit = new Commit(commitMessage, wcFullTree.getSha1(), currentUserName, prevCommitSha1, null);
        } else {
            String prevCommit1Sha1 = currentRepo.getMerge().getOurs().calculateSha1();
            String prevCommit2Sha1 = currentRepo.getMerge().getTheirs().calculateSha1();
            commit = new Commit(commitMessage, wcFullTree.getSha1(), currentUserName, prevCommit1Sha1, prevCommit2Sha1);
        }
        String newCommitSha1 = commit.calculateSha1();
        commit.saveObject(currentRepo.getRepoLocation() + OBJECTS_PATH);
        updateHeadBranch(commit, newCommitSha1);
        buildAndInitHeadTree();

        return "The Commit created successfully!";
    }

    private void updateHeadBranch(Commit commit, String newCommitSha1) {
        Branch branch = currentRepo.getHead();
        branch.setCommit(commit);
        branch.setCommitSha1(newCommitSha1);
        String branchName = branch.getName();
        String fullPath = currentRepo.getRepoLocation() + BRANCHES_PATH + File.separator + branchName;
        writeToTextFile(fullPath, newCommitSha1);
    }

    private Item buildWCTree(String currentLocation) {
        if (!Files.isDirectory(Paths.get(currentLocation))) {
            return new Blob(currentLocation, currentUserName);
        }

        Folder folder = new Folder(currentLocation, currentUserName);
        try (Stream<Path> paths = Files.list(Paths.get(currentLocation + File.separator))){
            if (paths.count() == 0) {
                return null;
            }
            try (Stream<Path> paths2 = Files.list(Paths.get(currentLocation + File.separator))){
                paths2.filter(f -> !f.getFileName().startsWith(".magit")).forEach(curr -> {
                    Item sub = buildWCTree(curr.toString());
                    if (sub != null) {
                        folder.addItemToItemsList(sub);
                    }
                });
                folder.calculateSha1();
            } catch (IOException e) {
                System.out.println("Failed to build working copy");
            }
        } catch (IOException e) {
            System.out.println("Failed to build working copy");
        }
        return folder;
    }

    public Map<ChangeType, List<Item>> getWCStatus() {
        if (currentRepo == null) {
            return null;
        }
        Folder wcFullTree = (Folder)buildWCTree(currentRepo.getRepoLocation());
        Folder headFullTree = currentRepo.getRepoHeadTree();
        WCStatus wcStatus = new WCStatus(wcFullTree,headFullTree);
        Map<ChangeType, List<Item>> changes = getChangeTypeListMap(wcStatus);
        return changes;
    }

    private Map<ChangeType, List<Item>> getChangeTypeListMap(WCStatus wcStatus) {
        Map<ChangeType, List<Item>> changes = new HashMap<>();
        changes.put(ChangeType.DELETED, wcStatus.getDeleted());
        changes.put(ChangeType.CREATED, wcStatus.getCreated());
        changes.put(ChangeType.UPDATED, wcStatus.getUpdated());
        return changes;
    }

    public Map<String, Branch> getBranches() {
        if (currentRepo == null) {
            return null;
        }
        return currentRepo.getBranches();
    }

    public Branch getHeadBranch() {
        if (currentRepo == null) {
            return null;
        }
        return currentRepo.getHead();
    }

    private Branch loadBranch(Path branchPath, String branchName, String repoPath) {
        String commitSha1 = readFromTextFile(branchPath.toString());
        Commit commit = null;
        try {
            if (commitSha1 != null && !commitSha1.equals("null")) {
                commit = Commit.loadCommitObject(repoPath + OBJECTS_PATH + File.separator + commitSha1);
            } else {
                commitSha1 = null;
            }
        } catch (Exception e) {
            System.out.println("Failed to load branch " + branchName);
        }

        return new Branch(branchName, commit, commitSha1);
    }

    public String createBranch(String branchName, String commitSha1) {
        if (currentRepo.getBranches().get(branchName) != null) {
            return "ERROR: Branch- " + branchName + " is already exist";
        }

        Commit commit;
        try {
            commit = Commit.loadCommitObject(currentRepo.getRepoLocation() + OBJECTS_PATH + File.separator + commitSha1);
        } catch (Exception e) {
            //e.printStackTrace();
            return "ERROR: the commit sha1 is illegal or not exist";
        }

        Branch newBranch = new Branch(branchName, commit, commitSha1);
        currentRepo.setNewBranch(newBranch);
        writeToTextFile(currentRepo.getRepoLocation() + BRANCHES_PATH + File.separator + branchName, commitSha1);
        return "Branch- " + branchName + " created successfully!";
    }

    public BranchExistence deleteBranch(String branchName) {
        BranchExistence status = checkBranchExistence(branchName);
        if (status == BranchExistence.EXIST_AND_NOT_HEAD) {
            currentRepo.removeBranch(branchName);
            String branchPath = currentRepo.getRepoLocation() + BRANCHES_PATH + File.separator + branchName;
            deleteFile(branchPath);
        }

        return status;
    }

    public BranchExistence checkBranchExistence (String branchName) {
        if (currentRepo.getBranches().get(branchName) == null) {
            return BranchExistence.NOT_EXIST;
        }
        if (currentRepo.getHead().getName().equals(branchName)) {
            return BranchExistence.HEAD;
        }

        return BranchExistence.EXIST_AND_NOT_HEAD;
    }

    public boolean isWCEmpty() {
        Folder wcFullTree = (Folder)buildWCTree(currentRepo.getRepoLocation());
        Folder headFullTree = currentRepo.getRepoHeadTree();
        WCStatus wcStatus = new WCStatus(wcFullTree,headFullTree);

        return wcStatus.isEmpty();
    }

    public void checkout(String branchName) {
        currentRepo.checkout(branchName);
        writeToTextFile(currentRepo.getRepoLocation() + HEAD_PATH, branchName);
        deleteWC(currentRepo.getRepoLocation());
        if (!buildAndInitHeadTree()) return;
        currentRepo.getRepoHeadTree().loadToWC(currentRepo.getRepoLocation());
    }

    private boolean buildAndInitHeadTree() {
        Branch headBranch = currentRepo.getHead();
        Commit headCommit = headBranch.getCommit();
        if (headCommit == null) {
            return false;
        }
        Folder newHeadRoot = loadTreeByCommit(headCommit);
        currentRepo.setHeadRoot(newHeadRoot);
        return true;
    }

    private Folder loadTreeByCommit(Commit headCommit) {
        return loadTreeByCommit(headCommit, currentRepo.getRepoLocation());
    }

    private Folder loadTreeByCommit(Commit headCommit, String repoLocation) {
        String rootFolderSha1 = headCommit.getSha1();
        Folder newHeadRoot = new Folder(repoLocation, headCommit.getAuthor());
        try {
            Folder.loadObject(repoLocation + OBJECTS_PATH, rootFolderSha1, currentRepo.getRepoLocation(), newHeadRoot);
        } catch (Exception e) {
            System.out.println("Failed to build and initialize head commit tree");
        }
        newHeadRoot.calculateSha1();
        return newHeadRoot;
    }

    private void deleteWC(String path) {
        try (Stream<Path> paths = Files.list(Paths.get(path))) {
            List<String> toBeDeleted = paths.filter(f -> !f.getFileName().startsWith(".magit")).map(p -> p.toString()).collect(Collectors.toList());
            for (String p : toBeDeleted) {
                deleteFile(p);
            }
        } catch (IOException e) {
            System.out.println("Failed to delete working copy");
        }
    }

    private void deleteFile(String path) {
        File file = new File(path);
        if (file.isDirectory()) {
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                System.out.println("Failed to delete " + path);
            }
        } else {
            file.delete();
        }
    }

    public List<Commit> getActiveBranchCommitsHistory() {
        List<Commit> commits = new ArrayList<>();
        Branch activeBranch = currentRepo.getHead();
        Commit commit = activeBranch.getCommit();
        if (commit != null) {
            while (commit.getPreviousCommit1Sha1() != null) {
                try {
                    commits.add(commit);
                    String previousCommitSha1 = commit.getPreviousCommit1Sha1();
                    commit = Commit.loadCommitObject(currentRepo.getRepoLocation() + OBJECTS_PATH + File.separator + previousCommitSha1);
                } catch (Exception e) {
                    System.out.println("Failed to load commit");
                }
            }
            commits.add(commit);
        }

        return commits;
    }

    public Folder getHeadTree() {
        return currentRepo.getRepoHeadTree();
    }

    public void loadXmlRepo(XmlLoader xmlLoader, boolean isContentFilesToDelete) {
        if(isContentFilesToDelete) {
            File file = new File(xmlLoader.getRepo().getLocation() + MAGIT_PATH);
            try {
                FileUtils.deleteDirectory(file);
            } catch (IOException e) {
                System.out.println("Failed to delete " + xmlLoader.getRepo().getLocation() + MAGIT_PATH);
                return;
            }
            deleteWC(xmlLoader.getRepo().getLocation());
        }
        XmlRepoBuilder repoBuilder = new XmlRepoBuilder(xmlLoader);
        currentRepo = repoBuilder.getRepository();
        String repoLocation = currentRepo.getRepoLocation();
        createRepoInComputerLibrary(repoLocation, currentRepo.getRepoName());
        Map<String, Commit> commits = repoBuilder.getCommits();
        for (Commit c : commits.values()) {
            c.saveObject(repoLocation + OBJECTS_PATH);
        }
        Map<String, Folder> rootFolders = repoBuilder.getCommitsTree();
        Map<String, MagitBlob> magitBlobMap = repoBuilder.getMagitBlobsBySha1();
        for (Folder f : rootFolders.values()) {
            saveTree(f, magitBlobMap);
        }
        Map<String, Branch> branches = currentRepo.getBranches();
        Branch head = currentRepo.getHead();
        Utils.writeToTextFile(repoLocation + HEAD_PATH, head.getName());
        if (xmlLoader.getRepo().getMagitRemoteReference() != null) {
            String remoteName = xmlLoader.getRepo().getMagitRemoteReference().getName();
            try {
                Files.createDirectories(Paths.get(repoLocation + BRANCHES_PATH + File.separator + remoteName));
            } catch (IOException e) {
                //e.printStackTrace();
                return;
            }
            writeToTextFile(repoLocation + REMOTE_REPO_NAME_PATH, remoteName);
            writeToTextFile(repoLocation + REMOTE_PATH, xmlLoader.getRepo().getMagitRemoteReference().getLocation());
        }
        for (Branch b : branches.values()) {
            Utils.writeToTextFile(repoLocation + BRANCHES_PATH + File.separator + b.getName(), b.getCommitSha1());
        }
        Folder root = currentRepo.getRepoHeadTree();
        root.loadToWC(root.getFullPath());
    }

    private void saveTree(Folder f, Map<String, MagitBlob> magitBlobMap) {
        f.saveObject(currentRepo.getRepoLocation() + OBJECTS_PATH);
        for (magit.engine.Item item : f.getItems()) {
            if (item.getType() == ItemType.FOLDER) {
                saveTree((Folder)item, magitBlobMap);
            } else { //item.getType() == ItemType.BLOB
                ((Blob)item).saveObject(currentRepo.getRepoLocation() + OBJECTS_PATH, magitBlobMap.get(item.getSha1()).getContent());
            }
        }
    }

    public Map<String, Commit> getCommits() {
        Map<String, Commit> commits = new HashMap<>();

        if(currentRepo != null) {
            Map<String, Branch> branches= currentRepo.getBranches();
            for (Branch b : branches.values()) {
                getPrevCommits(commits, b.getCommitSha1());
            }
        }

        return commits;
    }

    private void getPrevCommits(Map<String, Commit> commits, String commitSha1) {
        Commit currCommit = null;
        try {
            currCommit = Commit.loadCommitObject(currentRepo.getRepoLocation() + OBJECTS_PATH + File.separator + commitSha1);
            commits.put(commitSha1, currCommit);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        if (currCommit != null && currCommit.getPreviousCommit1Sha1() != null) {
            getPrevCommits(commits, currCommit.getPreviousCommit1Sha1());
        }
    }

    public Folder getCommitTree(Commit selectedCommit) {
        return loadTreeByCommit(selectedCommit);
    }

    public Map<ChangeType, List<Item>> getDelta(Folder treeRoot, String commit1Sha1) {
        Commit commit;
        try {
            commit = Commit.loadCommitObject(currentRepo.getRepoLocation() + OBJECTS_PATH + File.separator + commit1Sha1);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR: commit is not Loaded");
            return null;
        }

        Folder treeRoot2 = loadTreeByCommit(commit);
        WCStatus wcStatus = new WCStatus(treeRoot, treeRoot2);
        Map<ChangeType, List<Item>> changes = getChangeTypeListMap(wcStatus);

        return changes;
    }

    private void fastForward(Commit theirsCommit) {
    Branch head = currentRepo.getHead();
    head.setCommit(theirsCommit);
    head.setCommitSha1(theirsCommit.calculateSha1());
    writeToTextFile(currentRepo.getRepoLocation() + BRANCHES_PATH + File.separator + head.getName(), theirsCommit.calculateSha1());
    checkout(head.getName());
}

    public MergeData mergePartA(String selectedBranch) {
        //mergePartA
        Commit oursCommit = currentRepo.getHead().getCommit();
        Commit theirsCommit = currentRepo.getBranches().get(selectedBranch).getCommit();
        Commit common;
        try {
            common = findCommonAncestor(oursCommit, theirsCommit);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("ERROR: with parse date or load commit in findCommonAncestor function");
            return null;
        }

        //fast Forward
        if (common.calculateSha1().equals(oursCommit.calculateSha1())){
            fastForward(theirsCommit);
            return new MergeData("Fast Forward: head is 'theirs'", null);
        }
        if (common.calculateSha1().equals(theirsCommit.calculateSha1())) {
            return new MergeData("Fast Forward: no change", null);
        }

        Folder oursTree = currentRepo.getRepoHeadTree();
        Folder theirsTree = loadTreeByCommit(theirsCommit);
        Folder commonTree = loadTreeByCommit(common);

        WCStatus oursAndCommon = new WCStatus(oursTree, commonTree);
        WCStatus theirsAndCommon = new WCStatus(theirsTree, commonTree);

        Merge merge = new Merge(oursTree, theirsTree, commonTree, oursAndCommon, theirsAndCommon, oursCommit, theirsCommit, currentRepo.getRepoLocation());
        currentRepo.setMerge(merge);

        return merge.getMergeData();
    }

    private Commit findCommonAncestor(Commit commit1, Commit commit2) throws Exception {
        Commit curr1 = commit1;
        Commit curr2 = commit2;
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss:SSS");
        String sha1;
        while(!curr1.calculateSha1().equals(curr2.calculateSha1())) {

            int compare = dateFormat.parse(curr1.getDate()).compareTo(dateFormat.parse(curr2.getDate()));
            if (compare < 0) {
                sha1 = curr2.getPreviousCommit1Sha1();
                curr2 = Commit.loadCommitObject(currentRepo.getRepoLocation() + OBJECTS_PATH + File.separator + sha1);
            }
            if (compare > 0) {
                sha1 = curr1.getPreviousCommit1Sha1();
                curr1 = Commit.loadCommitObject(currentRepo.getRepoLocation() + OBJECTS_PATH + File.separator + sha1);
            }
        }

        return curr1;
    }

    public void mergePartB(String commitMessage) {
        createCommit(commitMessage);
        currentRepo.setMerge(null);
    }

    public boolean resolve(String path, String content) {
        if (content == null) {
            deleteFile(path);
        } else {
            Utils.writeToTextFile(path, content);
        }
        Map<String, Change> changes = currentRepo.getMerge().getMergeData().getChanges();
        changes.remove(path);
        return changes.isEmpty();
    }

    public String clone(String remoteRepoName, String remoteRepoLocation, String localRepName, String localRepoLocation) {
        try {
            Files.createDirectory(Paths.get(localRepoLocation + MAGIT_PATH));
            Files.createDirectory(Paths.get(localRepoLocation + OBJECTS_PATH));
            Files.createDirectory(Paths.get(localRepoLocation + BRANCHES_PATH));
            Files.createDirectory(Paths.get(localRepoLocation + BRANCHES_PATH + File.separator + remoteRepoName));
            writeToTextFile(localRepoLocation + REPO_NAME_PATH, localRepName);
            writeToTextFile(localRepoLocation + REMOTE_REPO_NAME_PATH, remoteRepoName);
            writeToTextFile(localRepoLocation + REMOTE_PATH, remoteRepoLocation);
            fetchRemoteObjects(remoteRepoLocation, localRepoLocation);
            fetchRemoteBranches(remoteRepoLocation, localRepoLocation, remoteRepoName);
            FileUtils.moveFileToDirectory(new File(localRepoLocation + BRANCHES_PATH + File.separator + remoteRepoName + File.separator + "HEAD"),
                    new File(localRepoLocation + BRANCHES_PATH), false);
            String headBranchName = readFromTextFile(localRepoLocation + HEAD_PATH);
            FileUtils.copyFileToDirectory(new File(localRepoLocation + BRANCHES_PATH + File.separator + remoteRepoName + File.separator + headBranchName),
                    new File(localRepoLocation + BRANCHES_PATH), false);
            setCurrentRepo(localRepoLocation);
            checkout(headBranchName);

        } catch (IOException e) {
            System.out.println("Failed to create clone");
            return "Failed to create clone";
        }


        return "Clone Create successfully!";
    }

    private void fetchRemoteBranches(String remoteRepoLocation, String localRepoLocation, String remoteRepoName) throws IOException {
        String remoteBranches = remoteRepoLocation + BRANCHES_PATH;
        String localBranches = localRepoLocation + BRANCHES_PATH + File.separator + remoteRepoName;

        fetchDir(remoteBranches, localBranches);
    }

    private void fetchDir(String remoteBranches, String localBranches) throws IOException {
        File remote = new File(remoteBranches);
        File local = new File(localBranches);

        Set<String> diffSet = new HashSet<>(Arrays.asList(remote.list()));
        if (local.list().length > 0) {
            diffSet.removeAll(Arrays.asList(local.list()));
        }
        for (String obj : diffSet) {
            FileUtils.copyFile(new File(remoteBranches + File.separator + obj),
                    new File(localBranches + File.separator + obj));
        }
    }

    private void fetchRemoteObjects(String remoteRepoLocation, String localRepoLocation) throws IOException {
        String remoteObjects = remoteRepoLocation + OBJECTS_PATH;
        String localObjects = localRepoLocation + OBJECTS_PATH;

        fetchDir(remoteObjects, localObjects);
    }

    public String fetch() {
        if (currentRepo == null) {
            return "ERROR: There is no active repository in the system";
        }
        if (!Files.exists(Paths.get(currentRepo.getRepoLocation() + REMOTE_PATH))) {
            return "ERROR: this repository does not have a remote repository";
        }
        String remoteRepoLocation = readFromTextFile(currentRepo.getRepoLocation() + REMOTE_PATH);
        String localRepoLocation = currentRepo.getRepoLocation();
        String remoteRepoName = readFromTextFile(remoteRepoLocation + REPO_NAME_PATH);

        try {
            fetchRemoteBranches(remoteRepoLocation, localRepoLocation, remoteRepoName);
            fetchRemoteObjects(remoteRepoLocation, localRepoLocation);
        } catch (IOException e) {
            e.printStackTrace();
            return "ERROR: can not fetch files";
        }

        return "Fetch done successfully!";
    }

    public String reset(String commitSha1) {
        Commit commit;
        try {
            commit = Commit.loadCommitObject(currentRepo.getRepoLocation() + OBJECTS_PATH + File.separator + commitSha1);
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: the sha1 you enter is illegal";
        }

        Branch head = currentRepo.getHead();
        head.setCommit(commit);
        head.setCommitSha1(commitSha1);
        writeToTextFile(currentRepo.getRepoLocation() + BRANCHES_PATH + File.separator + head.getName(), commitSha1);
        checkout(head.getName());

        return "Reset done Successfully!";
    }

    public String pull() {
        if (currentRepo == null) {
            return "ERROR: there is no active repository in the system!";
        }
        if (currentRepo.getRemoteRepoLocation() == null) {
            return "ERROR: the active repository has no remote repository!";
        }
        if (!currentRepo.getBranches().containsKey(currentRepo.getRemoteRepoName() + File.separator + currentRepo.getHead().getName())) {
           return "ERROR: the head branch is not a remote tracking branch";
        }

        Branch head = currentRepo.getHead();
        Repository remoteRepo = loadRemoteRepo(currentRepo.getRemoteRepoLocation());
        Branch remoteRepoBranch = remoteRepo.getBranches().get(head.getName());

        try {
            pullAllCommitsBetween(head.getCommit(), remoteRepoBranch.getCommit(), remoteRepo.getRepoLocation());
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: problem during pull";
        }
        reset(remoteRepoBranch.getCommitSha1());

        //update remote branch
        writeToTextFile(currentRepo.getRepoLocation() + BRANCHES_PATH + File.separator + currentRepo.getRemoteRepoName() + File.separator + remoteRepoBranch.getName(), remoteRepoBranch.getCommitSha1());

        return "Pull done successfully!";
    }

    private void pullAllCommitsBetween(Commit prev, Commit update, String remoteRepoPath) throws Exception {
        String prevSha1 = prev.calculateSha1();
        if (prevSha1.equals(update.calculateSha1())) {
            return;
        }
        Commit curr1 = update;
        Commit curr2 = Commit.loadCommitObject(remoteRepoPath + OBJECTS_PATH + File.separator + curr1.getPreviousCommit1Sha1());
        do {
            curr1.saveObject(currentRepo.getRepoLocation() + OBJECTS_PATH);
            Folder prevRoot = loadTreeByCommit(curr2, remoteRepoPath);
            Folder updateRoot = loadTreeByCommit(curr1, remoteRepoPath);

            WCStatus delta = new WCStatus(updateRoot, prevRoot);
            delta.saveChanges(currentRepo.getRepoLocation() + OBJECTS_PATH);

            curr1 = curr2;
            curr2 = Commit.loadCommitObject(remoteRepoPath + OBJECTS_PATH + File.separator + curr1.getPreviousCommit1Sha1());
        } while (!prevSha1.equals(curr1.calculateSha1()));
    }

    private Repository loadRemoteRepo(String fullPath) {
        String headBranchName = readFromTextFile(fullPath + HEAD_PATH);
        File remote = new File(fullPath + REMOTE_REPO_NAME_PATH);
        Map<String, Branch> branches = loadBranches(fullPath, null);
        Branch headBranch = branches.get(headBranchName);
        return new Repository(null, fullPath, headBranch, branches);
    }

    private void pushAllCommitsBetween(Commit prev, Commit update) throws Exception {
        String prevSha1 = prev.calculateSha1();
        if (prevSha1.equals(update.calculateSha1())) {
            return;
        }
        String repoLocation = currentRepo.getRepoLocation();
        String remoteRepoLocation = currentRepo.getRemoteRepoLocation();
        Commit curr1 = update;
        Commit curr2 = Commit.loadCommitObject(repoLocation + OBJECTS_PATH + File.separator + curr1.getPreviousCommit1Sha1());

        do {
            curr1.saveObject(remoteRepoLocation + OBJECTS_PATH);
            Folder prevRoot = loadTreeByCommit(curr2, repoLocation);
            Folder updateRoot = loadTreeByCommit(curr1, repoLocation);

            WCStatus delta = new WCStatus(updateRoot, prevRoot);
            delta.saveChanges(remoteRepoLocation + OBJECTS_PATH);

            curr1 = curr2;
            curr2 = Commit.loadCommitObject(repoLocation + OBJECTS_PATH + File.separator + curr1.getPreviousCommit1Sha1());
        } while (!prevSha1.equals(curr1.calculateSha1()));
    }

    public String push() {
        if (currentRepo == null) {
            return "ERROR: there is no active repository in the system!";
        }
        if (currentRepo.getRemoteRepoLocation() == null) {
            return "ERROR: the active repository has no remote repository!";
        }
        if (!currentRepo.getBranches().containsKey(currentRepo.getRemoteRepoName() + File.separator + currentRepo.getHead().getName())) {
            return "ERROR: the head branch is not a remote tracking branch";
        }

        Repository remoteRepo = loadRemoteRepo(currentRepo.getRemoteRepoLocation());
        Branch headRB = currentRepo.getBranches().get(currentRepo.getRemoteRepoName() + File.separator + currentRepo.getHead().getName());
        Branch head = currentRepo.getHead();
        Branch remoteRepoBranch = remoteRepo.getBranches().get(head.getName());

        if(!headRB.getCommitSha1().equals(remoteRepoBranch.getCommitSha1())) {
            return "ERROR: the remote repository is not clean";
        }

        try {
            pushAllCommitsBetween(headRB.getCommit(), head.getCommit());
        } catch (Exception e) {
            //e.printStackTrace();
            return "ERROR: problem during push";
        }

        writeToTextFile(currentRepo.getRemoteRepoLocation() + BRANCHES_PATH + File.separator + remoteRepoBranch.getName(), head.getCommitSha1());

        return "Push done successfully!";
    }

    public Branch getRBPointingCommit(String commitSha1) {
        Map<String, Branch> branches = currentRepo.getBranches();
        for (Branch b : branches.values()) {
            if (b.getCommitSha1().equals(commitSha1) && b.getName().indexOf('\\') > 0) {
                return b;
            }
        }

        return null;
    }
}

