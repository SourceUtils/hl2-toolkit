package com.timepath.hl2.io.captions

import com.timepath.io.OrderedInputStream
import com.timepath.steam.io.VDF

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.text.MessageFormat
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import java.util.zip.CRC32

/**
 * @author TimePath
 * @see <a href="https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/utils/captioncompiler
 * /captioncompiler.cpp">the reference implementation</a> and its <a href="https://github.com/ValveSoftware/source
 * -sdk-2013/blob/master/mp/src/public/captioncompiler.h">header</a>
 */
public object VCCD {

    /**
     * VCCD
     */
    private val COMPILED_CAPTION_FILEID = 1145258838
    /**
     * DCCV
     */
    private val COMPILED_CAPTION_FILEID_XBOX = 1447248708
    private val COMPILED_CAPTION_VERSION = 1
    /**
     * ( 4 * 2 ) + ( 2 * 2 )
     */
    private val ENTRY_SIZE = 12
    /**
     * 4 * 6
     */
    private val HEADER_SIZE = 24
    private val LOG = Logger.getLogger(javaClass<VCCD>().getName())
    private val MAX_BLOCK_BITS = 13
    private val MAX_BLOCK_SIZE = 1 shl MAX_BLOCK_BITS

    public fun hash(`in`: String): Int {
        val crc = CRC32()
        crc.update(`in`.toLowerCase().getBytes(Charset.forName("UTF-8")))
        return crc.getValue().toInt()
    }

    throws(javaClass<IOException>())
    public fun load(`is`: InputStream): List<VCCDEntry>? {
        return load(OrderedInputStream(`is`))
    }

    throws(javaClass<IOException>())
    private fun load(ois: OrderedInputStream): List<VCCDEntry>? {
        LOG.log(Level.INFO, "Loading from {0}", ois)
        ois.order(ByteOrder.LITTLE_ENDIAN)
        val header = ois.readInt()
        val encoding: Charset
        if (header == COMPILED_CAPTION_FILEID) {
            encoding = Charset.forName("UTF-16LE")
        } else if (header == COMPILED_CAPTION_FILEID_XBOX) {
            encoding = Charset.forName("UTF-16BE")
            ois.order(ByteOrder.BIG_ENDIAN)
        } else {
            LOG.severe("Header mismatch")
            return null
        }
        val version = ois.readInt()
        if (version != COMPILED_CAPTION_VERSION) {
            LOG.log(Level.WARNING, "Unsupported version: {0}", version)
        }
        val blocks = ois.readInt()
        val blockSize = ois.readInt()
        val totalEntries = ois.readInt()
        val dataOffset = ois.readInt()
        LOG.log(Level.FINE, "Version: {0}, Blocks: {1}, BlockSize: {2}, DirectorySize: {3}, DataOffset: {4}", array<Any>(version, blocks, blockSize, totalEntries, dataOffset))
        val entries = arrayOfNulls<VCCDEntry>(totalEntries)
        for (i in totalEntries.indices) {
            val e = VCCDEntry()
            e.hash = ois.readInt()
            e.block = ois.readInt()
            e.offset = ois.readShort().toInt()
            e.length = (ois.readShort().toInt())
            entries[i] = e
            LOG.log(Level.FINEST, "Loading {0}, {1} ({2}->{3})", array<Any>(i, e.hash, e.offset, e.offset + e.length))
        }
        ois.skipTo(dataOffset)
        for (e in entries) {
            e!!
            ois.skipTo(dataOffset + (e.block * blockSize) + e.offset)
            val size = e.length - 2
            val chars = ByteArray(size)
            ois.read(chars)
            e.value = (String(chars, encoding))
        }
        // The rest of the file is useless, 0's or otherwise
        LOG.log(Level.INFO, "Loaded from {0}", ois)
        return Arrays.asList<VCCDEntry>(*entries)
    }

    /**
     * Import a UCS-2 (UTF-16) LE encoded VDF containing caption tokens
     *
     * @param is
     * @return
     */
    throws(javaClass<IOException>())
    public fun parse(`is`: InputStream): List<VCCDEntry> {
        val v = VDF.load(`is`, StandardCharsets.UTF_16)
        val children = LinkedList<VCCDEntry>()
        val vdfNode = v["lang", "Tokens"]
        if (vdfNode == null) return listOf()
        val props = vdfNode.getProperties()
        val usedKeys = LinkedList<String>()
        run {
            var i = props.size() - 1
            while (i >= 0) {
                // do it in reverse to make overriding easier. TODO: use iterator
                val p = props[i]
                LOG.log(Level.FINER, "Adding {0}", p.toString())
                val e = VCCDEntry()
                val key = p.getKey()
                if (usedKeys.contains(key) || "//" == key || "\\n" == key) {
                    LOG.log(Level.WARNING, "Discarding: {0}", key)
                    continue
                }
                usedKeys.add(key)
                e.key = (key)
                e.value = (p.getValue() as String)
                children.add(e)
                i--
            }
        }
        Collections.sort<VCCDEntry>(children)
        return children
    }

    throws(javaClass<IOException>())
    public fun save(entries: List<VCCDEntry>, os: OutputStream) {
        save(entries, os, false, false)
    }

    throws(javaClass<IOException>())
    public fun save(entries: List<VCCDEntry>, os: OutputStream, byteswap: Boolean, smallBlocks: Boolean) {
        val buf = save(entries, byteswap, smallBlocks)
        val bytes = ByteArray(buf.capacity())
        buf.get(bytes)
        os.write(bytes)
        os.close()
    }

