package com.timepath.hl2.io.captions;

import com.timepath.io.OrderedInputStream;
import com.timepath.steam.io.VDF;
import com.timepath.steam.io.VDFNode;
import com.timepath.steam.io.VDFNode.VDFProperty;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.CRC32;

/**
 * @author TimePath
 * @see <a href="https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/utils/captioncompiler
 * /captioncompiler.cpp">the reference implementation</a> and its <a href="https://github.com/ValveSoftware/source
 * -sdk-2013/blob/master/mp/src/public/captioncompiler.h">header</a>
 */
public class VCCD {

    /**
     * VCCD
     */
    private static final int COMPILED_CAPTION_FILEID = 0x44434356;
    /**
     * DCCV
     */
    private static final int COMPILED_CAPTION_FILEID_XBOX = 0x56434344;
    private static final int COMPILED_CAPTION_VERSION = 1;
    /**
     * ( 4 * 2 ) + ( 2 * 2 )
     */
    private static final int ENTRY_SIZE = 12;
    /**
     * 4 * 6
     */
    private static final int HEADER_SIZE = 24;
    private static final Logger LOG = Logger.getLogger(VCCD.class.getName());
    private static final int MAX_BLOCK_BITS = 13;
    private static final int MAX_BLOCK_SIZE = 1 << MAX_BLOCK_BITS;

    private VCCD() {
    }

    public static int hash(@NotNull String in) {
        @NotNull CRC32 crc = new CRC32();
        crc.update(in.toLowerCase().getBytes(Charset.forName("UTF-8")));
        return (int) crc.getValue();
    }

    @Nullable
    public static List<VCCDEntry> load(@NotNull InputStream is) throws IOException {
        return load(new OrderedInputStream(is));
    }

    @Nullable
    private static List<VCCDEntry> load(@NotNull OrderedInputStream ois) throws IOException {
        LOG.log(Level.INFO, "Loading from {0}", ois);
        ois.order(ByteOrder.LITTLE_ENDIAN);
        int header = ois.readInt();
        Charset encoding;
        if (header == COMPILED_CAPTION_FILEID) {
            encoding = Charset.forName("UTF-16LE");
        } else if (header == COMPILED_CAPTION_FILEID_XBOX) {
            encoding = Charset.forName("UTF-16BE");
            ois.order(ByteOrder.BIG_ENDIAN);
        } else {
            LOG.severe("Header mismatch");
            return null;
        }
        int version = ois.readInt();
        if (version != COMPILED_CAPTION_VERSION) {
            LOG.log(Level.WARNING, "Unsupported version: {0}", version);
        }
        int blocks = ois.readInt();
        int blockSize = ois.readInt();
        int totalEntries = ois.readInt();
        int dataOffset = ois.readInt();
        LOG.log(Level.FINE,
                "Version: {0}, Blocks: {1}, BlockSize: {2}, DirectorySize: {3}, DataOffset: {4}",
                new Object[]{version, blocks, blockSize, totalEntries, dataOffset});
        @NotNull VCCDEntry[] entries = new VCCDEntry[totalEntries];
        for (int i = 0; i < entries.length; i++) {
            @NotNull VCCDEntry e = new VCCDEntry();
            e.setHash(ois.readInt());
            e.setBlock(ois.readInt());
            e.setOffset(ois.readShort());
            e.setLength(ois.readShort());
            entries[i] = e;
            LOG.log(Level.FINEST, "Loading {0}, {1} ({2}->{3})", new Object[]{
                    i, e.getHash(), e.getOffset(), e.getOffset() + e.getLength()
            });
        }
        ois.skipTo(dataOffset);
        for (@NotNull VCCDEntry e : entries) {
            ois.skipTo(dataOffset + (e.block * blockSize) + e.offset);
            int size = e.length - 2;
            @NotNull byte[] chars = new byte[size];
            ois.read(chars);
            e.setValue(new String(chars, encoding));
        }
        // The rest of the file is useless, 0's or otherwise
        LOG.log(Level.INFO, "Loaded from {0}", ois);
        return Arrays.asList(entries);
    }

    /**
     * Import a UCS-2 (UTF-16) LE encoded VDF containing caption tokens
     *
     * @param is
     * @return
     */
    @NotNull
    public static List<VCCDEntry> parse(@NotNull InputStream is) throws IOException {
        @NotNull VDFNode v = VDF.load(is, StandardCharsets.UTF_16);
        @NotNull List<VCCDEntry> children = new LinkedList<>();
        final VDFNode vdfNode = v.get("lang", "Tokens");
        if(vdfNode == null) return Collections.emptyList();
        @NotNull List<VDFProperty> props = vdfNode.getProperties();
        @NotNull Collection<String> usedKeys = new LinkedList<>();
        for (int i = props.size() - 1; i >= 0; i--) { // do it in reverse to make overriding easier. TODO: use iterator
            VDFProperty p = props.get(i);
            LOG.log(Level.FINER, "Adding {0}", p.toString());
            @NotNull VCCDEntry e = new VCCDEntry();
            String key = p.getKey();
            if (usedKeys.contains(key) || "//".equals(key) || "\\n".equals(key)) {
                LOG.log(Level.WARNING, "Discarding: {0}", key);
                continue;
            }
            usedKeys.add(key);
            e.setKey(key);
            e.setValue((String) p.getValue());
            children.add(e);
        }
        Collections.sort(children);
        return children;
    }

    public static void save(@NotNull List<VCCDEntry> entries, @NotNull OutputStream os) throws IOException {
        save(entries, os, false, false);
    }

