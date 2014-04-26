package com.timepath.hl2.io.bsp;

import com.timepath.io.struct.StructField;
import java.util.logging.Logger;

public class Lump {

    private static final Logger LOG = Logger.getLogger(Lump.class.getName());

    /**
     * Lump ident code. Usually \0\0\0\0, else uncompressed lump data size in integer form, then LZMA
     */
    @StructField(index = 3)
    int ident;

    /**
     * Length of lump (bytes)
     */
    @StructField(index = 1)
    int length;

    /**
     * Offset into file (bytes)
     */
    @StructField(index = 0)
    int offset;

    LumpType type;

    /**
     * Lump format version
     */
    @StructField(index = 2)
    int version;

    @Override
    public String toString() {
        return type.toString();
    }

    /**
     * BSP files for console platforms such as PlayStation 3 and Xbox 360 usually have their lumps compressed with LZMA
     * <p/>
     * @return True if compressed
     */
    boolean isCompressed() {
        return ident != 0;
    }

    /**
     * Unused members of the lump_t array (those that have no data to point to) have all elements set to zero
     * <p/>
     * @return True if not used
     */
    boolean isEmpty() {
        return offset == 0 && length == 0 && version == 0 && ident == 0;
    }

}
