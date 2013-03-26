package com.timepath.steam.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * http://trac.assembla.com/clientregistrytoolkit/browser/trunk/SteamReg/Blob
 * http://cs.rin.ru/forum/viewtopic.php?f=29&t=44155
 * https://github.com/DHager/hl2parse/blob/master/hl2parse-binary/src/main/java/com/technofovea/hl2parse/registry/RegParser.java
 *
 * Nodes of name \1 contain 'folders', \2 are 'files'. Files contain a \1 and a \2 directory; KeyValues
 * 
 * @author timepath
 */
public class Blob {
    
    private static final Logger LOG = Logger.getLogger(Blob.class.getName());
    
    //<editor-fold defaultstate="collapsed" desc="Inner classes">
    public static class BlobNode {
        
        public BlobNode() {
        }
        
        public BlobNode(String s) {
            this.setName(s);
        }
        private ArrayList<BlobNode> children = new ArrayList<BlobNode>();
        
        public void addChild(BlobNode c) {
            children.add(c);
        }
        
        public BlobNode getChild(int i) {
            return children.get(i);
        }
        
        public int childCount() {
            return children.size();
        }
        private String name;
        
        public void setName(String s) {
            this.name = s;
        }
        
        public String getName() {
            return this.name;
        }
        private FolderItemType header;
        
        /**
         * @return the header
         */
        public FolderItemType getHeader() {
            return header;
        }
        
        /**
         * @param header the header to set
         */
        public void setHeader(FolderItemType header) {
            this.header = header;
        }
        
        @Override
        public String toString() {
            return getName();
        }
        
    }
    
    private static enum DataType {
        TEXT(0x00),
        DWORD(0x01),
        RAW(0x02),
        DECOMPRESED(0x50),
        COMPRESSED(0x43);
        
        DataType(int i) {
            this.i = i;
        }
        
        private int i;
        
        public int ID() {
            return i;
        }
        
        public static DataType get(int i) {
            for(DataType t : DataType.values()){
                if(t.ID() == i) {
                    return t;
                }
            }
            return null;
        }
        
    }
    
    private static enum FolderItemType {
        FOLDER(0x01),
        FILE(0x02);
        
        FolderItemType(int i) {
            this.i = i;
        }
        
        private int i;
        
        public int ID() {
            return i;
        }
        
        public static FolderItemType get(int i) {
            for(FolderItemType t : FolderItemType.values()){
                if(t.ID() == i) {
                    return t;
                }
            }
            return null;
        }
        
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Public">
    public static void analyze(File f, DefaultMutableTreeNode root) throws IOException {
        BlobNode bn = new BlobNode();
        parse(mapFile(f), bn);
        recurse(bn, root);
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Utils">
    //<editor-fold defaultstate="collapsed" desc="Slices">
    private static ByteBuffer readSlice(ByteBuffer source) {
        return readSlice(source, source.remaining());
    }

    private static ByteBuffer readSlice(ByteBuffer source, int length) {
        int originalLimit = source.limit();
        source.limit(source.position() + length);
        ByteBuffer sub = source.slice();
        source.position(source.limit());
        source.limit(originalLimit);
        sub.order(ByteOrder.LITTLE_ENDIAN);
        return sub;
    }
    //</editor-fold>
    
    private static ByteBuffer mapFile(File f) throws IOException {
        FileInputStream fis = new FileInputStream(f);
        FileChannel fc = fis.getChannel();
        MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, f.length());
        mbb.order(ByteOrder.LITTLE_ENDIAN);
        return mbb;
    }
    
    private static void recurse(BlobNode bn, DefaultMutableTreeNode tn) {
        DefaultMutableTreeNode t = new DefaultMutableTreeNode(bn);
        tn.add(t);
        ArrayList<Blob.BlobNode> c = bn.children;
        for(int i = 0; i < c.size(); i++) {
            recurse(c.get(i), t);
        }
    }
    
    private static void parse(ByteBuffer buf, BlobNode parent) throws BufferUnderflowException {
        if(buf == null) {
            return;
        }
        
        ByteBuffer mybuf = readSlice(buf);
        
        int kindNum = mybuf.get();
        FolderItemType kind = FolderItemType.get(kindNum);
        if(kind == null) {
//            LOG.log(Level.WARNING, "Unknown data kind {0}", Integer.toHexString(kindNum));
            if(parent.getHeader() == FolderItemType.FOLDER) { // reading value
                byte[] b = new byte[mybuf.remaining()];
                if(b.length < 256) {
                    String s = null;

                    int typeNum = mybuf.get();
                    DataType type = DataType.get(typeNum);
                    if(type != null) {
                        s = type.toString();
                    }
                    if(s == null) {
                        mybuf.position(0);
                        mybuf.get(b);
                        s = "[";
                        for(int i = 0; i < b.length; i++) {
                            if(i != 0) {
                                s += " ";
                            }
                            s += Integer.toHexString(b[i] & 0xff);
                        }
                        s += "]";
                    }
                    parent.addChild(new BlobNode("VALUE: " + s));
                }
            } else 
            if(parent.getHeader() == FolderItemType.FILE) { // reading value name
                BlobNode v = new BlobNode("TYPE");
                v.setHeader(FolderItemType.FOLDER);
                parse(mybuf, v);
                parent.addChild(v);
            }
            return;
        }
        LOG.log(Level.FINE, "Data kind {0}", kind);
        
        switch(kind) {
            case FOLDER:
                int typeNum = mybuf.get();
                DataType type = DataType.get(typeNum);
                if(type == null) {
//                    LOG.log(Level.WARNING, "Unknown data type {0}", Integer.toHexString(typeNum));
                    return;
                } else {
//                    LOG.log(Level.INFO, "Data type {0}", type);
                }
                switch(type) {
                    case COMPRESSED:
                        parse(decompress(mybuf), parent);
                        break;
                    case DECOMPRESED:
                        parseDecompressed(mybuf, parent);
                        break;
                }
                break;
            case FILE:
//                byte[] data = mybuf.array();
                break;
            default:
                LOG.log(Level.WARNING, "** Unexpected data kind {0}", Integer.toHexString(kindNum));
            break;
        }
    }
    
