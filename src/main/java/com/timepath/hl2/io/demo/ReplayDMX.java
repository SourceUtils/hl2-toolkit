package com.timepath.hl2.io.demo;

import com.timepath.hl2.io.demo.LZSS.LZSSException;
import com.timepath.io.OrderedInputStream;
import com.timepath.io.struct.StructField;

import java.io.*;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.text.MessageFormat;

/**
 * for f in $(ls *.block|sort --version-sort); do echo -e "$f\t$(md5sum<$f)  $(wc -c<$f)\t$(bzcat $f|wc -c)"; done
 * bzcat *.block > joined.dem
 *
 * @author TimePath
 * @see <a>http://forums.steampowered.com/forums/showthread.php?t=1882941</a>
 */
public class ReplayDMX {

    public final SessionInfoHeader           info;
    public final RecordingSessionBlockSpec[] blocks;

    public static ReplayDMX load(InputStream is)
    throws IOException, InstantiationException, IllegalAccessException, LZSSException
    {
        return new ReplayDMX(new BufferedInputStream(is));
    }

    public void print(PrintStream out) {
        out.println("header:");
        out.println("version: " + info.version);
        out.println("session name: " + info.sessionName);
        out.println("currently recording?: " + info.recording);
        out.println("# blocks: " + info.numBlocks);
        out.println("compressor: " + CompressorType.get(info.compressorType));
        out.println("md5 digest: " + md5(info.hash));
        out.println("payload size (compressed): " + info.payloadSize);
        out.println("payload size (uncompressed): " + info.payloadSizeUC);
        out.println("blocks:");
        out.println("index\tstatus\tMD5\t\t\t\t\t\t\t\t\tcompressor\tsize (uncompressed)\tsize (compressed)");
        for(int i = 0, blocksLength = blocks.length; i < blocksLength; i++) {
            RecordingSessionBlockSpec block = blocks[i];
            out.println(MessageFormat.format("{0}\t\t{1}\t{2}\t{3}\t{4}\t\t{5}\t\t\t\t\t{6}",
                                                    i,
                                                    block.reconstruction,
                                                    block.remoteStatus,
                                                    md5(block.hash),
                                                    CompressorType.get(block.compressorType),
                                                    block.fileSize,
                                                    block.uncompressedSize));
        }
    }

    private ReplayDMX(InputStream is) throws IOException, IllegalAccessException, InstantiationException, LZSSException {
        OrderedInputStream in = new OrderedInputStream(is);
        in.order(ByteOrder.LITTLE_ENDIAN);
        info = in.readStruct(new SessionInfoHeader());
        byte[] compressed = new byte[in.available()];
        in.readFully(compressed);
        in = new OrderedInputStream(new ByteArrayInputStream(LZSS.inflate(compressed)));
        in.order(ByteOrder.LITTLE_ENDIAN);
        blocks = new RecordingSessionBlockSpec[info.numBlocks];
        for(int i = 0; i < info.numBlocks; i++) {
            blocks[i] = in.readStruct(new RecordingSessionBlockSpec());
        }
    }

    private static String md5(byte[] hash) {
        BigInteger bi = new BigInteger(1, hash);
        return String.format("%0" + ( hash.length * 2 ) + "x", bi);
    }

    private static enum CompressorType {
        INVALID,
        LZSS,
        BZ2;

        public static CompressorType get(int i) {
            return i >= 0 && i < values().length - 1 ? values()[i + 1] : INVALID;
        }
    }

    private static class SessionInfoHeader {

        static final int MAX_SESSIONNAME_LENGTH = 260;
        @StructField(index = 0)
        byte    version;
        /**
         * Name of session.
         */
        @StructField(index = 1, limit = MAX_SESSIONNAME_LENGTH - 1)
        String  sessionName;
        /**
         * Is this session currently recording?
         */
        @StructField(index = 2, skip = 3)
        boolean recording;
        /**
         * # blocks in the session so far if recording, or total if not recording.
         */
        @StructField(index = 3)
        int     numBlocks;
        /**
         * {@link com.timepath.hl2.io.demo.ReplayDMX.CompressorType.INVALID} if header is not compressed.
         */
        @SuppressWarnings("JavadocReference")
        @StructField(index = 4)
        int     compressorType;
        /**
         * MD5 digest on payload.
         */
        @StructField(index = 5)
        byte[] hash = new byte[16];
        /**
         * Size of the payload - the compressed payload if it's compressed
         */
        @StructField(index = 6)
        int payloadSize;
        /**
         * Size of the uncompressed payload, if its compressed, otherwise 0
         */
        @StructField(index = 7)
        int payloadSizeUC;
        @StructField(index = 8)
        byte[] unused = new byte[128];

        private SessionInfoHeader() {}
    }

    private static class RecordingSessionBlockSpec {

        @StructField(index = 0)
        int  reconstruction;
        @StructField(index = 1)
        byte remoteStatus;
        @StructField(index = 2)
        byte[] hash = new byte[16];
        @StructField(index = 3, skip = 2)
        byte compressorType;
        @StructField(index = 4)
        int  fileSize;
        @StructField(index = 5)
        int  uncompressedSize;
        @StructField(index = 6)
        byte[] unused = new byte[8];

        private RecordingSessionBlockSpec() {}
    }
}
