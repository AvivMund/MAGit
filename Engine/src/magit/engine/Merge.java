package magit.engine;

import java.io.File;
import java.util.*;

public class Merge {
    MergeData mergeData;
    Commit ours;
    Commit theirs;
    String repoLocation;
    Folder oursRoot;
    Folder theirsRoot;
    Folder commonRoot;
    WCStatus oursAndCommon;
    WCStatus theirsAndCommon;

    public Merge(Folder oursRoot, Folder theirsRoot, Folder commonRoot,
                 WCStatus oursAndCommon, WCStatus theirsAndCommon, Commit ours, Commit theirs, String repoLocation) {
        this.oursRoot = oursRoot;
        this.theirsRoot = theirsRoot;
        this.commonRoot = commonRoot;
        this.oursAndCommon = oursAndCommon;
        this.theirsAndCommon = theirsAndCommon;
        this.ours = ours;
        this.theirs = theirs;
        this.repoLocation = repoLocation;
        merge();
    }

    public MergeData getMergeData() {
        return mergeData;
    }

    public Commit getOurs() {
        return ours;
    }

    public Commit getTheirs() {
        return theirs;
    }

    private void merge() {
        Map<String, Change> changes = new HashMap<>();
        List<Item> createdOurs = oursAndCommon.getCreated();
        List<Item> createdTheirs = theirsAndCommon.getCreated();
        for (Item i : createdOurs) {
            if(i instanceof Blob) {
                Change c = changes.get(i.getFullPath());
                if (c == null) {
                    c = new Change(i.getFullPath());
                    changes.put(i.getFullPath(), c);
                }
                c.ours = i;
                c.oursType = ChangeType.CREATED;
            }
        }

        for (Item i : createdTheirs) {
            if(i instanceof Blob) {
                Change c = changes.get(i.getFullPath());
                if (c == null) {
                    c = new Change(i.getFullPath());
                    changes.put(i.getFullPath(), c);
                }
                c.theirs = i;
                c.theirsType = ChangeType.CREATED;
            }
        }

        List<Item> updatedOurs = oursAndCommon.getUpdated();
        List<Item> updatedTheirs = theirsAndCommon.getUpdated();
        for (Item i : updatedOurs) {
            if(i instanceof Blob) {
                Change c = changes.get(i.getFullPath());
                if (c == null) {
                    c = new Change(i.getFullPath());
                    changes.put(i.getFullPath(), c);
                }
                c.ours = i;
                c.oursType = ChangeType.UPDATED;
                c.sha1Common = i.getTempRef();
            }
        }

        for (Item i : updatedTheirs) {
            if(i instanceof Blob) {
                Change c = changes.get(i.getFullPath());
                if (c == null) {
                    c = new Change(i.getFullPath());
                    changes.put(i.getFullPath(), c);
                }
                c.theirs = i;
                c.theirsType = ChangeType.UPDATED;
                c.sha1Common = i.getTempRef();
            }
        }

        List<Item> deletedOurs = oursAndCommon.getDeleted();
        List<Item> deletedTheirs = theirsAndCommon.getDeleted();
        for (Item i : deletedOurs) {
            if(i instanceof Blob) {
                Change c = changes.get(i.getFullPath());
                if (c == null) {
                    c = new Change(i.getFullPath());
                    changes.put(i.getFullPath(), c);
                }
                //c.ours = i;
                c.oursType = ChangeType.DELETED;
            }
        }

        for (Item i : deletedTheirs) {
            if(i instanceof Blob) {
                Change c = changes.get(i.getFullPath());
                if (c == null) {
                    c = new Change(i.getFullPath());
                    changes.put(i.getFullPath(),c);
                }
                //c.theirs = i;
                c.theirsType = ChangeType.DELETED;
            }
        }

        List<String> resolved = new ArrayList<>();
        for (Change c: changes.values()) {
            if (resolve(c)) {
                resolved.add(c.path);
            }
        }

        for (String p: resolved){
            changes.remove(p);
        }

        if (changes.isEmpty()) {
            mergeData = new MergeData("No conflicts!", changes);
        } else {
            mergeData = new MergeData("Conflicts found, please resolve", changes);
        }
    }

    private boolean resolve(Change c) {
        if (c.oursType == ChangeType.CREATED && c.theirsType == ChangeType.CREATED
        || c.oursType == ChangeType.UPDATED && c.theirsType == ChangeType.UPDATED
        || c.oursType == ChangeType.UPDATED && c.theirsType == ChangeType.DELETED
        || c.oursType == ChangeType.DELETED && c.theirsType == ChangeType.UPDATED) {
            return false;
        }
        if (c.oursType == null) {
            handleTheirs(c);
            return true;
        }
        return true;
    }

    private void handleTheirs(Change c) {
        if (c.theirsType == ChangeType.DELETED) {
            File file = new File(c.path);
            file.delete();
        } else {
            c.theirs.loadToWC(repoLocation);
        }
    }
}
