package com.timepath.hl2.io.bsp

import com.timepath.hl2.io.bsp.lump.LumpType
import com.timepath.io.struct.StructField
import kotlin.properties.Delegates

public class Lump {
    /**
     * Length of lump (bytes)
     */
    StructField(index = 1)
    public var length: Int = 0
    /**
     * Offset into file (bytes)
     */
    StructField(index = 0)
    public var offset: Int = 0
    var type: LumpType by Delegates.notNull()
    /**
     * Lump ident code. Usually \0\0\0\0, else uncompressed lump data size in integer form, then LZMA
     */
    StructField(index = 3)
    private val ident: Int = 0
    /**
     * Lump format version
     */
    StructField(index = 2)
    private val version: Int = 0

    override fun toString(): String {
        return type.toString()
    }

    /**
     * BSP files for console platforms such as PlayStation 3 and Xbox 360 usually have their lumps compressed with LZMA
     *
     * @return True if compressed
     */
    fun isCompressed(): Boolean {
        return ident != 0
    }

    /**
     * Unused members of the lump_t array (those that have no data to point to) have all elements set to zero
     *
     * @return True if not used
     */
    fun isEmpty(): Boolean {
        return (offset == 0) && (length == 0) && (version == 0) && (ident == 0)
    }
}
