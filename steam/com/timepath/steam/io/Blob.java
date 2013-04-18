package com.timepath.steam.io;

import com.timepath.DataUtils;
import com.timepath.Utils;
import com.timepath.swing.TreeUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Calendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.xml.bind.DatatypeConverter;

/**
 *
 * https://github.com/DHager/hl2parse/blob/master/hl2parse-binary/src/main/java/com/technofovea/hl2parse/registry/RegParser.java
 *
 * Nodes of name \1\0\0\0 contain 'folders', \2\0\0\0 contain 'files'
 * Leaf \1\0\0\0 directories the data type used in the following \2\0\0\0 node's payload
 *
 * @author timepath
 */
public class Blob {

    private static final Logger LOG = Logger.getLogger(Blob.class.getName());

    public static void analyze(File f, DefaultMutableTreeNode root) throws IOException {
        BlobNode bn = new BlobNode("root");
        ByteBuffer buf = DataUtils.mapFile(f);
        parsePayload(buf, bn, false);
        TreeUtils.moveChildren(bn, root);
    }

    @SuppressWarnings("fallthrough")
    private static void parsePayload(ByteBuffer parentbuf, BlobNode parent, boolean rawData) {
        ByteBuffer buf = DataUtils.getSlice(parentbuf);
        if(!rawData) {
            if(buf.remaining() == 1) {
                byte byt = buf.get();
                parent.add(new BlobNode(byt));
                return;
            }
            if(parent.getMeta() == 2) {
                int dataType = ((BlobNode) parent.getPreviousSibling()).dataType;
                if(dataType != -1) {
                    switch(dataType) {
                        case 0: // Text
                            String str = DataUtils.getText(buf, true);
                            Calendar cal;
                            try {
                                cal = DatatypeConverter.parseDateTime(str);
                                str = "Date: " + cal.getTime().toString();
                            } catch(java.lang.IllegalArgumentException ex) {
                            }
                            LOG.log(Level.FINE, "String: {0}", str);
                            parent.add(new BlobNode("String: " + str));
                            break;
                        case 1: // Dword
                            int val = buf.getInt();
                            LOG.log(Level.FINE, "DWORD: {0}", val);
                            parent.add(new BlobNode("DWORD: " + val));
                            break;
                        case 2: // Raw
                            byte[] data = new byte[6];
                            buf.get(data, 0, Math.min(buf.remaining(), data.length));
                            buf.position(0);
                            BlobNode raw = new BlobNode("Raw data: " + Utils.hex(data) + " ...");
                            parent.add(raw);
                            parsePayload(buf, raw, true);
                            break;
                        default:
                            if(!rawData) {
                                parent.add(new BlobNode("Unhandled data type " + dataType));
                            }
//                        LOG.log(Level.WARNING, "Unhandled data type {0}", dataType);
                            break;
                    };
                    return;
                }
            }
        }
        short id = buf.getShort();
        BlobNode d = new BlobNode("Payload: 0x" + Integer.toHexString(id));
        switch(id) {
            case 0x4301:
                parent.add(d);
                ByteBuffer decompressed = decompress(buf);
                
//                int stride = 40;
//                byte[] data = new byte[stride];
//                System.out.println(decompressed.remaining() + ":");
//                File f = new File("/home/timepath/Desktop/binout.blob");
//                RandomAccessFile rf = null;
//                try {
//                    f.createNewFile();
//                    rf = new RandomAccessFile(f, "rw");
//                } catch(IOException ex) {
//                    Logger.getLogger(Blob.class.getName()).log(Level.SEVERE, null, ex);
//                }
//                for(int i = 0; i < data.length; i++) {
//                    decompressed.get(data, 0, Math.min(decompressed.remaining(), stride));
//                    if(rf != null) {
//                        try {
//                            rf.write(data);
//                        } catch(IOException ex) {
//                            Logger.getLogger(Blob.class.getName()).log(Level.SEVERE, null, ex);
//                        }
//                    }
//                    System.out.println(Utils.hex(data));
//                }
//                decompressed.position(0);
                
//                parsePayload(decompressed, d, false);
                break;
            case 0x5001:
                parent.add(d);
                int length = buf.getInt();
//                d.add(new BlobNode("length", length));
                int padding = buf.getInt();
//                d.add(new BlobNode("padding", padding));
                int limit = Math.min((buf.position() - 10) + length + padding, buf.position() + buf.remaining()); // workaround for decompressed
                LOG.log(Level.INFO, "limit: {0}", limit);
                buf.limit(limit); // 10 because is relative to when this section started
                ByteBuffer payload = DataUtils.getSlice(buf);
                //<editor-fold defaultstate="collapsed" desc="Payload">
                BlobNode children = new BlobNode("Children");
                if(payload.remaining() > padding) {
                    d.add(children);
                }
                while(payload.remaining() > padding) {
                    BlobNode child = new BlobNode();
                    children.add(child);
                    short descLength = payload.getShort();
//                    child.add(new BlobNode("nameLength", descLength));
                    int payloadLength = payload.getInt();
//                    child.add(new BlobNode("payloadLength", payloadLength));
                    ByteBuffer childDesc = DataUtils.getSlice(payload, descLength);
                    String name = DataUtils.getText(childDesc);
                    if(name.equals("\1\0\0\0") || name.equals("\2\0\0\0")) {
                        childDesc.position(0);
                        child.setMeta(childDesc.getInt());
                    }
                    name = name.replaceAll("\1\0\0\0", "<Folder>");
                    name = name.replaceAll("\2\0\0\0", "<File>");
//                    child.add(new BlobNode("name", name));
                    child.setUserObject(name);
                    ByteBuffer childPayload = DataUtils.getSlice(payload, payloadLength);
                    parsePayload(childPayload, child, false);
                }
                payload.get(new byte[padding]);
                //</editor-fold>
                break;
            default:
                if(parent.getMeta() == 1) {
                    buf.position(0);
                    int dataType = buf.getInt();
                    parent.dataType = dataType;
                    parent.add(new BlobNode("Payload type: " + dataType));
                    return;
                } else if(!rawData) {
                    LOG.log(Level.WARNING, "Unhandled {0}", id);
                }
                break;
        }
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
        LOG.log(Level.INFO, "Inflating a compressed binary section, initial length (including header) is {0}", originalBuffer.remaining());
        int headerSkip = 2;
        Inflater inflater = new Inflater(true);

        ByteBuffer mybuf = DataUtils.getSlice(originalBuffer);

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
    }

    @SuppressWarnings("serial")
    private static class BlobNode extends DefaultMutableTreeNode {

        BlobNode(Object obj) {
            this.setUserObject(obj);
        }

        BlobNode(String name, Object obj) {
            this.name = name;
            this.setUserObject(obj);
        }

        private String name;

        private int dataType = -1;

        @Override
        public String toString() {
            Object o = this.getUserObject();
            if(o == null) {
                return "unnamed";
            } else if(o instanceof String) {
                return (String) o;
            } else {
                return (name != null ? name : o.getClass().getSimpleName()) + ": " + o;
            }
        }

        BlobNode() {
        }

        private int meta;

        public boolean isMeta() {
            return meta != 0;
        }

        public int getMeta() {
            return meta;
        }

        public void setMeta(int meta) {
            this.meta = meta;
        }
    }

    private Blob() {
    }
}