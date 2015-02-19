package com.timepath.hl2.io.demo

import com.timepath.io.BitBuffer

/**
 * @author TimePath
 */
public enum class GameEventMessageType(i: Int) {
    /**
     * A zero terminated string
     */
    STRING : GameEventMessageType(1)
    /**
     * Float, 32 bit
     */
    FLOAT : GameEventMessageType(2)
    /**
     * Signed int, 32 bit
     */
    LONG : GameEventMessageType(3)
    /**
     * Signed int, 16 bit
     */
    SHORT : GameEventMessageType(4)
    /**
     * Unsigned int, 8 bit
     */
    BYTE : GameEventMessageType(5)
    /**
     * Unsigned int, 1 bit
     */
    BOOL : GameEventMessageType(6)
    /**
     * Any data, but not networked to clients
     */
    LOCAL : GameEventMessageType(7)

    class object {
        fun get(i: Int): GameEventMessageType? {
            val vals = values()
            if ((i < 1) || (i > vals.size())) {
                return null
            }
            return vals[i - 1]
        }
    }

    public fun parse(bb: BitBuffer): Any? {
        return null
    }
}
