package com.timepath.hl2.io.demo

import com.timepath.io.BitBuffer

import java.util.Collections
import java.util.LinkedHashMap
import java.util.logging.Logger

class GameEvent(bb: BitBuffer) {
    public val declarations: Map<String, GameEventMessageType>
    public val name: String

    {
        name = bb.getString()
        val decl = LinkedHashMap<String, GameEventMessageType>(0)
        while (true) {
            val entryType = bb.getBits(3).toInt()
            if (entryType == 0) {
                // End of event description
                break
            }
            val entryName = bb.getString()
            decl.put(entryName, GameEventMessageType[entryType])
        }
        declarations = Collections.unmodifiableMap<String, GameEventMessageType>(decl)
    }

    public fun parse(bb: BitBuffer): Map<String, Any> {
        val values = LinkedHashMap<String, Any>(declarations)
        for (entry in declarations.entrySet()) {
            values.put(entry.getKey(), entry.getValue().parse(bb))
        }
        return values
    }

    override fun toString(): String {
        return name + ": " + declarations
    }

    class object {

        private val LOG = Logger.getLogger(javaClass<GameEvent>().getName())
    }
}
