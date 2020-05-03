package magit.engine;

import java.util.HashMap;
import java.util.Map;

public class Repository {
    private Merge merge = null;
    private String name;
    private String location;
    private Folder headRoot;
    private Branch head;
    private Map<String, Branch> branches = new HashMap<>();
    private String remoteRepoLocation = null;
    private String remoteRepoName = null;

    public Repository(String repoName, String repoLocation, String username) {
        name = repoName;
        location = repoLocation;
        headRoot = new Folder(repoLocation, username);
        head = new Branch("master", null, null);
        branches.put(head.getName(), head);
    }

    public Repository(String repoName, String fullPath, Branch headBranch, Map<String, Branch> branches) {
        name = repoName;
        location = fullPath;
        head = headBranch;
        this.branches = branches;
    }

    public String getRepoName() { return name; }

    public String getRepoLocation() {
        return location;
    }

    public Branch getHead() {
        return head;
    }

    public Map<String, Branch> getBranches() {
        return branches;
    }

    public void setNewBranch(Branch newBranch) {
        branches.put(newBranch.getName(), newBranch);
    }

    public void removeBranch(String branchName) {
        branches.remove(branchName);
    }

    public void checkout(String branchName) {
        head = branches.get(branchName);
    }

    public Folder getRepoHeadTree() {
        return headRoot;
    }

    public void setHeadRoot(Folder newHeadTree) {
        headRoot = newHeadTree;
    }

    public void setMerge(Merge merge) {
        this.merge = merge;
    }

    public Merge getMerge() {
        return merge;
    }

    public String getRemoteRepoLocation() {
        return remoteRepoLocation;
    }

    public void setRemoteRepoLocation(String remoteRepoLocation) {
        this.remoteRepoLocation = remoteRepoLocation;
    }

    public String getRemoteRepoName() {
        return remoteRepoName;
    }

    public void setRemoteRepoName(String remoteRepoName) {
        this.remoteRepoName = remoteRepoName;
    }
}
