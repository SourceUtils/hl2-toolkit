package com.timepath.hl2.io.bsp.lump

import com.timepath.hl2.io.bsp.Lump
import com.timepath.hl2.io.bsp.LumpHandler
import com.timepath.io.OrderedInputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.FloatBuffer

class VertexHander : LumpHandler<FloatBuffer> {

    throws(IOException::class)
    override fun handle(l: Lump, `in`: OrderedInputStream): FloatBuffer {
        val verts = ByteBuffer.allocateDirect(l.length)
        val vertBuf = ByteArray(l.length)
        `in`.readFully(vertBuf)
        verts.put(vertBuf)
        verts.flip()
        return verts.asFloatBuffer()
    }

    companion object {

        private val MAX_MAP_VERTS = 65536
    }
}
