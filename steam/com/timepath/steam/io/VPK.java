package com.timepath.steam.io;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Loads _dir.vpk files
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
        short dummy = b.getShort();
        if(dummy != ((short) 0xFFFF)) {
            LOG.info("Dummy: " + dummy);
        }
        b.get(new byte[e.preloadBytes]);
    }

    private DefaultMutableTreeNode es;

    private String name;

    public InputStream get(int index) {
        return null;
    }

    public VPK() {
    }

    private static int expectedHeader = 0x55AA1234;

    public VPK load(File file) {
        this.name = file.getName();
        try {
            RandomAccessFile rf = new RandomAccessFile(file, "r");
            ByteBuffer b = rf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, rf.length());
            b.order(ByteOrder.LITTLE_ENDIAN);


            int signature = b.getInt();
            if(signature != expectedHeader) {
                return null;
            }
            int ver = b.getInt();
            // length of directory
            int treeLength = b.getInt(); // XXX: unsigned


            int Unknown1 = 0; // 0 in most
            int FooterLength = 0;
            int Unknown3 = 0; // 48 in most
            int Unknown4 = 0; // 296 in most
            if(ver >= 2) {
                Unknown1 = b.getInt();
                FooterLength = b.getInt();
                Unknown3 = b.getInt();
                Unknown4 = b.getInt();
            }

            entries = new VPKDirectoryEntry[6786];

            Object[][] debug = {
                //                {"signature = ", Integer.toHexString(signature), ", looking for " + Integer.toHexString(expectedHeader)},
                {"ver = ", ver},
                {"length = ", treeLength},
                {"Unknown1 = ", Unknown1},
                {"FooterLength = ", FooterLength},
                {"Unknown3 = ", Unknown3},
                {"Unknown4 = ", Unknown4}
            };

            StringBuilder sb = new StringBuilder();
            for(int l = 0; l < debug.length; l++) {
                for(int x = 0; x < debug[l].length; x++) {
                    sb.append(debug[l][x]);
                }
                sb.append("\n");
            }
            LOG.info(sb.toString());

            parseTree(b);//.get(new byte[treeLength]));
            b.get(new byte[Unknown1]);
            b.get(new byte[FooterLength]);
            b.get(new byte[Unknown3]);
            parse4(b.get(new byte[Unknown4]));

            LOG.log(Level.INFO, "Underflow: {0}", (b.remaining()));

        } catch(IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return this;
    }

    private static String readString(ByteBuffer buf) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for(;;) {
            byte b = buf.get();
            if(b == 0) {
                break;
            }
            baos.write(b);
        }
//        LOG.info(Arrays.toString(baos.toByteArray()) + "");
        return Charset.forName("UTF-8").decode(ByteBuffer.wrap(baos.toByteArray())).toString();
    }

    private VPKDirectoryEntry[] entries;

    /**
     * If a file contains preload data, the preload data immediately follows the above
     * structure.
     * The entire size of a file is PreloadBytes + EntryLength.
     */
    static class VPKDirectoryEntry implements DirectoryEntry {

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
         * If ArchiveIndex is 0x7fff, the offset of the file data relative to the end of the
         * directory (see the header for more details).
         * Otherwise, the offset of the data from the start of the specified archive.
         */
        int entryOffset;

        /**
         * If zero, the entire file is stored in the preload data.
         * Otherwise, the number of bytes stored starting at EntryOffset.
         */
        int entryLength;

        static int terminator = 0xffff; // short

        public int getItemSize() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Object getAttributes() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isDirectory() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getPath() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public Archive getArchive() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public boolean isComplete() {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        public String getName() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public String getAbsoluteName() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public DirectoryEntry[] getImmediateChildren() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public int getIndex() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        public void extract(File out) throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    private static final Logger LOG = Logger.getLogger(VPK.class.getName());

    public ArrayList<DirectoryEntry> find(String search) {
        ArrayList<DirectoryEntry> list = new ArrayList<DirectoryEntry>();
        return list;
    }

    public DirectoryEntry getRoot() {
        return entries[0];
    }

    public void analyze(DefaultMutableTreeNode top, boolean leaves) {
        top.add(es);
    }

    @Override
    public String toString() {
        return this.name;
    }

    /**
     * 0xA0000000
     *
     * @param get
     */
    private void parse4(ByteBuffer get) {
    }

    private void parseTree(ByteBuffer b) {
        int cf = 0;
        int cd = 0;
        int cn = 0;
        // If the file data is stored in the same file as the directory, its offset is (sizeof(header) + TreeLength)
        es = new DefaultMutableTreeNode("root");
        for(;;) {
            String extension = readString(b);
            if(extension.length() == 0) {
                break;
            }
            cf++;
//            DefaultMutableTreeNode e = new DefaultMutableTreeNode(extension);
//            es.add(e);
            for(;;) {
                String path = readString(b);
                if(path.length() == 0) {
                    break;
                }
                cd++;
                DefaultMutableTreeNode p = new DefaultMutableTreeNode(path);
                if(!path.equals(" ")) {
                    es.add(p);
                }
                for(;;) {
                    String filename = readString(b);
                    if(filename.length() == 0) {
                        break;
                    }
                    cn++;
                    if(path.equals(" ")) {
                        LOG.log(Level.FINE, "{0} has no extension", filename);
                        es.add(new DefaultMutableTreeNode(filename));
                    } else {
                        p.add(new DefaultMutableTreeNode(filename + "." + extension));
                    }
                    ReadFileInformationAndPreloadData(b);
                }
            }
        }
        LOG.info("fCount: " + cf);
        LOG.info("dCount: " + cd);
        LOG.info("nCount: " + cn);
    }
}
