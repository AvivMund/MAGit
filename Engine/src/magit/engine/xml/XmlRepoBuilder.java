package magit.engine.xml;

import magit.engine.*;
import magit.xml.*;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class XmlRepoBuilder {
    MagitRepository magitRepo;
    private Map<String, MagitBlob> blobsMap;
    private Map<String, MagitSingleFolder> foldersMap;
    private Map<String, MagitSingleCommit> commitsMap;
    private Map<String, Blob> blobs = new HashMap<>();
    private Map<String, MagitBlob> magitBlobsBySha1 = new HashMap<>();
    private Map<String, Commit> commits = new HashMap<>();
    private Map<String, MagitSingleBranch> branchesNameMagitBranch = new HashMap<>();
    private Map<String, Folder> commitsTree = new HashMap<>();
    Repository repository;

    public XmlRepoBuilder(XmlLoader xmlLoader) {
        this.magitRepo = xmlLoader.getRepo();
        this.blobsMap = xmlLoader.getBlobsMap();
        this.foldersMap = xmlLoader.getFoldersMap();
        this.commitsMap = xmlLoader.getCommitsMap();
        buildRepo();
    }

    public Map<String, Commit> getCommits() {
        return commits;
    }

    public Map<String, Folder> getCommitsTree() {
        return commitsTree;
    }

    public Repository getRepository() {
        return repository;
    }

    public Map<String, MagitBlob> getMagitBlobsBySha1() {
        return magitBlobsBySha1;
    }

    private void buildRepo() {
        buildBlobsIntoMap();
        for (MagitSingleFolder f : foldersMap.values()) {
            if (f.isIsRoot()) {
                commitsTree.put(f.getId(), buildCommitsTrees(f, magitRepo.getLocation()));
            }
        }

        buildCommitsIntoMap();
        Map<String, Branch> branches = buildBranches();
        Branch head = branches.get(magitRepo.getMagitBranches().getHead());
        repository = new Repository(magitRepo.getName(), magitRepo.getLocation(), head, branches);
        MagitSingleBranch b = branchesNameMagitBranch.get(head.getName());
        MagitSingleCommit mc = commitsMap.get(b.getPointedCommit().getId());
        Folder root = commitsTree.get(mc.getRootFolder().getId());
        repository.setHeadRoot(root);
        if (magitRepo.getMagitRemoteReference() != null) {
            repository.setRemoteRepoName(magitRepo.getMagitRemoteReference().getName());
            repository.setRemoteRepoLocation(magitRepo.getMagitRemoteReference().getLocation());
        }
    }

    private void buildCommitsIntoMap() {
        for (MagitSingleCommit c : commitsMap.values()) {
            commits.put(c.getId(), new Commit(c, commitsTree.get(c.getRootFolder().getId()).getSha1()));
        }
        for (MagitSingleCommit currMagitCommit : commitsMap.values()) {
            Commit currCommit = commits.get(currMagitCommit.getId());
            if (currCommit.getPreviousCommit1Sha1() != null && currCommit.getPreviousCommit1Sha1().equals("")) {
                currCommit.setPreviousCommit1Sha1(findPrevCommitSha1(currMagitCommit));
            }
        }
    }

    private String findPrevCommitSha1(MagitSingleCommit currMagitCommit) {
        if (currMagitCommit.getPrecedingCommits() == null ||
                currMagitCommit.getPrecedingCommits().getPrecedingCommit().size() == 0) {
            return null;
        }
        MagitSingleCommit prevMagitCommit = commitsMap.get(currMagitCommit.getPrecedingCommits().getPrecedingCommit().get(0).getId());
        Commit prevCommit = commits.get(prevMagitCommit.getId());
        if (prevCommit.getPreviousCommit1Sha1() == "") {
            prevCommit.setPreviousCommit1Sha1(findPrevCommitSha1(prevMagitCommit));
        }

        return prevCommit.calculateSha1();
    }

    private void buildBlobsIntoMap() {
        for (MagitBlob b : blobsMap.values()) {
            Blob blob = new Blob(b);
            blobs.put(b.getId(), blob);
            magitBlobsBySha1.put(blob.getSha1(), b);
        }
    }

    private Folder buildCommitsTrees(MagitSingleFolder folder, String path) {
        Folder f = new Folder(folder, path);
        for (magit.xml.Item item : folder.getItems().getItem()) {
            if (item.getType().equals("blob")) {
                Blob b = blobs.get(item.getId());
                b.setFullPath(path + File.separator + b.getName());
                //b.setFile();
                f.addItemToItemsList(b);
            }
            else { //item.getType().equals("folder")
                MagitSingleFolder mf = foldersMap.get(item.getId());
                f.addItemToItemsList(buildCommitsTrees(mf, path + File.separator + mf.getName()));
            }
        }
        f.calculateSha1();

        return f;
    }

    private Map<String, Branch> buildBranches() {
        Map<String, Branch> branches = new HashMap<>();
        for (MagitSingleBranch b : magitRepo.getMagitBranches().getMagitSingleBranch()) {
            Commit c = commits.get(b.getPointedCommit().getId());
            branches.put(b.getName(), new Branch(b.getName(), c, c.calculateSha1()));
            branchesNameMagitBranch.put(b.getName(), b);
        }

        return branches;
    }

}
