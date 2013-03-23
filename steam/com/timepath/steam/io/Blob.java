package com.timepath.steam.io;

import com.timepath.DataUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
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
 * Nodes of name \1 contain 'folders', \2 are 'files' (KeyValues)
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
        private int header;
        
        /**
         * @return the header
         */
        public int getHeader() {
            return header;
        }
        
        /**
         * @param header the header to set
         */
        public void setHeader(int header) {
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
        DIRECTORY(0x50),
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
    
    //<editor-fold defaultstate="collapsed" desc="Tabbing">
    private static int level = 0;

    private static String tab(int t) {
        String s = "";
        for(int i = 0; i < t; i++) {
            s += "  ";
        }
        return s;
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
    
    //<editor-fold defaultstate="collapsed" desc="Legacy code">
    private static void legacy(RandomAccessFile rf, Blob.BlobNode parent) throws IOException {
        int totalLength = DataUtils.readULEInt(rf); // Length of entire data node: header + data
        int nullPadding = DataUtils.readULEInt(rf);
        int descLength = DataUtils.readULEShort(rf);
        int dataLength = DataUtils.readULEInt(rf); // Length of all child data, header(10) inclusive
        byte[] nameArray = DataUtils.readBytes(rf, descLength);
        String name = new String(nameArray);

        int headLength = 2 + 4 + 4 + 2 + 4 + nameArray.length; // minimum 20
        long offset = rf.getFilePointer() - headLength;
        long dataStart = offset + headLength;
        long dataEnd = dataStart + dataLength;

        parent.setName("'" + name.replaceAll("\1", "[1]").replaceAll("\2", "[2]") + "', hS: " + offset + ", hL: " + headLength + ", hE/dS: " + dataStart + ", dL: " + dataLength + ", dE: " + dataEnd + " | bL: " + totalLength + ", bE: " + (offset + totalLength));

        //        if(dataLength >= 20) {
        System.out.println(tab(level) + ">> " + parent.getName());
        level++;
        while(rf.getFilePointer() < dataEnd) {
            Blob.BlobNode child = null;// = readBlob(rf, new Blob.BlobNode());

            if(child.getName() != null) {
                parent.children.add(child);
            }
        }
        level--;
        System.out.println(tab(level) + "<< " + parent.getName());
        //        }
        rf.seek(dataEnd);
        //        if(rf.getFilePointer() != dataEnd) {
        //            LOG.log(Level.WARNING, "{0} finished in wrong position ({1} should be {2})", new Object[]{bn.getName(), rf.getFilePointer(), dataEnd});
        //            rf.seek(dataEnd);
        //        }

    }
    //</editor-fold>
    //</editor-fold>
    
    private static void parse(ByteBuffer buf, BlobNode parent) throws BufferUnderflowException {
        if(buf == null) {
            return;
        }
        ByteBuffer mybuf = readSlice(buf);
        int kind = mybuf.get();
        switch(kind) {
            case 0x01: // 'folders'
                int typeNum = mybuf.get();
                DataType type = DataType.get(typeNum);
                if(type == null) {
//                    LOG.log(Level.WARNING, "Unknown data type {0}", Integer.toHexString(typeNum));
                    break;
                } else {
//                    LOG.log(Level.INFO, "Data type {0}", type);
                }
                switch(type) {
                    case COMPRESSED:
                        parse(decompress(mybuf), parent);
                    break;
                    case DIRECTORY:
                        parseDecompressed(mybuf, parent);
                    break;
                    default:
//                        LOG.log(Level.WARNING, "Unknown magic value {0}", new Object[] {Integer.toHexString(magic)});
                    break;
                }
                break;
            case 0x02: // 'files'
//                byte[] data = mybuf.array();
                break;
            default:
                System.out.println(tab(level) + "** Unknown data kind " + Integer.toHexString(kind));
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

            int totalLen = mybuf.getInt(); // Total size of blob
            int padding = mybuf.getInt();
            
            mybuf.limit(mybuf.position() + totalLen - 10 + padding);
            while(mybuf.remaining() > padding) {
            
                int descriptorLen = mybuf.getShort();
                int payloadLen = mybuf.getInt(); // remaining bytes

                ByteBuffer desc = readSlice(mybuf, descriptorLen);
                ByteBuffer payload = readSlice(mybuf, payloadLen);

                // Other
                int headerLen = 16 + descriptorLen; // payloadLen - 10? 16 because of magic(2), len(4), padding(4), descriptorLen(2), payloadLen(4)
                int childrenLen = totalLen - headerLen;
                
                // Check
    //            if(mybuf.remaining() < childrenLen + padding) { 
    //                LOG.log(Level.WARNING, "Content length ({0} and null padding ({1}) are bigger than the total remaining bytes ({2})", new Object[] {childrenLen, padding, mybuf.remaining()});
    //                return;
    //            }
                
                String name = getText(desc);
                desc.rewind();
                
                StringBuilder sb = new StringBuilder();
                sb.append("'").append(name.replaceAll("\1", "[1]").replaceAll("\2", "[2]")).append("'");
                sb.append(" ");
                sb.append("| totalLen(4): ").append(totalLen);
                sb.append(", padding(4): ").append(padding);
                sb.append(", descriptorLen(2): ").append(descriptorLen);
                sb.append(", payloadLen(4): ").append(payloadLen);
                sb.append(", descriptor(").append(descriptorLen).append(")");
                sb.append(" ");
                sb.append("| headerLen: ").append(headerLen);
                parent.setName(sb.toString());
                
                if(parent.getHeader() == FolderItemType.FOLDER.ID()) { // reading value
                    byte[] b = new byte[payload.remaining()];
                    if(b.length > 100) {
                        continue;
                    }
                    
                    String s = null;
                    
                    int typeNum = payload.get();
                    DataType type = DataType.get(typeNum);
                    if(type != null) {
                        s = type.toString();
                    }
                    if(s == null) {
                        payload.position(0);
                        payload.get(b);
                        s = "[";
                        for(int i = 0; i < b.length; i++) {
                            if(i != 0) {
                                s += " ";
                            }
                            s += Integer.toHexString(b[i] & 0xff);
                        }
                        s += "]";
                    }
                    System.out.println(tab(level+2) + "== " + s);
                    continue;
                }
                if(parent.getHeader() == FolderItemType.FILE.ID()) { // reading value name
                    BlobNode v = new BlobNode();
                    v.setHeader(FolderItemType.FOLDER.ID());
                    parse(payload, v);
                    System.out.println(tab(level+1) + "== " + v.getName());
                    continue;
                }
                
                // parse payload
                desc.position(0);
                int typeNum = desc.get();
                desc.position(0);
                String s = getText(desc);
                desc.position(0);
                payload.position(0);
                FolderItemType thisType = FolderItemType.get(typeNum);
                if(thisType == null) { // regular directory
                    System.out.println(tab(level++) + "=> " + parent.getName());
                    BlobNode tmp = new BlobNode(); // a container directory
                    parse(payload, tmp);
                    parent.children.addAll(tmp.children);
                    --level;
                    System.out.println(tab(level) + "<= " + parent.getName());
                } else {
                    switch(thisType) {
                        case FOLDER:
                            System.out.println(tab(level++) + "-> " + parent.getName());
                            BlobNode dir = new BlobNode(); // a normal directory
                            parse(payload, dir);
                            parent.children.add(dir);
                            --level;
                            System.out.println(tab(level) + "<- " + parent.getName());
                            break;
                        case FILE:
                            System.out.println(tab(level++) + "+> " + parent.getName());
                            BlobNode f = new BlobNode(); // a data entry
                            f.setHeader(FolderItemType.FILE.ID());
                            parse(payload, f);
                            System.out.println(tab(level++) + "*> " + f.getName());
                            parent.children.add(f);
                            
                            --level;
                            System.out.println(tab(level) + "<* " + f.getName());
                            --level;
                            System.out.println(tab(level) + "<+ " + parent.getName());
                            break;
                        default:
                            LOG.log(Level.WARNING, "Unexpected folder item type: {0}", typeNum);
                            break;
                    }
                }
            }
            
            mybuf.position(mybuf.position() + padding);
            if(mybuf.remaining() != 0) {
                System.out.println(tab(level) + "** Underflow: " + mybuf.remaining());
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
        LOG.log(Level.INFO, "Inflating a compressed binary section, initial length (including header) is {0}", originalBuffer.remaining());
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

            LOG.log(Level.INFO, "Header claims payload compressed length is {0}, deflated length is {1}, compression level {2}", new Object[] {compressedLen, decompressedLen, compLevel});

            if(mybuf.remaining() < compressedLen) {
                LOG.log(Level.WARNING, "The buffer remainder is too small ({0}) to contain the amount of data the header specifies ({1}).", new Object[] {mybuf.remaining(), compressedLen});
            }

            mybuf.limit(mybuf.position() + compressedLen);

            byte[] compressed = new byte[mybuf.remaining()];
            mybuf.get(compressed);
            inflater.setInput(compressed, headerSkip, compressed.length - headerSkip);
            byte[] decompressed = new byte[decompressedLen];
            try {
                LOG.info("Beginning decompression");
                inflater.inflate(decompressed);
                LOG.info("Decompression successful");
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