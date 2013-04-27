package com.timepath.hl2.io;

import com.timepath.DataUtils;
import com.timepath.steam.io.util.Property;
import com.timepath.steam.io.VDF;
import com.timepath.steam.io.util.VDFNode;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * https://developer.valvesoftware.com/wiki/Closed_Captions
 * https://developer.valvesoftware.com/wiki/Subtitles_(Portal_2)
 * https://developer.valvesoftware.com/wiki/Soundscript
 * phonemes
 *
 * @author timepath
 */
public class VCCD {

    private static final Logger LOG = Logger.getLogger(VCCD.class.getName());

    public VCCD() {
    }

    /**
     * Used for writing captions
     *
     * @param curr
     * @param round
     *
     * @return
     */
    private static int alignValue(double curr, double round) {
        return (int) (Math.ceil(curr / round) * round);
    }

    private static int expectedHeader = (('V') | ('C' << 8) | ('C' << 16) | ('D' << 24));

    private static int expectedVersion = 1;

    public static ArrayList<CaptionEntry> load(InputStream is) {
        ArrayList<CaptionEntry> list = new ArrayList<CaptionEntry>();
        try {
            byte[] array = new byte[is.available()];
            is.read(array);
            ByteBuffer buf = ByteBuffer.wrap(array);
            buf.order(ByteOrder.LITTLE_ENDIAN);
            
            if(buf.getInt() != expectedHeader) {
                LOG.severe("Header mismatch");
            }
            int version = buf.getInt();
            if(version != expectedVersion) {
                LOG.log(Level.WARNING, "Unsupported version: {0}", version);
            }
            int blocks = buf.getInt();
            int blockSize = buf.getInt();
            int directorySize = buf.getInt();
            int dataOffset = buf.getInt();
            LOG.log(Level.INFO, "Version: {0}, Blocks: {1}, BlockSize: {2}, DirectorySize: {3}, DataOffset: {4}", new Object[]{version, blocks, blockSize, directorySize, dataOffset});

            CaptionEntry[] entries = new CaptionEntry[directorySize];
            for(int i = 0; i < directorySize; i++) {
                CaptionEntry e = new CaptionEntry();
                e.setKey(buf.getInt());
                e.setBlock(buf.getInt());
                e.setOffset(buf.getShort());
                e.setLength(buf.getShort());
//                    System.out.println("<" + i + " - " + e);
                entries[i] = e;
            }
            buf.position(dataOffset);
            for(int i = 0; i < directorySize; i++) {
                buf.position(dataOffset + (entries[i].block * blockSize) + entries[i].offset);
                StringBuilder sb = new StringBuilder((entries[i].length / 2) - 1);
                for(int x = 0; x < (entries[i].length / 2) - 1; x++) {
                    sb.append(buf.getChar()); // 2 bytes
                }
                buf.get(new byte[2]);
                entries[i].setValue(sb.toString());
                list.add(entries[i]);
            }
            is.close(); // The rest of the file is garbage, 0's or otherwise
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return list;
    }

    /**
     * Ensure alphabetical order
     *
     * @param file
     * @param entries
     */
    public static void save(String file, ArrayList<CaptionEntry> entries) {
        if(file == null) {
            return;
        }
        try {
            Collections.sort(entries);
            int directorySize = entries.size();
            int blockSize = 8192;
            int length = 0;
            int blocks = 1;
            for(int i = 0; i < directorySize; i++) {
                int eval = length + entries.get(i).getLength();
                if(eval > blockSize) {
                    blocks++;
                    length = entries.get(i).getLength();
                } else {
                    length = eval;
                }
            }

            LOG.log(Level.FINE, "Blocks: {0}", blocks);

            int dataOffset = alignValue((6 * 4) + (directorySize * 12), 512);

            File f = new File(file);
            if(f.exists()) {
                f.delete();
            } else {
                f.createNewFile();
            }
            RandomAccessFile rf = new RandomAccessFile(f, "rw");
            DataUtils.writeLEInt(rf, expectedHeader);
            DataUtils.writeLEInt(rf, 1);
            DataUtils.writeLEInt(rf, blocks);
            DataUtils.writeLEInt(rf, blockSize);
            DataUtils.writeLEInt(rf, directorySize);
            DataUtils.writeLEInt(rf, dataOffset);

            int currentBlock = 0;
            int firstInBlock = 0;
            for(int i = 0; i < directorySize; i++) {
                CaptionEntry e = entries.get(i);
                e.setBlock(0);
                e.setOffset(0);

                int offset;

                int proposedOffset = 0;
                for(int j = firstInBlock; j < i; j++) {
                    proposedOffset += entries.get(j).getLength();
                }
                if((proposedOffset + e.getLength()) > blockSize) {
                    offset = 0;
                    currentBlock++;
                    firstInBlock = i;
                    LOG.fine("Doesn't fit; new block");
                } else {
                    offset = proposedOffset;
                }

                e.setBlock(currentBlock);
                e.setOffset(offset);

//                    System.out.println(">" + i + " - " + e);

                DataUtils.writeULong(rf, e.getKey());
                DataUtils.writeLEInt(rf, e.getBlock());
                DataUtils.writeULEShort(rf, (short) e.getOffset());
                DataUtils.writeULEShort(rf, (short) e.getLength());
            }

            rf.write(new byte[(dataOffset - (int) rf.getFilePointer())]);

            int lastBlock = 0;
            for(int i = 0; i < directorySize; i++) {
                CaptionEntry e = entries.get(i);
                if(e.getBlock() > lastBlock) {
                    lastBlock = e.getBlock();
                    rf.write(new byte[blockSize - (entries.get(i - 1).getOffset() + entries.get(i - 1).getLength())]);
                }
                DataUtils.writeLEChars(rf, e.getValue());
                DataUtils.writeLEChar(rf, 0);
            }
            int last = entries.size() - 1;
            rf.write(new byte[blockSize - (entries.get(last).getOffset() + entries.get(last).getLength())]);
            rf.close();
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        LOG.log(Level.INFO, "Saved {0}", file);
    }

    public static int takeCRC32(String in) {
        CRC32 crc = new CRC32();
        crc.update(in.toLowerCase().getBytes());
        return (int) crc.getValue();
    }

    /**
     * The file must be in UCS-2 LE or UTF-16 LE (unicode) format
     *
     * @param file
     *
     * @return
     */
    public static ArrayList<CaptionEntry> importFile(String file) throws FileNotFoundException {
        DefaultMutableTreeNode tn = new DefaultMutableTreeNode();
        VDF v = new VDF();
        v.readExternal(new FileInputStream(new File(file)), "UTF-16");
        ArrayList<CaptionEntry> children = new ArrayList<CaptionEntry>();
        ArrayList<Property> props = ((VDFNode)(tn.getChildAt(0).getChildAt(1))).getProperties();
        ArrayList<String> usedKeys = new ArrayList<String>();
        for(int i = props.size() - 1; i >= 0; i--) {
            Property p = props.get(i);
//            LOG.log(Level.INFO, "Adding {0}", p);
            CaptionEntry e = new CaptionEntry();
            String key = p.getKey().replaceAll("\"", "");
            if(key.equals("//") || key.equals("\\n") || usedKeys.contains(key)) {
                LOG.log(Level.INFO, "Discarding: {0}", key);
                continue;
            }
            usedKeys.add(key);
            e.setKey(key);
            e.setValue(p.getValue().replaceAll("\"", ""));
            children.add(e);
        }
        Collections.sort(children);
        return children;
    }

    /**
     * Entries are stored alphabetically by original value of hash
     */
    public static class CaptionEntry implements Comparable<CaptionEntry> {

        public CaptionEntry() {
        }

        private long key;

        public int getKey() {
            return (int) key;
        }

        public void setKey(long key) {
            this.key = key;
        }

        private String trueKey;

        public String getTrueKey() {
//            if(trueKey == null) {
//                trueKey = attemptDecode((int) key);
//            }
            return trueKey;
        }

        public void setKey(String key) {
            this.key = takeCRC32(key);
            this.trueKey = key;
        }

        private int block;

        public int getBlock() {
            return block;
        }

        public void setBlock(int block) {
            this.block = block;
        }

        private int offset;

        public int getOffset() {
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }

        private int length;

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
            if(this.value != null) {
                this.value = value.substring(0, (length / 2) - 1);
            }
        }

        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String string) {
            this.value = string;
            this.length = (string.length() + 1) * 2;
        }

        @Override
        public String toString() {
            return new StringBuilder().append("H: ").append(key).append(", b: ").append(block).append(", o: ").append(offset).append(", l: ").append(length).toString();
        }

        public int compareTo(CaptionEntry t) {
            String e1 = this.getTrueKey();
            if(e1 == null) {
                e1 = "";
            }
            String e2 = t.getTrueKey();
            if(e2 == null) {
                e2 = "";
            }
            return e1.compareToIgnoreCase(e2);
        }
    }

}