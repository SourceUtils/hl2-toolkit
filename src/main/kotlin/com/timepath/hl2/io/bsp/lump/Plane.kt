package com.timepath.hl2.io.bsp.lump

import com.timepath.hl2.io.bsp.Lump
import com.timepath.hl2.io.bsp.LumpHandler
import com.timepath.hl2.io.util.Vector3f
import com.timepath.io.OrderedInputStream
import com.timepath.io.struct.Struct
import com.timepath.io.struct.StructField

private val MAX_MAP_PLANES = 65536

public class Plane(
        /** Normal vector */
        StructField(0) val normal: Vector3f = Vector3f(),
        /** Distance from origin */
        StructField(1) val dist: Float = 0f,
        /** Plane axis identifier */
        StructField(2) val type: Int = 0
) {
    class Handler : LumpHandler<List<Plane>> {
        override fun invoke(l: Lump, ois: OrderedInputStream): List<Plane> {
            return (0..(l.length / Struct.sizeof(Plane())) - 1).map {
                ois.readStruct<Plane>(Plane())
            }
        }
    }
}
