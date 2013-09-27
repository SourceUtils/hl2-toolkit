package com.timepath.hl2.io;

import com.timepath.steam.io.util.Property;
import com.timepath.steam.io.VDF;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

/**
 *
 * https://developer.valvesoftware.com/wiki/Closed_Captions
 * https://developer.valvesoftware.com/wiki/Subtitles_(Portal_2)
 * https://developer.valvesoftware.com/wiki/Soundscript
 * phonemes
 * 
 * @author TimePath
 */
public class VCCD {
    
    private static final Logger LOG = Logger.getLogger(VCCD.class.getName());

    private VCCD() {
    }

    private static int HEADER_MAGIC = (('V') | ('C' << 8) | ('C' << 16) | ('D' << 24));

    private static int HEADER_VERSION_EXPECTED = 1;
    
    private static final int HEADER_SIZE = (4 * 6);

    private static final int ENTRY_SIZE = (4 * 2) + (2 * 2);

    private static final int DEFAULT_BLOCK_SIZE = 8192;

    private static final Charset VALUE_CHARSET = Charset.forName("UTF-16LE");

    public static List<CaptionEntry> load(InputStream is) throws IOException {
        byte[] array = new byte[is.available()];
        is.read(array);
        is.close();
        ByteBuffer buf = ByteBuffer.wrap(array);
        return load(buf);
    }

    public static List<CaptionEntry> load(ByteBuffer buf) {
        LOG.log(Level.INFO, "Loading from {0}", buf);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        if(buf.getInt() != HEADER_MAGIC) {
            LOG.severe("Header mismatch");
        }
        int version = buf.getInt();
        if(version != HEADER_VERSION_EXPECTED) {
            LOG.log(Level.WARNING, "Unsupported version: {0}", version);
        }
        int blocks = buf.getInt();
        int blockSize = buf.getInt();
        int totalEntries = buf.getInt();
        int dataOffset = buf.getInt();
        LOG.log(Level.FINE,
                "Version: {0}, Blocks: {1}, BlockSize: {2}, DirectorySize: {3}, DataOffset: {4}",
                new Object[] {version, blocks, blockSize, totalEntries, dataOffset});

        CaptionEntry[] entries = new CaptionEntry[totalEntries];
        for(int i = 0; i < entries.length; i++) {
            CaptionEntry e = new CaptionEntry();
            e.setKey(buf.getInt());
            e.setBlock(buf.getInt());
            e.setOffset(buf.getShort());
            e.setLength(buf.getShort());
            entries[i] = e;
            LOG.log(Level.FINE, "Loading {0}, {1} ({2}->{3})", new Object[] {
                i, e.getKey(), e.getOffset(), e.getOffset() + e.getLength()});
        }
        buf.position(dataOffset);
        for(CaptionEntry e : entries) {
            buf.position(dataOffset + (e.block * blockSize) + e.offset);
            int limit = buf.limit();
            int size = e.length - 2;
            buf.limit(buf.position() + size);
            e.setValue(VALUE_CHARSET.decode(buf.slice()).toString());
            buf.limit(limit);
        }
        // The rest of the file is useless, 0's or otherwise
        LOG.log(Level.INFO, "Loaded from {0}", buf);
        return Arrays.asList(entries);
    }

    public static ByteBuffer save(List<CaptionEntry> entries, OutputStream os) throws IOException {
        ByteBuffer buf = save(entries);
        byte[] bytes = new byte[buf.limit()];
        buf.get(bytes);
        buf.flip();
        os.write(bytes);
        os.flush();
        os.close();
        return buf;
    }

