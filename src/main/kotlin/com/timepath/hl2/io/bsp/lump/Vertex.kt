package com.timepath.hl2.io.bsp.lump

import com.timepath.hl2.io.bsp.Lump
import com.timepath.hl2.io.bsp.LumpHandler
import com.timepath.io.OrderedInputStream
import java.nio.ByteBuffer
import java.nio.FloatBuffer

private val MAX_MAP_VERTS = 65536

class Vertex {
    class Hander : LumpHandler<FloatBuffer> {
        override fun invoke(l: Lump, ois: OrderedInputStream): FloatBuffer {
            val verts = ByteBuffer.allocateDirect(l.length)
            val vertBuf = ByteArray(l.length)
            ois.readFully(vertBuf)
            verts.put(vertBuf)
            verts.flip()
            return verts.asFloatBuffer()
        }
    }
}
