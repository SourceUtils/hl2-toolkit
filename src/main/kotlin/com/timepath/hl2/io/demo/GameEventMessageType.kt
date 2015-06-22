package com.timepath.hl2.io.demo

import com.timepath.io.BitBuffer

public enum class GameEventMessageType(private val i: Int, val parse: (BitBuffer) -> Any?) {

    /** Marks the end of an event description */
    END(0, { throw UnsupportedOperationException() }),
    /** A zero terminated string */
    STRING(1, { it.getString() }),
    /** Float, 32 bit */
    FLOAT(2, { it.getFloat() }),
    /** Signed int, 32 bit */
    LONG(3, { it.getInt() }),
    /** Signed int, 16 bit */
    SHORT(4, { it.getShort() }),
    /** Unsigned int, 8 bit */
    BYTE(5, { it.getByte() }),
    /** Unsigned int, 1 bit */
    BOOL(6, { it.getBoolean() }),
    /** Any data, but not networked to clients */
    LOCAL(7, { null });

    companion object {
        fun get(i: Int) = values().firstOrNull { it.i == i }
    }
}
