package com.timepath.hl2.io.bsp.lump

import com.timepath.hl2.io.bsp.Lump
import com.timepath.hl2.io.bsp.LumpHandler
import com.timepath.io.OrderedInputStream
import com.timepath.io.struct.Struct

import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger

class FaceHandler : LumpHandler<List<Face>> {

    throws(IOException::class)
    override fun handle(l: Lump, `in`: OrderedInputStream): List<Face> {
        try {
            return (0..(l.length / Struct.sizeof(Face())) - 1).map {
                `in`.readStruct<Face>(Face())
            }
        } catch (ex: InstantiationException) {
            LOG.log(Level.SEVERE, null, ex)
        } catch (ex: IllegalAccessException) {
            LOG.log(Level.SEVERE, null, ex)
        }

        return emptyList()
    }

    companion object {

        private val LOG = Logger.getLogger(javaClass<FaceHandler>().getName())
        private val MAX_MAP_FACES = 65536
    }
}
