package com.timepath.steam.io;

import com.timepath.DataUtils;
import com.timepath.DateUtils;
import com.timepath.Utils;
import com.timepath.swing.TreeUtils;
import java.io.File;
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

    private static void parsePayload(ByteBuffer parentbuf, BlobNode parent, boolean rawData) {
        ByteBuffer buf = DataUtils.getSlice(parentbuf);
        if(buf.remaining() < 2) {
            return;
        }
        short id = buf.getShort();
        BlobNode d = new BlobNode("Payload: 0x" + Integer.toHexString(id));
        switch(id) {
            //<editor-fold defaultstate="collapsed" desc="Compressed">
            /*
             * case 0x4301:
             * ByteBuffer decompressed = decompress(buf);
             * //<editor-fold defaultstate="collapsed" desc="Debug">
             * int stride = 20;
             * byte[] data = new byte[stride];
             * File f = new File("binout.blob");
             * RandomAccessFile rf = null;
             * if(f != null) {
             * try {
             * f.createNewFile();
             * rf = new RandomAccessFile(f, "rw");
             * } catch(IOException ex) {
             * Logger.getLogger(Blob.class.getName()).log(Level.SEVERE, null, ex);
             * }
             * }
             * LOG.log(Level.INFO, "{0}:", decompressed.remaining());
             * for(int i = 0; i < data.length; i++) {
             * decompressed.get(data, 0, Math.min(decompressed.remaining(), stride));
             * if(rf != null) {
             * try {
             * rf.write(data);
             * } catch(IOException ex) {
             * Logger.getLogger(Blob.class.getName()).log(Level.SEVERE, null, ex);
             * }
             * }
             * LOG.info(Utils.hex(data));
             * }
             * decompressed.position(0);
             * //</editor-fold>
             * parsePayload(decompressed, d, false);
             * break;
             */
            //</editor-fold>
            case 0x5001:
                int length = buf.getInt();
                int padding = buf.getInt();
                int limit = (buf.position() - 10) + length + padding; // 10 because is relative to when this section started
//                limit = Math.min(limit, buf.position() + buf.remaining()); // workaround for decompressed
                LOG.log(Level.FINE, "limit: {0}", limit);
                buf.limit(limit);
                ByteBuffer payload = DataUtils.getSlice(buf);
                //<editor-fold defaultstate="collapsed" desc="Payload">
                BlobNode children = d;
                while(payload.remaining() > padding) {
                    BlobNode child = new BlobNode();
                    short descLength = payload.getShort();
                    int payloadLength = payload.getInt();
                    ByteBuffer childDesc = DataUtils.getSlice(payload, descLength);
                    String name = DataUtils.getText(childDesc);
                    if(name.equals("\1\0\0\0") || name.equals("\2\0\0\0")) {
                        childDesc.position(0);
                        child.setMeta(childDesc.getInt());
                    }
                    name = name.replaceAll("\1\0\0\0", "<Folder>");
                    name = name.replaceAll("\2\0\0\0", "<File>");
                    child.setUserObject(name);

                    ByteBuffer childPayload = DataUtils.getSlice(payload, payloadLength);

                    if(payloadLength == 10) {
                        continue;
                    }
                    BlobNode nextup = child;
                    if(!child.isMeta()) {
                        children.add(child);
                    } else {
                        nextup = parent;
                    }
                    if(child.getMeta() == 1 && payloadLength == 4) {
                        parent.dataType = childPayload.getInt();//parent.add(new BlobNode("Payload type: " + parent.dataType));
                    } else {
                        int dataType = parent.dataType;
                        if(dataType != -1) {
                            switch(dataType) {
                                case 0: // Text
                                    String str = DataUtils.getText(childPayload, true);
                                    String date = DateUtils.parse(str);
                                    if(date != null) {
                                        str += " Date: " + date;
                                    }
                                    LOG.log(Level.FINE, "String: {0}", str);
                                    parent.add(new BlobNode("String: " + str));
                                    break;
                                case 1: // Dword
                                    int val = childPayload.getInt();
                                    LOG.log(Level.FINE, "DWORD: {0}", val);
                                    parent.add(new BlobNode("DWORD: " + val));
                                    break;
                                case 2: // Raw
                                    int remaining = childPayload.remaining();
                                    int max = 10;
                                    byte[] data = new byte[Math.min(childPayload.remaining(), max)];
                                    childPayload.get(data);
                                    childPayload.position(0);
                                    BlobNode raw = new BlobNode("Raw data: " + Utils.hex(data) + (remaining > max ? " ..." : ""));
                                    parent.add(raw);
                                    parsePayload(childPayload, raw, true);
                                    break;
                                default:
                                    if(!rawData) {
                                        parent.add(new BlobNode("Unhandled data type: " + dataType));
                                    }
//                                LOG.log(Level.WARNING, "Unhandled data type {0}", dataType);
                                    break;
                            }
                        } else {
                            parsePayload(childPayload, nextup, false);
                        }
                    }
                }
                payload.get(new byte[padding]);

                if(buf.remaining() > 0) {
                    LOG.log(Level.INFO, "Underflow: {0}", buf.remaining());
                    return;
                }
                //</editor-fold>
                break;
            default:
                if(!rawData) {
                    LOG.log(Level.WARNING, "Unhandled {0}", id);
                }
                break;
        }
        TreeUtils.moveChildren(d, parent);
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