    private fun save(entries: List<VCCDEntry>, byteswap: Boolean, smallBlocks: Boolean): ByteBuffer {
        var requiredBlocks = 0
        var blockSize = MAX_BLOCK_SIZE
        if (smallBlocks) blockSize /= 2
        if (!entries.isEmpty()) {
            // Don't waste time if empty
            Collections.sort<VCCDEntry>(entries) // Ensure alphabetical order
            var longest: VCCDEntry? = null
            var totalLength = 0
            var totalWaste = 0
            for (e in entries) {
                // Pack into blocks
                val thisLength = e.length
                if (thisLength >= blockSize) {
                    LOG.log(Level.WARNING, "Token overflow: {0}", e)
                    continue
                }
                // XXX: The official compiler will not use the last byte in a block
                if ((totalLength + thisLength) >= (requiredBlocks * blockSize)) {
                    // If overflow
                    val waste = (requiredBlocks * blockSize) - totalLength
                    totalWaste += waste
                    totalLength += waste
                    requiredBlocks++ // Expand
                }
                e.block = requiredBlocks - 1 // Zero indexed
                e.offset = totalLength % blockSize
                totalLength += thisLength
                if ((longest == null) || (longest!!.length < e.length)) {
                    longest = e
                }
            }
            LOG.log(Level.INFO, "Found {0} strings", entries.size())
            LOG.log(Level.INFO, "Longest string ''{0}'' = ({1})", array(longest!!.key, longest!!.length))
            LOG.log(Level.INFO, "{0} bytes wasted", array<Any>(totalWaste))
        }
        var dataOffset = HEADER_SIZE + (entries.size() * ENTRY_SIZE)
        // Round up to nearest multiple of 512
        val multiple = 512
        dataOffset = ((dataOffset + multiple) - 1) / multiple * multiple
        val totalSize = dataOffset + (requiredBlocks * blockSize)
        val buf = ByteBuffer.allocate(totalSize)
        buf.order(if (byteswap) ByteOrder.BIG_ENDIAN else ByteOrder.LITTLE_ENDIAN)
        LOG.log(Level.INFO, "Saving to {0}", buf)
        buf.putInt(COMPILED_CAPTION_FILEID)
        val version = 1
        buf.putInt(version)
        buf.putInt(requiredBlocks)
        buf.putInt(blockSize)
        buf.putInt(entries.size())
        buf.putInt(dataOffset)
        LOG.log(Level.FINE, "Version: {0}, Blocks: {1}, BlockSize: {2}, DirectorySize: {3}, DataOffset: {4}", array<Any>(version, requiredBlocks, blockSize, entries.size(), dataOffset))
        var i = 0
        for (e in entries) {
            buf.putInt(e.hash)
            buf.putInt(e.block)
            buf.putShort((e.offset and 65535).toShort())
            buf.putShort((e.length and 65535).toShort())
            LOG.log(Level.FINEST, "Saving #{0} ({1}) - block: {2}, region: {3} + {4} -> {5}", array(++i, e.key, e.block, e.offset, e.length, e.offset + e.length))
        }
        buf.put(ByteArray(dataOffset - buf.position()))
        val encoding = if (byteswap) StandardCharsets.UTF_16BE else StandardCharsets.UTF_16LE
        val nul = "\u0000".getBytes(encoding)
        for (e in entries) {
            val p = dataOffset + (e.block * blockSize) + e.offset
            buf.position(p)
            buf.put(e.value!!.getBytes(encoding))
            buf.put(nul)
        }
        buf.put(ByteArray(totalSize - buf.position())) // Padding
        buf.flip()
        LOG.log(Level.INFO, "Saved to {0}", buf)
        return buf
    }


    public fun VCCDEntry(key: String, value: String): VCCDEntry {
        val __ = VCCDEntry()
        __.key = key
        __.value = value
        return __
    }

    public fun VCCDEntry(hash: Int, value: String): VCCDEntry {
        val __ = VCCDEntry()
        __.hash = hash
        __.value = value
        return __
    }

    public class VCCDEntry : Comparable<VCCDEntry> {
        public var block: Int = 0
        public var hash: Int = 0
        public var key: String? = null
            set(str) {
                hash = hash(str!!)
                key = str
            }
        public var length: Int = 0
            /**
             * Set byte length of value. Has no effect if not a multiple of 2
             *
             * @param length
             */
            set(length) {
                if ((length % 2) != 0) {
                    return
                }
                this.$length = length
                if (value != null) {
                    $value = value!!.substring(0, (length / 2) - 1)
                }
            }
        public var offset: Int = 0
        public var value: String? = null
            set(str) {
                $value = str!!
                $length = (str.length() + 1) * 2
            }

        override fun compareTo(other: VCCDEntry): Int {
            var e1 = key
            if (e1 == null) {
                e1 = ""
            }
            var e2 = other.key
            if (e2 == null) {
                e2 = ""
            }
            return e1!!.compareToIgnoreCase(e2!!)
        }

        override fun hashCode(): Int {
            var hash = 3
            hash = 71 * hash + this.hash
            hash = 71 * hash + (if ((value != null)) value!!.hashCode() else 0)
            return hash
        }

        override fun equals(other: Any?): Boolean {
            if (other is VCCDEntry) {
                if (hash != other.hash) {
                    return false
                }
                if (((value != null) && (other.value == null)) || ((other.value != null) && (value == null))) {
                    return false
                }
                if ((value == null) && (other.value == null)) {
                    return true
                }
                return value == other.value
            }
            return false
        }

        override fun toString(): String {
            return MessageFormat.format("[H: {0}, b: {1}, o: {2}, l: {3}]({4}) = '{5}'", hash, block, offset, length, if ((key != null)) ("\'$key\'") else '?', value)
        }

        companion object {

            private val LOG = Logger.getLogger(javaClass<VCCDEntry>().getName())
        }
    }
}
