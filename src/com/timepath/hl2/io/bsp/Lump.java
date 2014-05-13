package com.timepath.hl2.io.bsp;

import com.timepath.hl2.io.bsp.lump.LumpType;
import com.timepath.io.struct.StructField;

import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class Lump {

    private static final Logger LOG = Logger.getLogger(Lump.class.getName());
    /**
     * Length of lump (bytes)
     */
    @StructField(index = 1)
    public int length;
    /**
     * Offset into file (bytes)
     */
    @StructField(index = 0)
    public int offset;
    LumpType type;
    /**
     * Lump ident code. Usually \0\0\0\0, else uncompressed lump data size in integer form, then LZMA
     */
    @StructField(index = 3)
    private int ident;
    /**
     * Lump format version
     */
    @StructField(index = 2)
    private int version;

    @Override
    public String toString() {
        return type.toString();
    }

    /**
     * BSP files for console platforms such as PlayStation 3 and Xbox 360 usually have their lumps compressed with LZMA
     *
     * @return True if compressed
     */
    boolean isCompressed() {
        return ident != 0;
    }

    /**
     * Unused members of the lump_t array (those that have no data to point to) have all elements set to zero
     *
     * @return True if not used
     */
    boolean isEmpty() {
        return offset == 0 && length == 0 && version == 0 && ident == 0;
    }
}
