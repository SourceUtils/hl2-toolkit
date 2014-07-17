package com.timepath.hl2.io.demo;

import com.timepath.io.OrderedInputStream;
import com.timepath.io.struct.StructField;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.ByteOrder;
import java.text.MessageFormat;
import java.util.zip.InflaterInputStream;

/**
 * @author TimePath
 * @see <a>http://forums.steampowered.com/forums/showthread.php?t=1882941</a>
 */
public class ReplayDMX {

    public static ReplayDMX load(InputStream is) throws IOException, InstantiationException, IllegalAccessException {
        return new ReplayDMX(new BufferedInputStream(is));
    }

    ReplayDMX(InputStream is) throws IOException, IllegalAccessException, InstantiationException {
        System.out.println("header:");
        OrderedInputStream in = new OrderedInputStream(is);
        in.order(ByteOrder.LITTLE_ENDIAN);
        SessionInfoHeader info = in.readStruct(new SessionInfoHeader());
        System.out.println("version: " + info.version);
        System.out.println("session name: " + info.sessionName);
        System.out.println("currently recording?: " + info.recording);
        System.out.println("# blocks: " + info.numBlocks);
        System.out.println("compressor: " + CompressorType.get(info.compressorType));
        System.out.println("md5 digest: " + md5(info.hash));
        System.out.println("payload size (compressed): " + info.payloadSize);
        System.out.println("payload size (uncompressed): " + info.payloadSizeUC);
        System.out.println("blocks:");
        System.out.println("index\tstatus\tMD5\t\t\t\t\t\t\t\t\tcompressor\tsize (uncompressed)\tsize (compressed)");
        in = new OrderedInputStream(new BufferedInputStream(new InflaterInputStream(is))); // TODO: LZSS
        in.order(ByteOrder.LITTLE_ENDIAN);
        for(int i = 0; i < info.numBlocks; i++) {
            RecordingSessionBlockSpec spec = in.readStruct(new RecordingSessionBlockSpec());
            System.out.println(MessageFormat.format("{0}\t\t{1}\t{2}\t{3}\t{4}\t\t{5}\t\t\t\t\t{6}",
                                                    i,
                                                    spec.reconstruction,
                                                    spec.remoteStatus,
                                                    md5(spec.hash),
                                                    CompressorType.get(spec.compressorType),
                                                    spec.fileSize,
                                                    spec.uncompressedSize));
        }
    }

    private static String md5(byte[] hash) {
        BigInteger bi = new BigInteger(1, hash);
        return String.format("%0" + ( hash.length * 2 ) + "x", bi);
    }

    static enum CompressorType {
        INVALID,
        LZSS,
        BZ2;

        public static CompressorType get(int i) {
            return i >= 0 && i < values().length - 1 ? values()[i + 1] : INVALID;
        }
    }

    static class SessionInfoHeader {

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
