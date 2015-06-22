package com.timepath.hl2.io.demo

import com.timepath.io.BitBuffer


private val MAX_OSPATH = 260

class TupleMap<K, V>(l: MutableList<Pair<K, V>> = linkedListOf()) : MutableList<Pair<K, V>> by l {
    fun set(k: K, v: V) = add(k to v) let { Unit }
}

public fun PacketHandler(f: (BitBuffer, TupleMap<Any, Any>, HL2DEM, Int) -> Boolean): PacketHandler {
    return object : PacketHandler {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int)
                = f(bb, l, demo, lengthBits)
    }
}
