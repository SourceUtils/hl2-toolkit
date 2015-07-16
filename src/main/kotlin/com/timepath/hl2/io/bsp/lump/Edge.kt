package com.timepath.hl2.io.bsp.lump

import com.timepath.hl2.io.bsp.Lump
import com.timepath.hl2.io.bsp.LumpHandler
import com.timepath.io.OrderedInputStream
import com.timepath.io.struct.Struct
import com.timepath.io.struct.StructField

private val MAX_MAP_EDGES = 256000

public class Edge(
        /** Vertex indices */
        @StructField(0) val v: ShortArray = ShortArray(2)
) {
    class Handler : LumpHandler<List<Edge>> {
        override fun invoke(l: Lump, ois: OrderedInputStream): List<Edge> {
            return (0..(l.length / Struct.sizeof(Edge())) - 1).map {
                ois.readStruct<Edge>(Edge())
            }
        }
    }
}
