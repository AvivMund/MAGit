package magit.engine;

import magit.engine.zip.SingleFileZipOutputStream;
import magit.xml.MagitSingleFolder;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Folder extends Item {
    private List<Item> items = new ArrayList<>();

    public Folder(String fullPath, String[] tokens) {
        super(fullPath, tokens);
    }

    public Folder(String currentLocation, String currentUserName) {
        super(currentLocation, currentUserName, ItemType.FOLDER);
    }

    public Folder(MagitSingleFolder folder, String path) {
        super(Paths.get(path).getFileName().toString(), folder.getLastUpdateDate(), folder.getLastUpdater(), ItemType.FOLDER, path);
    }

    public List<Item> getItems() {
        return items;
    }

    public void calculateSha1(){
        Collections.sort(items, new Comparator<Item>() {
            @Override
            public int compare(Item o1, Item o2) {
                return o1.name.compareTo(o2.name); }
            });

        StringBuilder content = new StringBuilder();
        for (Item curr : items) {
            content.append(curr.getName() + curr.getSha1() + curr.getType());
        }

        this.sha1 = DigestUtils.sha1Hex(content.toString());
    }

    public void addItemToItemsList(Item item) {
        items.add(item);
    }

    @Override
    public void saveObject(String objectsPath) {
        try (SingleFileZipOutputStream zipOutputStream =
                     new SingleFileZipOutputStream(new FileOutputStream(objectsPath + File.separator + sha1), sha1);
             PrintWriter pw = new PrintWriter(zipOutputStream))
        {
            for (Item item : items) {
                pw.println(item.toObjectString());
            }
        } catch (IOException e) {
            System.out.println("Problem when saving to " + objectsPath + File.separator + sha1);

        }
    }

    @Override
    protected void loadToWC(String rootPath) {
        try {
            Files.createDirectories(Paths.get(fullPath));
        } catch (IOException e) {
            System.out.println("Problem when creating directory " + fullPath + " in working copy tree");
        }
        List<Item> children = this.getItems();
        for (Item child : children) {
            child.loadToWC(rootPath);
        }
    }
}
