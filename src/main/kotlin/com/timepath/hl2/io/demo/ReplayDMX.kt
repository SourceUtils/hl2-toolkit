package com.timepath.hl2.io.demo

import com.timepath.io.OrderedInputStream
import com.timepath.io.struct.StructField
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.PrintStream
import java.math.BigInteger
import java.nio.ByteOrder
import java.text.MessageFormat
import kotlin.properties.Delegates

/**
 * for f in $(ls *.block|sort --version-sort); do echo -e "$f\t$(md5sum<$f)  $(wc -c<$f)\t$(bzcat $f|wc -c)"; done
 * bzcat *.block > joined.dem
 *
 * @see <a>http://forums.steampowered.com/forums/showthread.php?t=1882941</a>
 */
public class ReplayDMX private constructor(input: InputStream) {

    public val info: SessionInfoHeader
    public val blocks: List<RecordingSessionBlockSpec>

    init {
        val ois = OrderedInputStream(input).let {
            it.order(ByteOrder.LITTLE_ENDIAN)
            it
        }
        info = ois.readStruct<SessionInfoHeader>(SessionInfoHeader())

        val compressed = ByteArray(ois.available())
        ois.readFully(compressed)
        val ois2 = OrderedInputStream(ByteArrayInputStream(LZSS.inflate(compressed))).let {
            it.order(ByteOrder.LITTLE_ENDIAN)
            it
        }
        blocks = (0..info.numBlocks - 1).map {
            ois2.readStruct<RecordingSessionBlockSpec>(RecordingSessionBlockSpec())
        }
    }

    public fun print(out: PrintStream) {
        out.println("header:")
        out.println("version: ${info.version}")
        out.println("session name: ${info.sessionName}")
        out.println("currently recording?: ${info.recording}")
        out.println("# blocks: ${info.numBlocks}")
        out.println("compressor: ${CompressorType[info.compressorType]}")
        out.println("md5 digest: ${md5(info.hash)}")
        out.println("payload size (compressed): ${info.payloadSize}")
        out.println("payload size (uncompressed): ${info.payloadSizeUC}")
        out.println("blocks:")
        out.println("index\tstatus\tMD5\t\t\t\t\t\t\t\t\tcompressor\tsize (uncompressed)\tsize (compressed)")
        blocks.forEachIndexed { i, block ->
            out.println(MessageFormat.format("{0}\t\t{1}\t{2}\t{3}\t{4}\t\t{5}\t\t\t\t\t{6}",
                    i, block.reconstruction, block.remoteStatus, md5(block.hash),
                    CompressorType[block.compressorType.toInt()], block.fileSize, block.uncompressedSize))
        }
    }

    enum class CompressorType {
        INVALID,
        LZSS,
        BZ2;

        companion object {
            public fun get(i: Int): CompressorType {
                val values = values()
                return when (i) {
                    in 0..values.size() - 2 -> values[i + 1]
                    else -> INVALID
                }
            }
        }
    }

    private class SessionInfoHeader {
        StructField(0) var version: Byte = 0
        /** Name of session. */
        StructField(1, limit = MAX_SESSIONNAME_LENGTH - 1)
        var sessionName: String by Delegates.notNull()
        /** Is this session currently recording? */
        StructField(2, skip = 3) var recording: Boolean = false
        /** Number of blocks in the session so far if recording, or total if not recording. */
        StructField(3) var numBlocks: Int = 0
        /** [CompressorType.INVALID] if header is not compressed. */
        StructField(4) var compressorType: Int = 0
        /** MD5 digest on payload. */
        StructField(5) var hash = ByteArray(16)
        /** Size of the payload - the compressed payload if it's compressed */
        StructField(6) var payloadSize: Int = 0
        /** Size of the uncompressed payload, if its compressed, otherwise 0 */
        StructField(7) var payloadSizeUC: Int = 0
        StructField(8) var unused = ByteArray(128)

        companion object {
            val MAX_SESSIONNAME_LENGTH = 260
        }
    }

    private class RecordingSessionBlockSpec {
        StructField(0) var reconstruction: Int = 0
        StructField(1) var remoteStatus: Byte = 0
        StructField(2) var hash = ByteArray(16)
        StructField(3, skip = 2) var compressorType: Byte = 0
        StructField(4) var fileSize: Int = 0
        StructField(5) var uncompressedSize: Int = 0
        StructField(6) var unused = ByteArray(8)
    }

    companion object {

        public fun load(input: InputStream): ReplayDMX = ReplayDMX(BufferedInputStream(input))

        private fun md5(hash: ByteArray) = "%0${hash.size() * 2}x".format(BigInteger(1, hash))
    }
}
