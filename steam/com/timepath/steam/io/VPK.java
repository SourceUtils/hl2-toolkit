package com.timepath.steam.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;

/**
 *
 * https://developer.valvesoftware.com/wiki/VPK_File_Format
 *
 * @author timepath
 */
public class VPK implements Archive {

    private static void ReadFileInformationAndPreloadData(ByteBuffer b) {
        VPKDirectoryEntry e = new VPKDirectoryEntry();
        e.CRC = b.getInt();
        e.preloadBytes = b.getShort();
        e.archiveIndex = b.getShort();
        e.entryOffset = b.getInt();
        e.entryLength = b.getInt();
    }

    public InputStream get(int index) {
        return null;
    }

    public static VPK load(String s) {
        VPK v = new VPK();
        try {
            RandomAccessFile rf = new RandomAccessFile(s, "r");
            ByteBuffer b = rf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, rf.length());
            b.order(ByteOrder.LITTLE_ENDIAN);

            int signature = b.getInt();
            int ver = b.getInt();
            int treeLength = b.getInt();

            if(ver >= 2) {
                int Unknown1 = b.getInt(); // 0 in CSGO
                int FooterLength = b.getInt();
                int Unknown3 = b.getInt(); // 48 in CSGO
                int Unknown4 = b.getInt(); // 0 in CSGO
            }

            // If the file data is stored in the same file as the directory, its offset is (sizeof(header) + TreeLength)
            for(;;) {
                String extension = readString(b);
                if(extension.length() == 0) {
                    break;
                }
                for(;;) {
                    String path = readString(b);
                    if(path.length() == 0) {
                        break;
                    }
                    for(;;) {
                        String filename = readString(b);
                        if(filename.length() == 0) {
                            break;
                        }
                        ReadFileInformationAndPreloadData(b);
                    }
                }
            }
            
            // Footer

        } catch(IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return v;
    }

    private static String readString(ByteBuffer buf) {
        String str = "";
        for(;;) {
            char c = buf.getChar();
            if(c == 0) {
                break;
            }
            str += c;
        }
        return str;
    }

    /**
     * If a file contains preload data, the preload data immediately follows the above
     * structure.
     * The entire size of a file is PreloadBytes + EntryLength.
     */
    static class VPKDirectoryEntry {

        /**
         * A 32bit CRC of the file's data.
         */
        int CRC;

        /**
         * The number of bytes contained in the index file.
         */
        short preloadBytes;
        
        /**
         * A zero based index of the archive this file's data is contained in.
         * If 0x7fff, the data follows the directory.
         */
        short archiveIndex;

        /**
         * If ArchiveIndex is 0x7fff, the offset of the file data relative to the end of the directory (see the header for more details).
         * Otherwise, the offset of the data from the start of the specified archive.
         */
        int entryOffset;

        /**
         * If zero, the entire file is stored in the preload data.
         * Otherwise, the number of bytes stored starting at EntryOffset.
         */
        int entryLength;

        static int terminator = 0xffff; // short

    }

    private static final Logger LOG = Logger.getLogger(VPK.class.getName());

}
