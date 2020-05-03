package magit.engine;


import java.io.File;
import java.io.InputStream;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.util.zip.ZipFile;


public abstract class Item  {
    protected String name;

    protected String fullPath;

    protected String sha1;

    protected ItemType type;

    private String lastUpdater;

    private String lastUpdateDate;

    private static final String SEPARATOR = "|";

    private String tempRef;

    public Item(String currentLocation, String currentUserName, ItemType type) {
        this.name = Paths.get(currentLocation).getFileName().toString();
        this.fullPath = currentLocation;
        this.type = type;

        lastUpdater = currentUserName;
        File file = new File(currentLocation.toString());
        SimpleDateFormat date = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss:SSS");
        lastUpdateDate = date.format(new Date(file.lastModified()));
    }

    public Item(String fullPath, String[] tokens) {
        this.name = tokens[Field.NAME.ordinal()];
        this.fullPath = fullPath;
        this.sha1 = tokens[Field.SHA1.ordinal()];
        this.type = ItemType.valueOf(tokens[Field.TYPE.ordinal()]);
        this.lastUpdater = tokens[Field.LAST_UPDATER.ordinal()];
        this.lastUpdateDate = tokens[Field.LAST_UPDATE_DATE.ordinal()];
    }

    public Item(String name, String lastUpdate, String author, String sha1Hex, ItemType type) {
        this.name = name;
        this.lastUpdateDate = lastUpdate;
        this.lastUpdater = author;
        this.sha1 = sha1Hex;
        this.type = type;
    }

    public Item(String name, String lastUpdateDate, String lastUpdater, ItemType type, String path) {
        this.name = name;
        this.lastUpdateDate = lastUpdateDate;
        this.lastUpdater = lastUpdater;
        this.fullPath = path;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getFullPath() {
        return fullPath;
    }

    public String getSha1() {
        return sha1;
    }

    public ItemType getType() {
        return type;
    }

    public String getLastUpdater() {
        return lastUpdater;
    }

    public String getLastUpdateDate() {
        return lastUpdateDate;
    }

    public String getTempRef() {
        return tempRef;
    }

    public void setTempRef(String tempRef) {
        this.tempRef = tempRef;
    }

    public void setFullPath(String fullPath) {
        this.fullPath = fullPath;
    }

    public void setLastUpdater(String lastUpdater) {
        this.lastUpdater = lastUpdater;
    }

    protected String toObjectString() {
        return name + SEPARATOR + sha1 + SEPARATOR + type + SEPARATOR + lastUpdater + SEPARATOR + lastUpdateDate;
    }

    private enum Field {
        NAME, SHA1, TYPE, LAST_UPDATER, LAST_UPDATE_DATE
    }

    public abstract void saveObject(String objectsPath);

    public static void loadObject(String objectsPath, String fileName, String wcPath, Folder parent) throws Exception {
        ZipFile zipFile = new ZipFile(objectsPath + File.separator + fileName);
        InputStream inputStream = zipFile.getInputStream(zipFile.entries().nextElement());
        Scanner scanner = new Scanner(inputStream);
        boolean hasMoreLines = true;
        while (hasMoreLines) {
            try {
                String line = scanner.nextLine();
                String[] tokens = line.split("[" + Item.SEPARATOR + "]");
                String itemPath = wcPath + File.separator + tokens[Field.NAME.ordinal()];
                if (tokens[Field.TYPE.ordinal()].equals(ItemType.FOLDER.toString())) {
                    Folder folder = new Folder(itemPath, tokens);
                    parent.addItemToItemsList(folder);
                    loadObject(objectsPath, tokens[Field.SHA1.ordinal()], itemPath, folder);
                } else { //BLOB
                    parent.addItemToItemsList(new Blob(itemPath, tokens));
                }
            } catch (Exception e) {
                hasMoreLines = false;
            }
        }
    }

    protected abstract void loadToWC(String rootPath);

    @Override
    public String toString() {
        return name;
    }
}
