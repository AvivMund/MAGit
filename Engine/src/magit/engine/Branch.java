package magit.engine;

public class Branch {
    private String name;
    private Commit commit;
    private String commitSha1;

    public Branch(String branchName, Commit commit, String commitSha1) {
        name = branchName;
        this.commit = commit;
        this.commitSha1 = commitSha1;
    }

    public String getName() { return name; }

    public Commit getCommit() {
        return commit;
    }

    public String getCommitSha1() {
        return commitSha1;
    }

    protected void setCommit(Commit commit) {
        this.commit = commit;
    }

    protected void setCommitSha1(String commitSha1) {
        this.commitSha1 = commitSha1;
    }
}