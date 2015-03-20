package com.timepath.hl2.io.bsp.lump

import com.timepath.hl2.io.bsp.Lump
import com.timepath.hl2.io.bsp.LumpHandler
import com.timepath.io.OrderedInputStream
import com.timepath.io.struct.Struct

import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger

class PlaneHandler : LumpHandler<List<Plane>> {

    throws(javaClass<IOException>())
    override fun handle(l: Lump, `in`: OrderedInputStream): List<Plane> {
        try {
            return (l.length / Struct.sizeof(Plane())).indices.map {
                `in`.readStruct<Plane>(Plane())
            }
        } catch (ex: InstantiationException) {
            LOG.log(Level.SEVERE, null, ex)
        } catch (ex: IllegalAccessException) {
            LOG.log(Level.SEVERE, null, ex)
        }

        return emptyList()
    }

    companion object {

        private val LOG = Logger.getLogger(javaClass<PlaneHandler>().getName())
        private val MAX_MAP_PLANES = 65536
    }
}
