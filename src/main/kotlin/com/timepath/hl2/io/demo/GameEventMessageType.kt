package com.timepath.hl2.io.demo

import com.timepath.io.BitBuffer

public enum class GameEventMessageType(private val i: Int) {

    /** Marks the end of an event description */
    END : GameEventMessageType(0) {
        override fun parse(bb: BitBuffer) = throw UnsupportedOperationException()
    }

    /** A zero terminated string */
    STRING : GameEventMessageType(1) {
        override fun parse(bb: BitBuffer) = bb.getString()
    }
    /** Float, 32 bit */
    FLOAT : GameEventMessageType(2) {
        override fun parse(bb: BitBuffer) = bb.getFloat()
    }
    /** Signed int, 32 bit */
    LONG : GameEventMessageType(3) {
        override fun parse(bb: BitBuffer) = bb.getInt()
    }
    /** Signed int, 16 bit */
    SHORT : GameEventMessageType(4) {
        override fun parse(bb: BitBuffer) = bb.getShort()
    }
    /** Unsigned int, 8 bit */
    BYTE : GameEventMessageType(5) {
        override fun parse(bb: BitBuffer) = bb.getByte()
    }
    /** Unsigned int, 1 bit */
    BOOL : GameEventMessageType(6) {
        override fun parse(bb: BitBuffer) = bb.getBoolean()
    }
    /** Any data, but not networked to clients */
    LOCAL : GameEventMessageType(7) {
        override fun parse(bb: BitBuffer) = null
    }

    public abstract fun parse(bb: BitBuffer): Any?

    companion object {
        fun get(i: Int): GameEventMessageType = values().first { it.i == i }
    }
}
