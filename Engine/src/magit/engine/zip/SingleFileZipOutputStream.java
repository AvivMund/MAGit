package magit.engine.zip;

import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SingleFileZipOutputStream extends ZipOutputStream {
    public SingleFileZipOutputStream(OutputStream out, String fileName) throws IOException {
        super(out);
        ZipEntry entry = new ZipEntry(fileName);
        this.putNextEntry(entry);
    }
}
