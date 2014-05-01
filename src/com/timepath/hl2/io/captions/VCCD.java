package com.timepath.hl2.io.captions;

import com.timepath.io.OrderedInputStream;
import com.timepath.steam.io.VDF1;
import com.timepath.steam.io.util.Property;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

/**
 * https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/utils/captioncompiler/captioncompiler.cpp
 * https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/public/captioncompiler.h
 * <p/>
 * @author andrew
 */
public class VCCD {

    private static final int COMPILED_CAPTION_FILEID = (('V') | ('C' << 8) | ('C' << 16) | ('D' << 24));

    private static final int COMPILED_CAPTION_VERSION = 1;

    private static final int ENTRY_SIZE = (4 * 2) + (2 * 2);

    private static final int HEADER_SIZE = (4 * 6);

    private static final Logger LOG = Logger.getLogger(VCCD.class.getName());

    private static final int MAX_BLOCK_BITS = 13;

    private static final int MAX_BLOCK_SIZE = 1 << MAX_BLOCK_BITS;

    private static final Charset VALUE_CHARSET = Charset.forName("UTF-16LE");

    public static int hash(String in) {
        CRC32 crc = new CRC32();
        crc.update(in.toLowerCase().getBytes());
        return (int) crc.getValue();
    }

    public static List<VCCDEntry> load(InputStream is) throws IOException {
        return load(new OrderedInputStream(is));
    }

