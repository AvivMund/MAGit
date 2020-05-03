package magit.engine;

import magit.engine.zip.SingleFileZipOutputStream;
import magit.xml.MagitSingleCommit;
import org.apache.commons.codec.digest.DigestUtils;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipFile;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Commit {

    @XmlAttribute
    private String sha1;

    @XmlElement
    private String previousCommit1Sha1 = "";

    @XmlElement
    private String previousCommit2Sha1 = "";

    @XmlElement
    private String message;

    @XmlAttribute
    private String author;

    @XmlElement
    private String date;

    public Commit() {
        this.sha1 = null;
        this.previousCommit1Sha1 = null;
        this.message = null;
        this.author = null;
        this.date = null;
    }

    public Commit(String commitMessage, String rootFolderSha1, String currentUserName, String prevCommit1Sha1, String prevCommit2Sha1) {
        this.sha1 = rootFolderSha1;
        this.previousCommit1Sha1 = prevCommit1Sha1;
        this.previousCommit2Sha1 = prevCommit2Sha1;
        this.message = commitMessage;
        this.author = currentUserName;
        SimpleDateFormat formatDateOfCreation = new SimpleDateFormat("dd.MM.yyyy-HH:mm:ss:SSS");
        this.date = formatDateOfCreation.format(new Date());
    }

    public Commit(MagitSingleCommit c, String sha1) {
        this.sha1 = sha1;
        this.message = c.getMessage();
        this.author = c.getAuthor();
        this.date = c.getDateOfCreation();
    }

    public String getSha1() {
        return sha1;
    }

    public String getPreviousCommit1Sha1() {
        return previousCommit1Sha1;
    }

    public String getPreviousCommit2Sha1() {
        return previousCommit2Sha1;
    }

    public String getMessage() {
        return message;
    }

    public String getAuthor() {
        return author;
    }

    public String getDate() {
        return date;
    }

    public void setPreviousCommit1Sha1(String previousCommit1Sha1) {
        this.previousCommit1Sha1 = previousCommit1Sha1;
    }

    protected static Commit loadCommitObject(String filename) {
        try (ZipFile zipFile = new ZipFile(filename)) {
            InputStream inputStream = zipFile.getInputStream(zipFile.entries().nextElement());

            JAXBContext jaxbContext = JAXBContext.newInstance(Commit.class);
            Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();
            return (Commit) jaxbUnmarshaller.unmarshal(inputStream);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("problem with load commit");
            return null;
        }
    }

    protected void saveObject(String objectsLocation) {
        String sha1 = calculateSha1();
        try (SingleFileZipOutputStream zipOutputStream =
                     new SingleFileZipOutputStream(new FileOutputStream(objectsLocation + File.separator + sha1), sha1)) {
            JAXBContext jaxbContext = JAXBContext.newInstance(Commit.class);
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            jaxbMarshaller.marshal(this, zipOutputStream);
        } catch (JAXBException e) {
            System.out.println("Problem when saving to " + objectsLocation + File.separator + sha1);
            return;
        } catch (IOException e) {
            System.out.println("Problem when saving to " + objectsLocation + File.separator + sha1);
            return;
        }
    }

    public String calculateSha1() {
        return DigestUtils.sha1Hex(sha1 + previousCommit1Sha1 + message + author + date);
    }

}