    public static ByteBuffer save(List<CaptionEntry> entries) throws UnsupportedEncodingException {
        Collections.sort(entries); // Ensure alphabetical order

        // Pack into blocks
        int blockSize = DEFAULT_BLOCK_SIZE;
        int totalLength = 0, requiredBlocks = 1, blockPtr;
        for(CaptionEntry e : entries) {
            blockPtr = totalLength % blockSize;
            if(blockPtr + e.getLength() > blockSize) {
                e.setOffset(0);
                totalLength = (requiredBlocks - 1) * blockSize;
                requiredBlocks++;
            } else {
                e.setOffset(blockPtr);
            }
            e.setBlock(requiredBlocks - 1);
            totalLength += e.getLength();
        }

        int dataOffset = (HEADER_SIZE + (entries.size() * ENTRY_SIZE));
        // Round up to nearest multiple of 512
        int multiple = 512;
        dataOffset = ((dataOffset + multiple - 1) / multiple) * multiple;
        int totalSize = dataOffset + (requiredBlocks * blockSize);

        ByteBuffer buf = ByteBuffer.allocate(totalSize);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        LOG.log(Level.INFO, "Saving to {0}", buf);
        buf.putInt(HEADER_MAGIC);
        int version = 1;
        buf.putInt(version);
        buf.putInt(requiredBlocks);
        buf.putInt(blockSize);
        buf.putInt(entries.size());
        buf.putInt(dataOffset);

        LOG.log(Level.FINE,
                "Version: {0}, Blocks: {1}, BlockSize: {2}, DirectorySize: {3}, DataOffset: {4}",
                new Object[] {version, requiredBlocks, blockSize, entries.size(), dataOffset});

        int i = 0;
        for(CaptionEntry e : entries) {
            buf.putInt(e.getKey());
            buf.putInt(e.getBlock());
            buf.putShort((short) (e.getOffset() & 0xFFFF));
            buf.putShort((short) (e.getLength() & 0xFFFF));
            LOG.log(Level.FINE, "Saving {0}, {1} ({2}->{3})", new Object[] {
                i++, e.getKey(), e.getOffset(), e.getOffset() + e.getLength()});
        }
        buf.put(new byte[dataOffset - buf.position()]);
        byte[] nul = new byte[2];
        for(CaptionEntry e : entries) {
            int p = (dataOffset + (e.getBlock() * blockSize) + e.getOffset());
            buf.position(p);
            buf.put(e.getValue().getBytes(VALUE_CHARSET));
            buf.put(nul);
        }
        buf.put(new byte[totalSize - buf.position()]);
        buf.flip();
        LOG.log(Level.INFO, "Saved to {0}", buf);
        return buf;
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
        VDF v = new VDF();
        v.readExternal(new FileInputStream(new File(file)), "UTF-16");
        ArrayList<CaptionEntry> children = new ArrayList<CaptionEntry>();
        ArrayList<Property> props = (v.getRoot().get(0).get(1)).getProperties();
        ArrayList<String> usedKeys = new ArrayList<String>();
        for(int i = props.size() - 1; i >= 0; i--) {
            Property p = props.get(i);
            LOG.log(Level.FINER, "Adding {0}", p);
            CaptionEntry e = new CaptionEntry();
            String key = p.getKey().replaceAll("\"", "");
            if(key.equals("//") || key.equals("\\n") || usedKeys.contains(key)) {
                LOG.log(Level.WARNING, "Discarding: {0}", key);
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

        /**
         * Set byte length of value. Has no effect if not a multiple of 2
         * @param length 
         */
        public void setLength(int length) {
            if(length % 2 != 0) {
                return;
            }
            this.length = length;
            if(this.value != null) {
                this.value = value.substring(0, (length / 2) - 1);
            }
        }

        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String str) {
            this.value = str;
            this.length = (str.length() + 1) * 2;
        }

        @Override
        public String toString() {
            return new StringBuilder()
                    .append("[H: ").append(key)
                    .append(", b: ").append(block)
                    .append(", o: ").append(offset)
                    .append(", l: ").append(length).append("]")
                    .append("(").append(trueKey != null ? "'" + trueKey + "'" : "?").append(")")
                    .append(" = '").append(value).append("'").toString();
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