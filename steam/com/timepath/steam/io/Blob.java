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
 * @author timepath
 */
public class Blob {
    
    private Blob.BlobNode data = new Blob.BlobNode("root");
    
    public Blob(File f) throws IOException {
        FileInputStream fis = new FileInputStream(f);
        FileChannel fc = fis.getChannel();
        MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_ONLY, 0, f.length());
        mbb.order(ByteOrder.LITTLE_ENDIAN);
        
        parse(mbb, data);
    }
    
    private static final Logger LOG = Logger.getLogger(Blob.class.getName());
    
    public static void analyze(File f, DefaultMutableTreeNode root) throws IOException {
        Blob b = new Blob(f);
        recurse(b.data, root);
    }
    
    private static void recurse(Blob.BlobNode bn, DefaultMutableTreeNode tn) {
        DefaultMutableTreeNode t = new DefaultMutableTreeNode(bn);
        tn.add(t);
        ArrayList<Blob.BlobNode> c = bn.children;
        for(int i = 0; i < c.size(); i++) {
            recurse(c.get(i), t);
        }
    }
    
    public class BlobNode {
        
        public BlobNode() {
            
        }
        
        public BlobNode(String s) {
            this.setName(s);
        }
        
        private ArrayList<Blob.BlobNode> children = new ArrayList<Blob.BlobNode>();
        
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
        
    }
    
    static final int HEADER_VAL = 0x5001; // Big endian = 01 50
    static final int HEADER_COMPRESSED = 0x4301; // Big endian = 01 43
    static final int NULL_TERMINATOR = 0x00;
    
    static final Charset charset = Charset.forName("UTF-8");
    
    public static ByteBuffer readSlice(ByteBuffer source) {
        return readSlice(source, source.remaining());
    }
    
    public static ByteBuffer readSlice(ByteBuffer source, int length) {
        int originalLimit = source.limit();

        source.limit(source.position() + length);
        ByteBuffer sub = source.slice();
        source.position(source.limit());
        source.limit(originalLimit);
        sub.order(ByteOrder.LITTLE_ENDIAN);
        return sub;
    }
    
    public static String getText(ByteBuffer source) {
        int pos = source.position();
        int end = source.limit();
        while (source.remaining() > 0) {
            if (source.get() == NULL_TERMINATOR) {
                end = source.position() - 1;
                break;
            }
        }
        source.position(pos);
        source.limit(end);

        return charset.decode(source).toString();
    }
    
    public void parse(ByteBuffer buf, Blob.BlobNode parent) {
        try {
            ByteBuffer mybuf = readSlice(buf);

            int magic = mybuf.getShort();


            if (magic != HEADER_VAL) {
                if (magic == HEADER_COMPRESSED) {
                    LOG.warning("The blob given appears to be compressed, but no compressed blob was expected.");
                } else {
                    LOG.log(Level.WARNING, "Expected magic value {0} but got {1}", new Object[]{Integer.toHexString(HEADER_VAL), Integer.toHexString(magic)});
                }
            }

            int childrenLen = mybuf.getInt() - 10; // Minus ten because it includes magic(2) len (4) padding (4)
            int nullPadded = mybuf.getInt();
            if (magic != HEADER_VAL) {
                LOG.log(Level.WARNING, "Expected magic value {0} but got {1}", new Object[]{Integer.toHexString(HEADER_VAL), Integer.toHexString(magic)});
            }
            if (mybuf.remaining() < childrenLen + nullPadded) {
                LOG.log(Level.WARNING, "Content length ({0} and null padding ({1}) are bigger than the total remaining bytes ({2})", new Object[]{childrenLen, nullPadded, mybuf.remaining()});
            }


            mybuf.limit(mybuf.position() + childrenLen + nullPadded);
            while (mybuf.remaining() > nullPadded) {
                int descriptorLen = mybuf.getShort();
                int payloadLen = mybuf.getInt();
                assert ((payloadLen + descriptorLen) <= mybuf.remaining());

                ByteBuffer desc = readSlice(mybuf, descriptorLen);
                Blob.BlobNode child = new Blob.BlobNode(getText(desc));
                parent.children.add(child);
                ByteBuffer payload = readSlice(mybuf, payloadLen);
                parse(payload, parent);
                
            }
            mybuf.position(mybuf.position() + nullPadded);
//            ret.underflow = mybuf.remaining();

//            return ret;
        } catch (BufferUnderflowException bue) {
            bue.printStackTrace();
        }



    }
    
    private int level = 0;
    
    private static String tab(int t) {
        String s = "";
        for(int i = 0; i < t; i++) {
            s += "  ";
        }
        return s;
    }
    
    
    /**
     * Compressed header:
     * int
     * compressed + 20 (size of header neglecting initial short)
     * int
     * dummy1
     * int
     * decompressed
     * int
     * dummy2
     * short
     * dummy3
     * byte[compressed]
     * perform zlib decompression (skip the first 2 bytes) to get byte[decompressed]
     */
    public static ByteBuffer decompress(ByteBuffer originalBuffer) {
        LOG.log(Level.INFO, "Inflating a compressed binary section, initial length (including header) is {0}", originalBuffer.remaining());
        int headerSkip = 2;
        Inflater inflater = new Inflater(true);

        try {
            ByteBuffer mybuf = readSlice(originalBuffer);

            int magic = mybuf.getShort();
            if (magic != HEADER_COMPRESSED) {
                if (magic == HEADER_VAL) {
                    LOG.warning("The blob given appears to be uncompressed, but a compressed blob was expected.");
                } else {
                    LOG.log(Level.WARNING, "Expected magic value {0} but got {1}", new Object[]{Integer.toHexString(HEADER_COMPRESSED), Integer.toHexString(magic)});
                }
            }

            // Includes length of magic header etc?
            int wholeLen = mybuf.getInt(); // Includes bytes starting with itself            
            int compressedLen = wholeLen - 20;            
            int x1 = mybuf.getInt();
            int decompressedLen = mybuf.getInt();
            int x2 = mybuf.getInt();
            int compLevel = mybuf.getShort();

            LOG.log(Level.INFO, "Header claims payload compressed length is {0}, deflated length is {1}, compression level {2}", new Object[]{compressedLen,decompressedLen,compLevel});

            if (mybuf.remaining() < compressedLen) {
                LOG.log(Level.WARNING, "The buffer remainder is too small ({0}) to contain the amount of data the header specifies ({1}).", new Object[]{mybuf.remaining(), compressedLen});
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
            } catch (DataFormatException ex) {
                ex.printStackTrace();
            }

            ByteBuffer newBuf = ByteBuffer.wrap(decompressed);
            newBuf.order(ByteOrder.LITTLE_ENDIAN);

            return newBuf;
        } catch (BufferUnderflowException bue) {
            bue.printStackTrace();
        }
        return null;
    }
    
    // Legacy code
    
    
    private Blob.BlobNode readBlob(RandomAccessFile rf, Blob.BlobNode bn) throws IOException {
        bn.setHeader(DataUtils.readULEShort(rf));
        switch(bn.getHeader()) {
            case HEADER_COMPRESSED:
                System.out.println(bn + "Compressed");
                break;
            case HEADER_VAL:
                readBlobUncompressed(rf, bn);
                break;
            case 0:
            case 1:
            case 2:
                break;
            default:
//                LOG.log(Level.WARNING, "Unknown header {0} at {1}", new Object[]{bn.getHeader(), (rf.getFilePointer() - 2)});
                break;
        }
        return bn;
    }

    /**
     * 
     * @param rf
     * @param parent section to read into
     * @throws IOException 
     */
    private void readBlobUncompressed(RandomAccessFile rf, Blob.BlobNode parent) throws IOException {
        int totalLength = DataUtils.readULEInt(rf); // Length of entire data node: header + data
        int nullPadding = DataUtils.readULEInt(rf);
        if(nullPadding != 0) {
            LOG.log(Level.INFO, "Padding: {0}", nullPadding);
        }
        int descLength = DataUtils.readULEShort(rf);
        int dataLength = DataUtils.readULEInt(rf); // Length of all child data, header(10) inclusive
        byte[] nameArray = DataUtils.readBytes(rf, descLength);
        String name = new String(nameArray);
        
        int headLength = 2 + 4 + 4 + 2 + 4 + nameArray.length; // minimum 20
        long offset = rf.getFilePointer() - headLength;
        long dataStart = offset + headLength;
        long dataEnd = dataStart + dataLength;
        
        parent.setName("'" + name.replaceAll("\1", "[1]").replaceAll("\2", "[2]") + "', hS: "+offset+", hL: "+headLength+", hE/dS: "+dataStart+", dL: "+dataLength+", dE: "+dataEnd+" | bL: "+totalLength+", bE: "+(offset+totalLength));
        
//        if(dataLength >= 20) {
            System.out.println(tab(level) + ">> " + parent.getName());
            level++;
            while(rf.getFilePointer() < dataEnd) {
                Blob.BlobNode child = readBlob(rf, new Blob.BlobNode());
                if(child.getHeader() == HEADER_COMPRESSED) {
                    break;
                }
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
    
}
