package magit.engine.zip;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipInputStream;

public class SingleFileZipInputStream extends ZipInputStream {
    public SingleFileZipInputStream(InputStream in) throws IOException {
        super(in);
        if (this.getNextEntry() == null) {
            throw new FileNotFoundException("No Entries in Zip file");
        }
    }
}