    public static void save(@NotNull List<VCCDEntry> entries, @NotNull OutputStream os, boolean byteswap, boolean smallBlocks) throws IOException {
        ByteBuffer buf = save(entries, byteswap, smallBlocks);
        @NotNull byte[] bytes = new byte[buf.capacity()];
        buf.get(bytes);
        os.write(bytes);
        os.close();
    }

    private static ByteBuffer save(@NotNull List<VCCDEntry> entries, boolean byteswap, boolean smallBlocks) {
        int requiredBlocks = 0;
        int blockSize = MAX_BLOCK_SIZE;
        if (smallBlocks) blockSize /= 2;
        if (!entries.isEmpty()) { // Don't waste time if empty
            Collections.sort(entries); // Ensure alphabetical order
            @Nullable VCCDEntry longest = null;
            int totalLength = 0, totalWaste = 0;
            for (@NotNull VCCDEntry e : entries) { // Pack into blocks
                int thisLength = e.getLength();
                if (thisLength >= blockSize) {
                    LOG.log(Level.WARNING, "Token overflow: {0}", e);
                    continue;
                }
                // XXX: The official compiler will not use the last byte in a block
                if ((totalLength + thisLength) >= (requiredBlocks * blockSize)) { // If overflow
                    int waste = (requiredBlocks * blockSize) - totalLength;
                    totalWaste += waste;
                    totalLength += waste;
                    requiredBlocks++; // Expand
                }
                e.setBlock(requiredBlocks - 1); // Zero indexed
                e.setOffset(totalLength % blockSize);
                totalLength += thisLength;
                if ((longest == null) || (longest.getLength() < e.getLength())) {
                    longest = e;
                }
            }
            LOG.log(Level.INFO, "Found {0} strings", entries.size());
            LOG.log(Level.INFO,
                    "Longest string ''{0}'' = ({1})",
                    new Object[]{longest.getKey(), longest.getLength()});
            LOG.log(Level.INFO, "{0} bytes wasted", new Object[]{totalWaste});
        }
        int dataOffset = HEADER_SIZE + (entries.size() * ENTRY_SIZE);
        // Round up to nearest multiple of 512
        int multiple = 512;
        dataOffset = ((dataOffset + multiple) - 1) / multiple * multiple;
        int totalSize = dataOffset + (requiredBlocks * blockSize);
        ByteBuffer buf = ByteBuffer.allocate(totalSize);
        buf.order(byteswap ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);
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
                new Object[]{version, requiredBlocks, blockSize, entries.size(), dataOffset});
        int i = 0;
        for (@NotNull VCCDEntry e : entries) {
            buf.putInt(e.getHash());
            buf.putInt(e.getBlock());
            buf.putShort((short) (e.getOffset() & 0xFFFF));
            buf.putShort((short) (e.getLength() & 0xFFFF));
            LOG.log(Level.FINEST, "Saving #{0} ({1}) - block: {2}, region: {3} + {4} -> {5}", new Object[]{
                    ++i, e.getKey(), e.getBlock(), e.getOffset(), e.getLength(), e.getOffset() + e.getLength()
            });
        }
        buf.put(new byte[dataOffset - buf.position()]);
        Charset encoding = byteswap ? StandardCharsets.UTF_16BE : StandardCharsets.UTF_16LE;
        @NotNull byte[] nul = "\0".getBytes(encoding);
        for (@NotNull VCCDEntry e : entries) {
            int p = dataOffset + (e.getBlock() * blockSize) + e.getOffset();
            buf.position(p);
            buf.put(e.getValue().getBytes(encoding));
            buf.put(nul);
        }
        buf.put(new byte[totalSize - buf.position()]); // Padding
        buf.flip();
        LOG.log(Level.INFO, "Saved to {0}", buf);
        return buf;
    }

    public static final class VCCDEntry implements Comparable<VCCDEntry> {

        private static final Logger LOG = Logger.getLogger(VCCDEntry.class.getName());
        private int block;
        private int hash;
        private String key;
        private int length;
        private int offset;
        private String value;

        public VCCDEntry(@NotNull String key, @NotNull String value) {
            setKey(key);
            setValue(value);
        }

        public VCCDEntry(int hash, @NotNull String value) {
            this.hash = hash;
            setValue(value);
        }

        private VCCDEntry() {
        }

        @Override
        public int compareTo(@NotNull VCCDEntry t) {
            String e1 = key;
            if (e1 == null) {
                e1 = "";
            }
            String e2 = t.key;
            if (e2 == null) {
                e2 = "";
            }
            return e1.compareToIgnoreCase(e2);
        }

        public String getKey() {
            return key;
        }

        public void setKey(@NotNull String str) {
            hash = hash(str);
            key = str;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 71 * hash + this.hash;
            hash = 71 * hash + ((value != null) ? value.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof VCCDEntry) {
                @NotNull VCCDEntry o = (VCCDEntry) obj;
                if (hash != o.hash) {
                    return false;
                }
                if (((value != null) && (o.value == null)) || ((o.value != null) && (value == null))) {
                    return false;
                }
                if ((value == null) && (o.value == null)) {
                    return true;
                }
                return value.equals(o.value);
            }
            return false;
        }

        @NotNull
        @Override
        public String toString() {
            return MessageFormat.format("[H: {0}, b: {1}, o: {2}, l: {3}]({4}) = '{5}'",
                    hash,
                    block,
                    offset,
                    length,
                    (key != null) ? ('\'' + key + '\'') : '?',
                    value);
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

        public int getLength() {
            return length;
        }

        /**
         * Set byte length of value. Has no effect if not a multiple of 2
         *
         * @param length
         */
        public void setLength(int length) {
            if ((length % 2) != 0) {
                return;
            }
            this.length = length;
            if (value != null) {
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

        public void setValue(@NotNull String str) {
            value = str;
            length = (str.length() + 1) * 2;
        }
    }
}