    /**
     * Import a UCS-2 (UTF-16) LE encoded VDF containing caption tokens
     *
     * @param is
     *
     * @return
     */
    public static List<VCCDEntry> parse(InputStream is) {
        VDF1 v = new VDF1();
        v.readExternal(is, "UTF-16");
        List<VCCDEntry> children = new LinkedList<VCCDEntry>();
        List<Property> props = (v.getRoot().get(0).get(1)).getProperties();
        List<String> usedKeys = new LinkedList<String>();
        for(int i = props.size() - 1; i >= 0; i--) { // Do it in reverse to make overriding easier
            Property p = props.get(i);
            LOG.log(Level.FINER, "Adding {0}", p.toString());
            VCCDEntry e = new VCCDEntry();
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

    public static void save(List<VCCDEntry> entries, OutputStream os) throws IOException {
        ByteBuffer buf = save(entries);
        byte[] bytes = new byte[buf.capacity()];
        buf.get(bytes);
        os.write(bytes);
        os.close();
    }

    private static List<VCCDEntry> load(OrderedInputStream ois) throws IOException {
        LOG.log(Level.INFO, "Loading from {0}", ois);
        ois.order(ByteOrder.LITTLE_ENDIAN);

        if(ois.readInt() != COMPILED_CAPTION_FILEID) {
            LOG.severe("Header mismatch");
        }
        int version = ois.readInt();
        if(version != COMPILED_CAPTION_VERSION) {
            LOG.log(Level.WARNING, "Unsupported version: {0}", version);
        }
        int blocks = ois.readInt();
        int blockSize = ois.readInt();
        int totalEntries = ois.readInt();
        int dataOffset = ois.readInt();
        LOG.log(Level.FINE,
                "Version: {0}, Blocks: {1}, BlockSize: {2}, DirectorySize: {3}, DataOffset: {4}",
                new Object[] {version, blocks, blockSize, totalEntries, dataOffset});

        VCCDEntry[] entries = new VCCDEntry[totalEntries];
        for(int i = 0; i < entries.length; i++) {
            VCCDEntry e = new VCCDEntry();
            e.setHash(ois.readInt());
            e.setBlock(ois.readInt());
            e.setOffset(ois.readShort());
            e.setLength(ois.readShort());
            entries[i] = e;
            LOG.log(Level.FINEST, "Loading {0}, {1} ({2}->{3})", new Object[] {
                i, e.getHash(), e.getOffset(), e.getOffset() + e.getLength()});
        }
        ois.skipTo(dataOffset);
        for(VCCDEntry e : entries) {
            ois.skipTo(dataOffset + (e.block * blockSize) + e.offset);
            int size = e.length - 2;
            byte[] chars = new byte[size];
            ois.read(chars);
            e.setValue(new String(chars, VALUE_CHARSET));
        }
        // The rest of the file is useless, 0's or otherwise
        LOG.log(Level.INFO, "Loaded from {0}", ois);
        return Arrays.asList(entries);
    }

    private static ByteBuffer save(List<VCCDEntry> entries) throws UnsupportedEncodingException {
        int requiredBlocks = 0, blockSize = MAX_BLOCK_SIZE;
        if(!entries.isEmpty()) { // Don't waste time if empty
            Collections.sort(entries); // Ensure alphabetical order

            VCCDEntry longest = null;
            int thisLength, totalLength = 0, waste, totalWaste = 0;
            for(VCCDEntry e : entries) { // Pack into blocks
                thisLength = e.getLength();
                if(thisLength >= blockSize) {
                    LOG.log(Level.WARNING, "Token overflow: {0}", e);
                    continue;
                }
                // XXX: The official compiler will not use the last byte in a block
                if(totalLength + thisLength >= requiredBlocks * blockSize) { // If overflow
                    waste = (requiredBlocks * blockSize) - totalLength; // Move to end of block
                    totalWaste += waste;
                    totalLength += waste;
                    requiredBlocks++; // Expand
                }
                e.setBlock(requiredBlocks - 1); // Zero indexed
                e.setOffset(totalLength % blockSize);
                totalLength += thisLength;

                if(longest == null || longest.getLength() < e.getLength()) {
                    longest = e;
                }
            }
            LOG.log(Level.INFO, "Found {0} strings", entries.size());
            LOG.log(Level.INFO, "Longest string ''{0}'' = ({1})",
                    new Object[] {longest.getKey(), longest.getLength()});
            LOG.log(Level.INFO, "{0} bytes wasted",
                    new Object[] {totalWaste});
        }

        int dataOffset = (HEADER_SIZE + (entries.size() * ENTRY_SIZE));
        // Round up to nearest multiple of 512
        int multiple = 512;
        dataOffset = ((dataOffset + multiple - 1) / multiple) * multiple;
        int totalSize = dataOffset + (requiredBlocks * blockSize);

        ByteBuffer buf = ByteBuffer.allocate(totalSize);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        LOG.log(Level.INFO, "Saving to {0}", buf);
        buf.putInt(COMPILED_CAPTION_FILEID);
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
        for(VCCDEntry e : entries) {
            buf.putInt(e.getHash());
            buf.putInt(e.getBlock());
            buf.putShort((short) (e.getOffset() & 0xFFFF));
            buf.putShort((short) (e.getLength() & 0xFFFF));
            LOG.log(Level.FINEST, "Saving #{0} ({1}) - block: {2}, region: {3} + {4} -> {5}", new Object[] {
                ++i, e.getKey(), e.getBlock(), e.getOffset(), e.getLength(), e.getOffset() + e.getLength()});
        }
        buf.put(new byte[dataOffset - buf.position()]);
        byte[] nul = "\0".getBytes(VALUE_CHARSET);
        for(VCCDEntry e : entries) {
            int p = (dataOffset + (e.getBlock() * blockSize) + e.getOffset());
            buf.position(p);
            buf.put(e.getValue().getBytes(VALUE_CHARSET));
            buf.put(nul);
        }
        buf.put(new byte[totalSize - buf.position()]); // Padding
        buf.flip();
        LOG.log(Level.INFO, "Saved to {0}", buf);
        return buf;
    }

    private VCCD() {
    }

    public static final class VCCDEntry implements Comparable<VCCDEntry> {

        private static final Logger LOG = Logger.getLogger(VCCDEntry.class.getName());

        private int block;

        private int hash;

        private String key;

        private int length;

        private int offset;

        private String value;

        public VCCDEntry(String key, String value) {
            setKey(key);
            setValue(value);
        }
        
        public VCCDEntry(int hash, String value) {
            setHash(hash);
            setValue(value);
        }

        private VCCDEntry() {

        }

        public int compareTo(VCCDEntry t) {
            String e1 = this.getKey();
            if(e1 == null) {
                e1 = "";
            }
            String e2 = t.getKey();
            if(e2 == null) {
                e2 = "";
            }
            return e1.compareToIgnoreCase(e2);
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof VCCDEntry) {
                VCCDEntry o = (VCCDEntry) obj;
                if(hash != o.hash) {
                    return false;
                }
                if((value != null && o.value == null) || (o.value != null && value == null)) {
                    return false;
                }
                if(value == null && o.value == null) {
                    return true;
                }
                return value.equals(o.value);
            }
            return false;
        }
        
        

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 71 * hash + this.hash;
            hash = 71 * hash + (this.value != null ? this.value.hashCode() : 0);
            return hash;
        }

        public int getBlock() {
            return block;
        }

        public void setBlock(int block) {
            this.block = block;
        }

        public int getHash() {
            return hash;
        }

        public void setHash(int key) {
            hash = key;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String str) {
            hash = hash(str);
            key = str;
        }

        public int getLength() {
            return length;
        }

        /**
         * Set byte length of value. Has no effect if not a multiple of 2
         * <p/>
         * @param length
         */
        public void setLength(int length) {
            if(length % 2 != 0) {
                return;
            }
            this.length = length;
            if(value != null) {
                value = value.substring(0, (length / 2) - 1);
            }
        }

        public int getOffset() {
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String str) {
            this.value = str;
            this.length = (str.length() + 1) * 2;
        }

        @Override
        public String toString() {
            return MessageFormat.format("[H: {0}, b: {1}, o: {2}, l: {3}]({4}) = '{5}'",
                                        hash, block, offset, length, key != null ? "'" + key + "'" : '?', value);
        }

    }

}
