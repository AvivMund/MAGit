package magit.engine;

import magit.engine.zip.SingleFileZipInputStream;
import magit.engine.zip.SingleFileZipOutputStream;
import magit.xml.MagitBlob;
import org.apache.commons.codec.digest.DigestUtils;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Blob extends Item {
    //private File file;

    public Blob(String currentLocation, String currentUserName) {
        super(currentLocation, currentUserName, ItemType.BLOB);
        File file = new File(currentLocation);
        try (FileInputStream fis = new FileInputStream(file)){
            sha1 = DigestUtils.sha1Hex(fis);
        } catch (IOException e) {
            System.out.println("Failed to open file " + currentLocation + " for sha1 computation");
        }
    }

    public Blob(MagitBlob mb) { //c'tor for MagitBLOB
        super(mb.getName(), mb.getLastUpdateDate(), mb.getLastUpdater(), DigestUtils.sha1Hex(mb.getContent()), ItemType.BLOB);
    }

    public Blob(String currentLocation, String[] tokens) {
        super(currentLocation, tokens);
    }

    @Override
    public void saveObject(String objectsPath) {
        try (FileInputStream fileInputStream = new FileInputStream(fullPath)) {
            saveObject(objectsPath, fileInputStream);
        } catch (FileNotFoundException e) {
            System.out.println("Problem when reading from " + fullPath);
        } catch (IOException e) {
            System.out.println("Problem when reading from " + fullPath);
        }
    }

    public void saveObject(String objectsPath, String content) {
        saveObject(objectsPath, new ByteArrayInputStream(content.getBytes()));
    }

    private void saveObject(String objectsPath, InputStream inputStream) {
        try (SingleFileZipOutputStream zipOutputStream = new SingleFileZipOutputStream(new FileOutputStream(objectsPath + File.separator + sha1), sha1)) {
            byte buff[] = new byte[1024];
            int numRead = 0;
            while (numRead >= 0) {
                numRead = inputStream.read(buff);
                if (numRead > 0) {
                    zipOutputStream.write(buff, 0, numRead);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Problem when saving to " + objectsPath + File.separator + sha1);
        } catch (IOException e) {
            System.out.println("Problem when saving to " + objectsPath + File.separator + sha1);
        }
    }

    @Override
    protected void loadToWC(String rootPath) {
        try (SingleFileZipInputStream zipInputStream =
                     new SingleFileZipInputStream(new FileInputStream(rootPath + RepoManager.OBJECTS_PATH + File.separator + sha1));
             FileOutputStream fileOutputStream = new FileOutputStream(fullPath)) {
            byte buff[] = new byte[1024];
            int numRead = 0;
            while (numRead >= 0) {
                numRead = zipInputStream.read(buff);
                if (numRead > 0) {
                    fileOutputStream.write(buff, 0, numRead);
                }
            }
            fileOutputStream.flush();
        } catch (FileNotFoundException e) {
            System.out.println("Problem when loading " + fullPath + " from repo");
        } catch (IOException e) {
            System.out.println("Problem when loading " + fullPath + " from repo");
        }
    }

    public static String getContent(String rootPath, String sha1) throws IOException {
        ZipFile zip = new ZipFile(rootPath + RepoManager.OBJECTS_PATH + File.separator + sha1);
        ZipEntry entry = zip.entries().nextElement();
        StringBuilder out = new StringBuilder();
        if (!entry.isDirectory()) {
            out = getTxtFiles(zip.getInputStream(entry));
        }

        return out.toString();
    }

    private static StringBuilder getTxtFiles(InputStream in)  {
        StringBuilder out = new StringBuilder();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line;
        try {
            while ((line = reader.readLine()) != null) {
                out.append(line);
                out.append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("ERROR: problem with file content");
            return out;
        }
        return out;
    }
}
