package com.timepath.hl2.io;

import com.timepath.DataUtils;
import com.timepath.hl2.io.util.Element;
import com.timepath.hl2.io.util.Property;
import com.timepath.steam.io.VDF;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
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

    String currentFile;

    private static String magicID = "VCCD";

    private static int captionVer = 1;

    public ArrayList<Entry> loadFile(String file) {
        if(file == null) {
            return null;
        }
        this.currentFile = file;
        ArrayList<Entry> list = new ArrayList<Entry>();
        try {
            RandomAccessFile rf = new RandomAccessFile(file, "r");
            String magic = new String(new byte[]{DataUtils.readByte(rf), DataUtils.readByte(rf), DataUtils.readByte(rf), DataUtils.readByte(rf)});
            if(!magic.equals(magic)) {
                LOG.severe("Header mismatch");
            }
            int ver = DataUtils.readULEInt(rf);
            int blocks = DataUtils.readULEInt(rf);
            int blockSize = DataUtils.readULEInt(rf);
            int directorySize = DataUtils.readULEInt(rf);
            int dataOffset = DataUtils.readULEInt(rf);
            LOG.log(Level.INFO, "Header: {0}, Version: {1}, Blocks: {2}, BlockSize: {3}, DirectorySize: {4}, DataOffset: {5}", new Object[]{magic, ver, blocks, blockSize, directorySize, dataOffset});

            Entry[] entries = new Entry[directorySize];
            for(int i = 0; i < directorySize; i++) {
                Entry e = new Entry();
                e.setKey(DataUtils.readULong(rf));
                e.setBlock(DataUtils.readULEInt(rf));
                e.setOffset(DataUtils.readULEShort(rf));
                e.setLength(DataUtils.readULEShort(rf));
//                    System.out.println("<" + i + " - " + e);
                entries[i] = e;
            }
            rf.seek(dataOffset);
            for(int i = 0; i < directorySize; i++) {
                rf.seek(dataOffset + (entries[i].block * blockSize) + entries[i].offset);
                StringBuilder sb = new StringBuilder((entries[i].length / 2) - 1);
                for(int x = 0; x < (entries[i].length / 2) - 1; x++) {
                    sb.append(DataUtils.readLEChar(rf));
                }
                rf.skipBytes(2);
                entries[i].setValue(sb.toString());
                list.add(entries[i]);
            }
            rf.close(); // The rest of the file is garbage, 0's or otherwise
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
//            saveFile(file + ".test", list); // debugging
        return list;
    }

    /**
     * Ensure alphabetical order
     *
     * @param file
     * @param entries
     */
    public void saveFile(String file, ArrayList<Entry> entries) {
        if(file == null) {
            return;
        }
        try {
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

            System.out.println("Blocks: " + blocks);

            int dataOffset = alignValue((6 * 4) + (directorySize * 12), 512);

            File f = new File(file);
            if(f.exists()) {
                f.delete();
            } else {
                f.createNewFile();
            }
            RandomAccessFile rf = new RandomAccessFile(f, "rw");
            rf.write(magicID.getBytes()); // Big endian
            DataUtils.writeLEInt(rf, 1);
            DataUtils.writeLEInt(rf, blocks);
            DataUtils.writeLEInt(rf, blockSize);
            DataUtils.writeLEInt(rf, directorySize);
            DataUtils.writeLEInt(rf, dataOffset);

            int currentBlock = 0;
            int firstInBlock = 0;
            for(int i = 0; i < directorySize; i++) {
                Entry e = entries.get(i);
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
                    System.out.println("Doesn't fit; new block");
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
                Entry e = entries.get(i);
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
     * @param file
     * @return 
     */
    public ArrayList<Entry> importFile(String file) {
        DefaultMutableTreeNode tn = new DefaultMutableTreeNode();
        VDF.analyze(new File(file), tn);
        ArrayList<Entry> children = new ArrayList<Entry>();
        ArrayList<Property> props = ((Element)((DefaultMutableTreeNode)tn.getChildAt(0).getChildAt(0)).getUserObject()).getProps();
        ArrayList<String> usedKeys = new ArrayList<String>();
        for(int i = props.size() - 1; i >= 0; i--) {
            Property p = props.get(i);
//            LOG.log(Level.INFO, "Adding {0}", p);
            Entry e = new Entry();
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

    public Entry getNewEntry() {
        return new Entry();
    }

    /**
     * Entries are stored alphabetically by original value of hash
     */
    public class Entry implements Comparable<Entry> {

        public Entry() {
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

        public int compareTo(Entry t) {
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