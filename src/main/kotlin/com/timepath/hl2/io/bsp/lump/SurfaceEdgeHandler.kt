package com.timepath.hl2.io.bsp.lump

import com.timepath.hl2.io.bsp.Lump
import com.timepath.hl2.io.bsp.LumpHandler
import com.timepath.io.OrderedInputStream
import java.io.IOException

class SurfaceEdgeHandler : LumpHandler<IntArray> {

    throws(IOException::class)
    override fun handle(l: Lump, `in`: OrderedInputStream): IntArray {
        val e = IntArray(l.length / 4)
        for (i in e.indices) {
            e[i] = `in`.readInt()
        }
        return e
    }

    companion object {

        private val MAX_MAP_SURFEDGES = 512000
    }
}
