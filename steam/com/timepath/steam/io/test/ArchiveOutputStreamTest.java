package com.timepath.steam.io.test;

import com.timepath.steam.SteamUtils;
import com.timepath.steam.io.GCF;
import com.timepath.steam.io.GCF.GCFDirectoryEntry;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class ArchiveOutputStreamTest {

    public static void main(String... args) throws IOException {
        GCF g = new GCF(new File(SteamUtils.getSteamApps(), "Team Fortress 2 Materials.gcf"));
        int index = 7;
        InputStream is = g.get(index);
        File out = new File("/home/timepath/Desktop/good.bin");
        File two = new File("/home/timepath/Desktop/test.bin");
        out.createNewFile();
        two.createNewFile();
        GCFDirectoryEntry e = g.directoryEntries[index];
        e.extract(out);
        InputStream in = e.getArchive().get(e.index);
        FileOutputStream os = new FileOutputStream(two);
        byte[] buffer = new byte[1024];
        int len;
        while((len = in.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        os.close();
    }

    private static final Logger LOG = Logger.getLogger(ArchiveOutputStreamTest.class.getName());
}
