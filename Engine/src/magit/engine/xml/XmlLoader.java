package magit.engine.xml;

import magit.xml.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class XmlLoader {
    private String xmlFullPath;
    private MagitRepository repo;
    private String errorMessage = "";
    private Map<String, MagitBlob> blobsMap = new HashMap<>();
    private Map<String, MagitSingleFolder> foldersMap = new HashMap<>();
    private Map<String, MagitSingleCommit> commitsMap = new HashMap<>();


    public XmlLoader(String xmlFullPath) {
        this.xmlFullPath = xmlFullPath;
        loadMagitRepoFromXml();
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public String getXmlFullPath() {
        return xmlFullPath;
    }

    public MagitRepository getRepo() {
        return repo;
    }

    public Map<String, MagitBlob> getBlobsMap() {
        return blobsMap;
    }

    public Map<String, MagitSingleFolder> getFoldersMap() {
        return foldersMap;
    }

    public Map<String, MagitSingleCommit> getCommitsMap() {
        return commitsMap;
    }

    private void loadMagitRepoFromXml() {
        try (FileInputStream fis = new FileInputStream(xmlFullPath)){
            JAXBContext jaxbContext = JAXBContext.newInstance(MagitRepository.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            repo = (MagitRepository) jaxbUnmarshaller.unmarshal(fis);
        } catch (FileNotFoundException e) {
            System.out.println("Failed to load xml file " + xmlFullPath);
        } catch (JAXBException e) {
            System.out.println("Failed to load xml file " + xmlFullPath);
        } catch (IOException e) {
            System.out.println("Failed to load xml file " + xmlFullPath);
        }
    }

    public boolean isValid() {
        if (repo.getMagitRemoteReference() != null) {
            String remoteMagitRepoLocation = repo.getMagitRemoteReference().getLocation() + "\\.magit";
            File f = new File(remoteMagitRepoLocation);
            if (!f.exists()) {
                errorMessage = "ERROR: the remote repository is not exist";
                return false;
            }
        }

        if (!validateDupIds()) {
            return false;
        }
        if (!validateFoldersPointers()) {
            return false;
        }
        if (!validateCommitsPointers()) {
            return false;
        }
        if (!validateBranchesPointers()) {
            return false;
        }

        return true;
    }

    private boolean validateBranchesPointers() {
        String head = repo.getMagitBranches().getHead();
        boolean flag = false;
        for (MagitSingleBranch b : repo.getMagitBranches().getMagitSingleBranch()) {
            if (!commitsMap.containsKey(b.getPointedCommit().getId())) {
                errorMessage = "ERROR: there is a branch reference to a commit with an undefined id";
                return false;
            }
            if (b.isTracking()) {
                String trackingBranch = b.getTrackingAfter();
                boolean found = false;
                for (MagitSingleBranch tb : repo.getMagitBranches().getMagitSingleBranch()){
                    if (tb.getName().equals(trackingBranch)) {
                        found = true;
                        if (!tb.isIsRemote()){
                            errorMessage = "ERROR: there is a branch that tracking after branch that is not marked as remote";
                            return false;
                        }
                    }
                }

                if(!found) {
                    errorMessage = "ERROR: there is a branch that tracking after branch that is not found";
                    return false;
                }
            }
            if (b.getName().equals(head)) {
                flag = true;
            }
        }
        if (!flag) {
            errorMessage = "ERROR: the head branch points to the name of an undefined branch";
            return false;
        }
        return true;
    }

    private boolean validateCommitsPointers() {
        for (MagitSingleCommit c : commitsMap.values()) {
            if (!foldersMap.containsKey(c.getRootFolder().getId())) {
                errorMessage = "ERROR: there is a commit reference to a folder with an undefined id";
                return false;
            }
            if (!foldersMap.get(c.getRootFolder().getId()).isIsRoot()) {
                errorMessage = "ERROR: there is a commit reference to a folder that is not defined as a root";
                return false;
            }
        }
        return true;
    }

    private boolean validateFoldersPointers() {
        for (MagitSingleFolder f : foldersMap.values()) {
            for (magit.xml.Item item : f.getItems().getItem()) {
                if (item.getType().equals("blob") && !blobsMap.containsKey(item.getId())) {
                    errorMessage = "ERROR: there is a pointer to a blob's id that does not found";
                    return false;
                }
                if (item.getType().equals("folder") && !foldersMap.containsKey(item.getId())) {
                    errorMessage = "ERROR: there is a pointer to a folder's id that does not found";
                    return false;
                }
                //3.5.	יש לוודא כי אין הפניה של folder ל id של עצמו
                if (item.getType().equals("folder") && f.getId().equals(item.getId())) {
                    errorMessage = "ERROR: there is a folder that points to its own id";
                    return false;
                }
            }
        }
        return true;
    }

    private boolean validateDupIds() {
        repo.getMagitBlobs().getMagitBlob().stream().forEach(blob -> blobsMap.put(blob.getId(), blob));
        if (blobsMap.size() !=  repo.getMagitBlobs().getMagitBlob().size()) {
            errorMessage = "ERROR: duplicate id found in blobs";
            return false;
        }
        repo.getMagitFolders().getMagitSingleFolder().stream().forEach(folder -> foldersMap.put(folder.getId(), folder));
        if (blobsMap.size() !=  repo.getMagitBlobs().getMagitBlob().size()) {
            errorMessage = "ERROR: duplicate id found in folders";
            return false;
        }
        repo.getMagitCommits().getMagitSingleCommit().stream().forEach(commit -> commitsMap.put(commit.getId(), commit));
        if (blobsMap.size() !=  repo.getMagitBlobs().getMagitBlob().size()) {
            errorMessage = "ERROR: duplicate id found in commits";
            return false;
        }
        return true;
    }
}
