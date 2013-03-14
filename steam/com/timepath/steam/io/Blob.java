package com.timepath.steam.io;

import com.timepath.DataUtils;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * http://trac.assembla.com/clientregistrytoolkit/browser/trunk/SteamReg/Blob
 *
 * @author timepath
 */
public class Blob {

    /**
     * Type:
     * short
     * 0x5001 = uncompressed
     * 0x4301 = compressed
     */
    /**
     * Compressed header:
     * int
     * compressed + 20 (size of header neglecting initial short)
     * int
     * dummy1
     * int
     * decompressed
     * int
     * dummy2
     * short
     * dummy3
     * byte[compressed]
     * perform zlib decompression (skip the first 2 bytes) to get byte[decompressed]
     */
    /**
     * Header:
     * uint
     * length + 10 (10 = size of header)
     * int padding
     * 4 bytes of nothing
     */
    /**
     * Node:
     * short
     * length
     * int
     * dataSize
     * byte[length]
     * name
     * byte[dataSize]
     * the actual data for this blob. May contain more blob data chunks
     */
    private static final Logger logger = Logger.getLogger(Blob.class.getName());

    private BlobNode blob;

    public Blob(File f) {
        try {
            RandomAccessFile rf = new RandomAccessFile(f, "r");
            this.blob = readBlob(rf);
        } catch(IOException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public String toString() {
        return blob.toString();
    }

    private static final int COMPRESSED = 0x4301;

    private static final int UNCOMPRESSED = 0x5001;

    public class BlobNode {

        public BlobNode() {
        }

        private short header;

        /**
         * @return the header
         */
        public short getHeader() {
            return header;
        }

        /**
         * @param header the header to set
         */
        public void setHeader(short header) {
            this.header = header;
        }

        private int length;

        /**
         * @return the length
         */
        public int getLength() {
            return length;
        }

        /**
         * @param length the length to set
         */
        public void setLength(int length) {
            this.length = length;
        }

        private String name;

        /**
         * @return the name
         */
        public String getName() {
            return name;
        }

        /**
         * @param name the name to set
         */
        public void setName(String name) {
            this.name = name;
        }

        public ArrayList<BlobNode> children = new ArrayList<BlobNode>();

        @Override
        public String toString() {
            return "'" + name + "'";
        }
    }

    /**
     * 1. Get data of current blob
     * 2. for all nested blobs, goto 1
     *
     * @param rf
     */
    private BlobNode readBlob(RandomAccessFile rf) throws IOException {
        BlobNode bn = new BlobNode();
        bn.setHeader(DataUtils.readLEShort(rf));
        switch(bn.getHeader()) {
            case COMPRESSED:
                break;
            case UNCOMPRESSED:
                readBlobUncompressed(rf, bn);
                break;
            default:
                break;
        }
        return bn;
    }

    private void readBlobUncompressed(RandomAccessFile rf, BlobNode bn) throws IOException {
        bn.setLength(DataUtils.readULEInt(rf) - 10);
        DataUtils.readULEInt(rf); // padding
        int nameLength = DataUtils.readULEShort(rf);
        int dataLength = DataUtils.readULEInt(rf);
        bn.setName(new String(DataUtils.readBytes(rf, nameLength)));
//        byte[] data = DataUtils.readBytes(rf, dataLength);
        bn.children.add(readBlob(rf));
    }
}