package com.timepath.steam.io.test;

import com.timepath.steam.SteamUtils;
import com.timepath.steam.io.GCF;
import com.timepath.steam.io.GCF.DirectoryEntry;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author timepath
 */
public class ArchiveOutputStreamTest {

    public static void main(String... args) throws IOException {
        GCF g = new GCF(new File(SteamUtils.locateSteamAppsDirectory() + "Team Fortress 2 Materials.gcf"));
        int index = 7;
        InputStream is = g.get(index);
        File out = new File("/home/timepath/Desktop/good.bin");
        File two = new File("/home/timepath/Desktop/test.bin");
        out.createNewFile();
        two.createNewFile();
        DirectoryEntry e = g.directoryEntries[index];
        e.extract(out);
        InputStream in = e.getGCF().get(e.index);
        FileOutputStream os = new FileOutputStream(two);
        byte[] buffer = new byte[1024];
        int len;
        while((len = in.read(buffer)) != -1) {
            os.write(buffer, 0, len);
        }
        os.close();
    }
}
