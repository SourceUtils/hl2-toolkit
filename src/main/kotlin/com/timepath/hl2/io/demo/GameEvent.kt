package com.timepath.hl2.io.demo

import com.timepath.io.BitBuffer

class GameEvent(bb: BitBuffer) {
    public val name: String
    public val declarations: Map<String, GameEventMessageType>

    init {
        name = bb.getString()
        declarations = linkedMapOf<String, GameEventMessageType>().let {
            while (true) {
                val entryType = GameEventMessageType[bb.getBits(3).toInt()]
                if (entryType == GameEventMessageType.END) {
                    break
                }
                val entryName = bb.getString()
                it[entryName] = entryType
            }
            it
        }
    }

    public fun parse(bb: BitBuffer): Map<String, Any?> = declarations.mapValues { it.value.parse(bb) }

    override fun toString() = "$name: $declarations"

}
