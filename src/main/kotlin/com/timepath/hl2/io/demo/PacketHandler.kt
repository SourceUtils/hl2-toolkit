package com.timepath.hl2.io.demo

import com.timepath.io.BitBuffer
import java.lang.reflect.Field

interface PacketHandler {

    public fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int = -1): Boolean {
        val o = read(bb, demo, lengthBits) ?: return false
        fun Field.getPrivate(instance: Any): Any? {
            val accessible = isAccessible()
            setAccessible(true)
            val value = get(instance)
            setAccessible(accessible)
            return value
        }
        o.javaClass.getDeclaredFields().forEach {
            it.getPrivate(o)?.let { value ->
                l[it.getName()] = value
            }
        }
        return true
    }

    public fun read(bb: BitBuffer, demo: HL2DEM, lengthBits: Int = -1): Any? = null
    override fun toString() = javaClass.getSimpleName()
}
