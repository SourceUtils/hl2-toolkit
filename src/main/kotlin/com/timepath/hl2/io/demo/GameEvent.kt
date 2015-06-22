package com.timepath.hl2.io.demo

import com.timepath.io.BitBuffer

class GameEvent(bb: BitBuffer) {
    public val name: String
    public val declarations: Map<String, GameEventMessageType>

    init {
        name = bb.getString()
        declarations = sequence {
            val evt = GameEventMessageType[bb.getBits(3).toInt()]
            when (evt!!) {
                GameEventMessageType.END -> null
                else -> bb.getString() to evt
            }
        }.toList().toMap()
    }

    public fun parse(bb: BitBuffer): Map<String, Any?> = declarations.mapValues { it.value.parse(bb) }

    override fun toString() = "$name: $declarations"

}
