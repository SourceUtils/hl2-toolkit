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
 * https://github.com/DHager/hl2parse/blob/master/hl2parse-binary/src/main/java/com/technofovea/hl2parse/registry/RegParser.java
 *
 * Nodes of name \1 contain 'folders', \2 are 'files'. Leaf \2 directories contain a key and a
 * value node
 * TODO: Load keys and values
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

        private ByteBuffer descriptor;
        
        public boolean isMeta() {
            return header != null;
        }

        public void setDescriptor(ByteBuffer nameBuffer) {
            this.descriptor = nameBuffer;
        }

        public ByteBuffer getDescriptor() {
            return descriptor;
        }

        private ByteBuffer payload;

        public void setPayload(ByteBuffer payload) {
            this.payload = payload;
        }

        public ByteBuffer getPayload() {
            return payload;
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

        private NodeType header;

        /**
         * @return the header
         */
        public NodeType getHeader() {
            return header;
        }

        /**
         * @param header the header to set
         */
        public void setHeader(NodeType header) {
            this.header = header;
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    private static enum FolderType {

        DECOMPRESED(0x50),
        COMPRESSED(0x43);

        FolderType(int i) {
            this.i = i;
        }

        private int i;

        public int ID() {
            return i;
        }

        public static FolderType get(int i) {
            for(FolderType t : FolderType.values()) {
                if(t.ID() == i) {
                    return t;
                }
            }
            return null;
        }
    }

    private static enum DataType {

        TEXT(0x00),
        DWORD(0x01),
        RAW(0x02);

        DataType(int i) {
            this.i = i;
        }

        private int i;

        public int ID() {
            return i;
        }

        public static DataType get(int i) {
            for(DataType t : DataType.values()) {
                if(t.ID() == i) {
                    return t;
                }
            }
            return null;
        }
    }

    private static enum NodeType {

        FOLDER(0x01),
        FILE(0x02);

        NodeType(int i) {
            this.i = i;
        }

        private int i;

        public int ID() {
            return i;
        }

        public static NodeType get(int i) {
            for(NodeType t : NodeType.values()) {
                if(t.ID() == i) {
                    return t;
                }
            }
            return null;
        }
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Utils">
    public static void analyze(File f, DefaultMutableTreeNode root) throws IOException {
        BlobNode bn = load(f);
        recurse(bn, root);
    }

    public static BlobNode load(File f) throws IOException {
        BlobNode bn = new BlobNode();
        parse(mapFile(f), bn);
        return bn;
    }

    //<editor-fold defaultstate="collapsed" desc="Slices">
    private static ByteBuffer getSlice(ByteBuffer source) {
        return readSlice(source, source.remaining());
    }

    /**
     * Reads length bytes ahead, turns them into a ByteBuffer, and then jumps there
     *
     * @param source
     * @param length
     *
     * @return
     */
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

    private static boolean verbose = false;

    private static void recurse(BlobNode bn, DefaultMutableTreeNode parent) {
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(bn);
        parent.add(node);
        for(BlobNode c : bn.children) {
            if(c.isMeta() && !verbose) {
                for(BlobNode c2 : c.children) {
                    recurse(c2, node);
                }
            } else {
                recurse(c, node);
            }
        }
    }
    //</editor-fold>

    private static int NULL_TERMINATOR = 0x00;

    private static String getText(ByteBuffer source) {
        int pos = source.position();
        int end = source.limit();
//        while(source.remaining() > 0) {
//            if(source.get() == NULL_TERMINATOR) { // Check for null terminator
//                end = source.position() - 1;
//                break;
//            }
//        }
        source.position(pos);
        source.limit(end);

        return Charset.forName("UTF-8").decode(source).toString();
    }

    private static void parse(ByteBuffer buf, BlobNode parent) throws BufferUnderflowException {
        if(parent.isMeta()) {
            switch(parent.getHeader()) {
                case FOLDER: // Data type
                    int i = parent.getPayload().getInt();
                    parent.getPayload().position(0);
                    DataType dt = DataType.get(i);
                    if(dt == null) {
                        break;
                    }
                    parent.addChild(new BlobNode(dt.name()));
                    return;
//                case FILE: // Data
//
//                    return;
            }
        }
        int kindNum = buf.get();
        NodeType kind = NodeType.get(kindNum);
        if(kind == null) {
//            LOG.log(Level.WARNING, "Unknown data kind {0}", Integer.toHexString(kindNum));
            buf.position(0);
            return;
        }
        LOG.log(Level.FINE, "Data kind {0}", kind);
        switch(kind) {
            case FOLDER: // 01
                int typeNum = buf.get();
                if(typeNum == 0) {
                    buf.get(new byte[2]);
                    parent.addChild(new BlobNode(getText(getSlice(buf))));
                }
                FolderType type = FolderType.get(typeNum);
                if(type == null) {
//                    LOG.log(Level.WARNING, "Unknown folder type {0}", Integer.toHexString(typeNum));
                    buf.position(0);
                    return;
                }
                switch(type) {
                    case COMPRESSED: // 43
                        parse(decompress(buf), parent); // repeat
                        break;
                    case DECOMPRESED: // 50
                        int totalLen = buf.getInt(); // total size of blob
                        int padding = buf.getInt();
                        padding = 0;
                        buf.limit((buf.position() - 10) + totalLen + padding); // 10 = padding (4) + totalLen (4) + header (2)
                        ByteBuffer nodes = readSlice(buf, buf.remaining() - padding);
                        int c = 0;
                        while(nodes.remaining() > 0) {
                            c++;
                            BlobNode node = parseNode(nodes);
                            parse(node.getPayload(), node);
                            parent.addChild(node);
                        }
                        parent.setName(parent.getName() + " (" + c + ")");
                        if(buf.remaining() != padding) {
                            LOG.log(Level.WARNING, "** Bytes missed: {0}", buf.remaining() - padding);
                        }
                        break;
                    default:
                        LOG.log(Level.INFO, "Folder type {0}", type);
                        break;
                }
                break;
            case FILE:
                int fileTypeNum = buf.get();
//                if(fileTypeNum == 0) {
//                    buf.get(new byte[2]);
//                    parent.addChild(new BlobNode(getText(getSlice(buf))));
//                }
                break;
            default:
                LOG.log(Level.WARNING, "** Unexpected data kind {0}", Integer.toHexString(kindNum));
                break;
        }
    }

    private static BlobNode parseNode(ByteBuffer buf) {
        BlobNode node = new BlobNode();

        short descriptorLen = buf.getShort();
        int payloadLen = buf.getInt();

        node.setDescriptor(readSlice(buf, descriptorLen));
        node.setPayload(readSlice(buf, payloadLen));

        node.getDescriptor().position(0);
        int descInt = node.getDescriptor().get();
        node.setHeader(NodeType.get(descInt));
        node.getDescriptor().position(0);
        node.setName(getText(node.getDescriptor()).replaceAll("\1", "<Folder>").replaceAll("\2", "<File>"));

        return node;
    }

    /**
     * Compressed header: int compressed + 20 (size of header neglecting initial
     * short) int dummy1 int decompressed int dummy2 short dummy3
     * byte[compressed] perform zlib decompression (skip the first 2 bytes) to
     * get byte[decompressed]
     *
     * @param originalBuffer compressed blob
     *
     * @return the originalBufffer decompressed
     */
    private static ByteBuffer decompress(ByteBuffer originalBuffer) {
        try {
            LOG.log(Level.FINE, "Inflating a compressed binary section, initial length (including header) is {0}", originalBuffer.remaining());
            int headerSkip = 2;
            Inflater inflater = new Inflater(true);

            ByteBuffer mybuf = getSlice(originalBuffer);

            // Includes length of magic header etc?
            int wholeLen = mybuf.getInt(); // Includes bytes starting with itself
            int compressedLen = wholeLen - 20;
            int x1 = mybuf.getInt();
            int decompressedLen = mybuf.getInt();
            int x2 = mybuf.getInt();
            int compLevel = mybuf.getShort();

            LOG.log(Level.FINE, "Header claims payload compressed length is {0}, deflated length is {1}, compression level {2}", new Object[]{compressedLen, decompressedLen, compLevel});

            if(mybuf.remaining() < compressedLen) {
                LOG.log(Level.WARNING, "The buffer remainder is too small ({0}) to contain the amount of data the header specifies ({1}).", new Object[]{mybuf.remaining(), compressedLen});
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