    private static String getText(ByteBuffer source) {
        int pos = source.position();
        int end = source.limit();
        while(source.remaining() > 0) {
            if(source.get() == 0x00) { // NULL_TERMINATOR
                end = source.position() - 1;
                break;
            }
        }
        source.position(pos);
        source.limit(end);

        return Charset.forName("UTF-8").decode(source).toString();
    }
    
    private static void parseDecompressed(ByteBuffer buf, BlobNode parent) {
        try {
            ByteBuffer mybuf = readSlice(buf);

            int totalLen = mybuf.getInt(); // total size of blob
            int padding = mybuf.getInt();
            
            mybuf.limit(mybuf.position() + totalLen - 10 + padding);
            while(mybuf.remaining() > padding) {
                BlobNode child = new BlobNode();
            
                int descriptorLen = mybuf.getShort();
                int payloadLen = mybuf.getInt(); // remaining bytes

                ByteBuffer desc = readSlice(mybuf, descriptorLen);
                ByteBuffer payload = readSlice(mybuf, payloadLen);
                
                String name = getText(desc);
                child.setName(name.replaceAll("\1", "Folder{").replaceAll("\2", "File{"));
                desc.rewind();
                desc.position(0);
                
                int typeNum = desc.get(); // foldable?
                desc.position(0);
                payload.position(0);
                FolderItemType thisType = FolderItemType.get(typeNum);
                if(thisType == null) { // regular named directory
                    parent.children.add(child);
                    parse(payload, child); // enumerate the children on any subdirectories to this node
                } else {
                    child.setHeader(thisType);
                    switch(thisType) {
                        case FOLDER: // Contains normal directories
                        case FILE: // Contains data entries
                            boolean verbose = false;
                            if(!verbose) {
                                parse(payload, parent);
                            } else {
                                parent.children.add(child);
                                parse(payload, child);
                            }
                            break;
                        default:
                            LOG.log(Level.WARNING, "Unexpected folder item type: {0}", typeNum);
                            break;
                    }
                }
                
            }
            
            mybuf.position(mybuf.position() + padding);
            if(mybuf.remaining() != 0) {
                LOG.log(Level.WARNING, "** Underflow: {0}", mybuf.remaining());
            }
        } catch(BufferUnderflowException bue) {
//            LOG.log(Level.SEVERE, "Buffer Underflow");//, bue);
        }
    }

    /**
     * Compressed header: int compressed + 20 (size of header neglecting initial
     * short) int dummy1 int decompressed int dummy2 short dummy3
     * byte[compressed] perform zlib decompression (skip the first 2 bytes) to
     * get byte[decompressed]
     *
     * @param originalBuffer compressed blob
     * @return the originalBufffer decompressed
     */
    private static ByteBuffer decompress(ByteBuffer originalBuffer) {
        LOG.log(Level.FINE, "Inflating a compressed binary section, initial length (including header) is {0}", originalBuffer.remaining());
        int headerSkip = 2;
        Inflater inflater = new Inflater(true);

        try {
            ByteBuffer mybuf = readSlice(originalBuffer);

            // Includes length of magic header etc?
            int wholeLen = mybuf.getInt(); // Includes bytes starting with itself
            int compressedLen = wholeLen - 20;
            int x1 = mybuf.getInt();
            int decompressedLen = mybuf.getInt();
            int x2 = mybuf.getInt();
            int compLevel = mybuf.getShort();

            LOG.log(Level.FINE, "Header claims payload compressed length is {0}, deflated length is {1}, compression level {2}", new Object[] {compressedLen, decompressedLen, compLevel});

            if(mybuf.remaining() < compressedLen) {
                LOG.log(Level.WARNING, "The buffer remainder is too small ({0}) to contain the amount of data the header specifies ({1}).", new Object[] {mybuf.remaining(), compressedLen});
            }

            mybuf.limit(mybuf.position() + compressedLen);

            byte[] compressed = new byte[mybuf.remaining()];
            mybuf.get(compressed);
            inflater.setInput(compressed, headerSkip, compressed.length - headerSkip);
            byte[] decompressed = new byte[decompressedLen];
            try {
                LOG.fine("Beginning decompression");
                inflater.inflate(decompressed);
                LOG.fine("Decompression successful");
            } catch(DataFormatException ex) {
                ex.printStackTrace();
            }

            ByteBuffer newBuf = ByteBuffer.wrap(decompressed);
            newBuf.order(ByteOrder.LITTLE_ENDIAN);

            return newBuf;
        } catch(BufferUnderflowException bue) {
            LOG.log(Level.SEVERE, "Buffer Underflow");//, bue);
        }
        return null;
    }
    
}