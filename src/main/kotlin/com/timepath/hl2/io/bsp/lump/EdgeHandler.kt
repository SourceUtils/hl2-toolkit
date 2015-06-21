package com.timepath.hl2.io.bsp.lump

import com.timepath.Logger
import com.timepath.hl2.io.bsp.Lump
import com.timepath.hl2.io.bsp.LumpHandler
import com.timepath.io.OrderedInputStream
import com.timepath.io.struct.Struct
import java.io.IOException
import java.util.logging.Level

class EdgeHandler : LumpHandler<List<Edge>> {

    throws(IOException::class)
    override fun handle(l: Lump, `in`: OrderedInputStream): List<Edge> {
        try {
            return (0..(l.length / Struct.sizeof(Edge())) - 1).map {
                `in`.readStruct<Edge>(Edge())
            }
        } catch (ex: InstantiationException) {
            LOG.log(Level.SEVERE, { null }, ex)
        } catch (ex: IllegalAccessException) {
            LOG.log(Level.SEVERE, { null }, ex)
        }
        return emptyList()
    }

    companion object {

        private val LOG = Logger()
        private val MAX_MAP_EDGES = 256000
    }
}
