package com.timepath.hl2.io.demo

import com.timepath.hl2.io.demo.LZSS.LZSSException
import com.timepath.io.OrderedInputStream
import com.timepath.io.struct.StructField

import java.io.*
import java.math.BigInteger
import java.nio.ByteOrder
import java.text.MessageFormat
import kotlin.properties.Delegates

/**
 * for f in $(ls *.block|sort --version-sort); do echo -e "$f\t$(md5sum<$f)  $(wc -c<$f)\t$(bzcat $f|wc -c)"; done
 * bzcat *.block > joined.dem
 *
 * @author TimePath
 * @see <a>http://forums.steampowered.com/forums/showthread.php?t=1882941</a>
 */
public class ReplayDMX [throws(javaClass<IOException>(), javaClass<IllegalAccessException>(), javaClass<InstantiationException>(), javaClass<LZSSException>())]
private(`is`: InputStream) {

    public val info: SessionInfoHeader
    public val blocks: List<RecordingSessionBlockSpec>

    {
        var `in` = OrderedInputStream(`is`)
        `in`.order(ByteOrder.LITTLE_ENDIAN)
        info = `in`.readStruct<SessionInfoHeader>(SessionInfoHeader())
        val compressed = ByteArray(`in`.available())
        `in`.readFully(compressed)
        `in` = OrderedInputStream(ByteArrayInputStream(LZSS.inflate(compressed)))
        `in`.order(ByteOrder.LITTLE_ENDIAN)
        blocks = info.numBlocks.indices.map {
            `in`.readStruct<RecordingSessionBlockSpec>(RecordingSessionBlockSpec())
        }
    }

    public fun print(out: PrintStream) {
        out.println("header:")
        out.println("version: " + info.version)
        out.println("session name: " + info.sessionName)
        out.println("currently recording?: " + info.recording)
        out.println("# blocks: " + info.numBlocks)
        out.println("compressor: " + CompressorType.get(info.compressorType))
        out.println("md5 digest: " + md5(info.hash))
        out.println("payload size (compressed): " + info.payloadSize)
        out.println("payload size (uncompressed): " + info.payloadSizeUC)
        out.println("blocks:")
        out.println("index\tstatus\tMD5\t\t\t\t\t\t\t\t\tcompressor\tsize (uncompressed)\tsize (compressed)")
        run {
            var i = 0
            val blocksLength = blocks.size
            while (i < blocksLength) {
                val block = blocks[i]
                out.println(MessageFormat.format("{0}\t\t{1}\t{2}\t{3}\t{4}\t\t{5}\t\t\t\t\t{6}", i, block.reconstruction, block.remoteStatus, md5(block.hash), CompressorType.get(block.compressorType.toInt()), block.fileSize, block.uncompressedSize))
                i++
            }
        }
    }

    enum class CompressorType {
        INVALID
        LZSS
        BZ2

        class object {
            public fun get(i: Int): CompressorType {
                return if (i >= 0 && i < values().size() - 1) values()[i + 1] else INVALID
            }
        }
    }

    private class SessionInfoHeader () {
        StructField(index = 0)
        var version: Byte = 0
        /**
         * Name of session.
         */
        StructField(index = 1, limit = MAX_SESSIONNAME_LENGTH - 1)
        var sessionName: String by Delegates.notNull()
        /**
         * Is this session currently recording?
         */
        StructField(index = 2, skip = 3)
        var recording: Boolean = false
        /**
         * # blocks in the session so far if recording, or total if not recording.
         */
        StructField(index = 3)
        var numBlocks: Int = 0
        /**
         * {@link com.timepath.hl2.io.demo.ReplayDMX.CompressorType.INVALID} if header is not compressed.
         */
        SuppressWarnings("JavadocReference")
        StructField(index = 4)
        var compressorType: Int = 0
        /**
         * MD5 digest on payload.
         */
        StructField(index = 5)
        var hash = ByteArray(16)
        /**
         * Size of the payload - the compressed payload if it's compressed
         */
        StructField(index = 6)
        var payloadSize: Int = 0
        /**
         * Size of the uncompressed payload, if its compressed, otherwise 0
         */
        StructField(index = 7)
        var payloadSizeUC: Int = 0
        StructField(index = 8)
        var unused = ByteArray(128)

        class object {

            val MAX_SESSIONNAME_LENGTH = 260
        }
    }

    private class RecordingSessionBlockSpec () {

        StructField(index = 0)
        var reconstruction: Int = 0
        StructField(index = 1)
        var remoteStatus: Byte = 0
        StructField(index = 2)
        var hash = ByteArray(16)
        StructField(index = 3, skip = 2)
        var compressorType: Byte = 0
        StructField(index = 4)
        var fileSize: Int = 0
        StructField(index = 5)
        var uncompressedSize: Int = 0
        StructField(index = 6)
        var unused = ByteArray(8)
    }

    class object {

        throws(javaClass<IOException>(), javaClass<InstantiationException>(), javaClass<IllegalAccessException>(), javaClass<LZSSException>())
        public fun load(`is`: InputStream): ReplayDMX {
            return ReplayDMX(BufferedInputStream(`is`))
        }

        private fun md5(hash: ByteArray): String {
            val bi = BigInteger(1, hash)
            return java.lang.String.format("%0" + (hash.size() * 2) + "x", bi)
        }
    }
}
