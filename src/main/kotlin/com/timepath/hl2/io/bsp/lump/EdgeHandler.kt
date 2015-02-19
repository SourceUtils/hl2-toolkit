package com.timepath.hl2.io.bsp.lump

import com.timepath.hl2.io.bsp.Lump
import com.timepath.hl2.io.bsp.LumpHandler
import com.timepath.io.OrderedInputStream
import com.timepath.io.struct.Struct

import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger

class EdgeHandler : LumpHandler<List<Edge>> {

    throws(javaClass<IOException>())
    override fun handle(l: Lump, `in`: OrderedInputStream): List<Edge> {
        try {
            return (l.length / Struct.sizeof(Edge())).indices.map {
                `in`.readStruct<Edge>(Edge())
            }
        } catch (ex: InstantiationException) {
            LOG.log(Level.SEVERE, null, ex)
        } catch (ex: IllegalAccessException) {
            LOG.log(Level.SEVERE, null, ex)
        }
        return emptyList()
    }

    class object {

        private val LOG = Logger.getLogger(javaClass<EdgeHandler>().getName())
        private val MAX_MAP_EDGES = 256000
    }
}
