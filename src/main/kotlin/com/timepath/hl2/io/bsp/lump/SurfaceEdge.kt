package com.timepath.hl2.io.bsp.lump

import com.timepath.hl2.io.bsp.Lump
import com.timepath.hl2.io.bsp.LumpHandler
import com.timepath.io.OrderedInputStream
import java.io.IOException

private val MAX_MAP_SURFEDGES = 512000

class SurfaceEdge {
    class Handler : LumpHandler<IntArray> {
        override fun invoke(l: Lump, ois: OrderedInputStream): IntArray {
            val e = IntArray(l.length / 4)
            for (i in e.indices) {
                e[i] = ois.readInt()
            }
            return e
        }
    }
}
