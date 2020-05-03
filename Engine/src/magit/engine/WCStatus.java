package magit.engine;

import java.util.ArrayList;
import java.util.List;

public class WCStatus {
    private List<Item> deleted = new ArrayList<>();
    private List<Item> updated = new ArrayList<>();
    private List<Item> created = new ArrayList<>();

    public WCStatus(Item wcTree, Item headTree) {
        findChanges(wcTree, headTree);
        deleted.sort((i1, i2) -> i1.toString().compareTo(i2.toString()));
        updated.sort((i1, i2) -> i1.toString().compareTo(i2.toString()));
        created.sort((i1, i2) -> i1.toString().compareTo(i2.toString()));
    }

    public List<Item> getDeleted() {
        return deleted;
    }

    public List<Item> getUpdated() {
        return updated;
    }

    public List<Item> getCreated() {
        return created;
    }

    private void findChanges(Item wcTree, Item headTree) {
        if (wcTree != null && headTree != null && wcTree.sha1.equals(headTree.sha1)) {
            return;
        }
        if (wcTree == null) {
            deleted.add(headTree);
            if (headTree instanceof Blob) {
                return;
            }
            //send my children wcTree = null
            iterateHeadItems((Folder) headTree);
        }
        if (headTree == null) {
            created.add(wcTree);
            if (wcTree instanceof Blob) {
                return;
            }
            //send my children with headTree = null
            iterateWcItems((Folder) wcTree);
        }
        if (wcTree != null && headTree != null) {
            if (wcTree instanceof Folder && headTree instanceof Folder) {
//                updated.add(wcTree);
                List<Item> wcItems = ((Folder) wcTree).getItems();
                List<Item> headItems = ((Folder) headTree).getItems();
                int w = 0;
                int h = 0;
                while ( w < wcItems.size() && h < headItems.size()) {
                    String wName = wcItems.get(w).name;
                    String hName = headItems.get(h).name;
                    if (wName.compareTo(hName) < 0) {
                        findChanges(wcItems.get(w), null);
                        w++;
                    } else if (wName.compareTo(hName) > 0) {
                        findChanges(null, headItems.get(h));
                        h++;
                    } else { // equals
                        wcItems.get(w).setLastUpdater(headItems.get(h).getLastUpdater());
                        findChanges(wcItems.get(w), headItems.get(h));
                        w++;
                        h++;
                    }
                }
                while ( w < wcItems.size()) {
                    findChanges(wcItems.get(w), null);
                    w++;
                }
                while ( h < headItems.size()) {
                    findChanges(null, headItems.get(h));
                    h++;
                }
                wcTree.setTempRef(headTree.sha1);
                updated.add(wcTree);
            } else {
                if (headTree instanceof Blob && wcTree instanceof Blob) {
                    wcTree.setTempRef(headTree.sha1);
                    updated.add(wcTree);
                } else if (headTree instanceof Blob){
                    created.add(wcTree);
                    deleted.add(headTree);
                    iterateWcItems((Folder) wcTree);
                } else { // wcTree instanceof Blob
                    created.add(wcTree);
                    deleted.add(headTree);
                    iterateHeadItems((Folder) headTree);
                }
            }
        }
    }

    private void iterateWcItems(Folder wcTree) {
        for (Item item : wcTree.getItems()) {
            findChanges(item, null);
        }
    }

    private void iterateHeadItems(Folder headTree) {
        for (Item item : headTree.getItems()) {
            findChanges(null, item);
        }
    }

    public boolean isEmpty() {
        return created.isEmpty() && deleted.isEmpty() && updated.isEmpty();
    }

    public void saveChanges(String objectsPath) {
        for (Item item : created) {
            item.saveObject(objectsPath);
        }
        for (Item item : updated) {
            item.saveObject(objectsPath);
        }
    }
}
