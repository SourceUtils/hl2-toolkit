package com.timepath.hl2.io.bsp.lump

import com.timepath.hl2.io.bsp.Lump
import com.timepath.hl2.io.bsp.LumpHandler
import com.timepath.io.OrderedInputStream

import java.io.IOException
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.util.logging.Logger

/**
 * @author TimePath
 */
class VertexHander : LumpHandler<FloatBuffer> {

    throws(javaClass<IOException>())
    override fun handle(l: Lump, `in`: OrderedInputStream): FloatBuffer {
        val verts = ByteBuffer.allocateDirect(l.length)
        val vertBuf = ByteArray(l.length)
        `in`.readFully(vertBuf)
        verts.put(vertBuf)
        verts.flip()
        return verts.asFloatBuffer()
    }

    class object {

        private val LOG = Logger.getLogger(javaClass<VertexHander>().getName())
        private val MAX_MAP_VERTS = 65536
    }
}
