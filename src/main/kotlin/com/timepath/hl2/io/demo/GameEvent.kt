package com.timepath.hl2.io.demo

import com.timepath.io.BitBuffer
import com.timepath.with

class GameEvent(bb: BitBuffer) {
    public val name: String
    public val declarations: Map<String, GameEventMessageType>

    init {
        name = bb.getString()
        declarations = linkedMapOf<String, GameEventMessageType>() with {
            while (true) {
                val evt = GameEventMessageType[bb.getBits(3).toInt()]
                if (evt == GameEventMessageType.END) break
                this[bb.getString()] = evt
            }
        }
    }

    public fun parse(bb: BitBuffer): Map<String, Any?> = declarations.mapValues { it.value.parse(bb) }

    override fun toString() = "$name: $declarations"

}
