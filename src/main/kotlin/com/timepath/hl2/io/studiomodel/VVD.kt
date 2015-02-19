package com.timepath.hl2.io.studiomodel

import com.timepath.DataUtils
import com.timepath.io.ByteBufferInputStream
import com.timepath.io.OrderedInputStream
import com.timepath.io.struct.StructField

import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.text.MessageFormat
import java.util.Arrays
import java.util.logging.Level
import java.util.logging.Logger

class VVD [throws(javaClass<IOException>(), javaClass<InstantiationException>(), javaClass<IllegalAccessException>())]
private(`in`: InputStream) {
    val vertexBuffer: ByteBuffer
    val normalBuffer: ByteBuffer
    val tangentBuffer: ByteBuffer
    val uvBuffer: FloatBuffer
    private val `is`: OrderedInputStream

    {
        `is` = OrderedInputStream(`in`)
        `is`.mark(Integer.MAX_VALUE)
        `is`.order(ByteOrder.LITTLE_ENDIAN)
        val header = `is`.readStruct<VertexFileHeader>(VertexFileHeader())
        LOG.log(VERBOSITY, "VertexFileHeader header = {0}", header.toString())
        val lod = 0
        val vertCount = header.numLODVertexes[lod]
        position(header.vertexDataStart)
        vertexBuffer = ByteBuffer.allocateDirect(vertCount * 3 * 4).order(ByteOrder.LITTLE_ENDIAN)
        normalBuffer = ByteBuffer.allocateDirect(vertCount * 4 * 4).order(ByteOrder.LITTLE_ENDIAN)
        uvBuffer = ByteBuffer.allocateDirect(vertCount * 2 * 4).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
        tangentBuffer = ByteBuffer.allocateDirect(vertCount * 4 * 4).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0..Math.max(header.numFixups, 1) - 1) {
            // at least once
            var sourceVertexID = 0
            var numVertexes = vertCount
            if (header.numFixups != 0) {
                // Fixup Table
                position(header.fixupTableStart + (i * 12))
                val fixlod = `is`.readInt() // used to skip culled root lod
                sourceVertexID = `is`.readInt() // absolute index from start of vertex/tangent blocks
                numVertexes = `is`.readInt()
                if (fixlod < lod) {
                    continue
                }
            }
            for (j in 0..numVertexes - 1) {
                // Vertex table, 48 byte rows
                position(header.vertexDataStart + ((sourceVertexID + j) * 48))
                // TODO: Bones
                val boneWeightBuf = ByteArray(3 * 4)
                `is`.readFully(boneWeightBuf)
                val boneIdBuf = ByteArray(4)
                `is`.readFully(boneIdBuf)
                val vertBuf = ByteArray(3 * 4)
                `is`.readFully(vertBuf)
                vertexBuffer.put(vertBuf)
                val normBuf = ByteArray(3 * 4)
                `is`.readFully(normBuf)
                normalBuffer.put(normBuf)
                val u = `is`.readFloat()
                val v = 1 - `is`.readFloat()
                uvBuffer.put(u).put(v)
                // Tangent table, 16 byte rows
                position(header.tangentDataStart + ((sourceVertexID + j) * 16))
                val tanBuf = ByteArray(4 * 4)
                `is`.readFully(tanBuf)
                tangentBuffer.put(tanBuf)
            }
        }
        vertexBuffer.flip()
        normalBuffer.flip()
        uvBuffer.flip()
        tangentBuffer.flip()
        LOG.log(VERBOSITY, "Underflow: {0}", array<Any>(`is`.available()))
    }

    private fun position(index: Int) {
        //        LOG.log(VERBOSITY, "seeking to {0}", index);
        try {
            `is`.reset()
            `is`.skipBytes(index - `is`.position())
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }

    }

    class VertexFileHeader {

        /**
         * MODEL_VERTEX_FILE_ID
         */
        StructField(index = 0)
        var id: Int = 0
        /**
         * MODEL_VERTEX_FILE_VERSION
         */
        StructField(index = 1)
        var version: Int = 0
        /**
         * same as studiohdr_t, ensures sync
         */
        StructField(index = 2)
        var checksum: Int = 0
        /**
         * num of valid lods
         */
        StructField(index = 3)
        var numLODs: Int = 0
        /**
         * num verts for desired root lod
         */
        StructField(index = 4)
        var numLODVertexes = IntArray(StudioModel.MAX_NUM_LODS)
        /**
         * num of vertexFileFixup_t
         */
        StructField(index = 5)
        var numFixups: Int = 0
        /**
         * offset from base to fixup table
         */
        StructField(index = 6)
        var fixupTableStart: Int = 0
        /**
         * offset from base to vertex block
         */
        StructField(index = 7)
        var vertexDataStart: Int = 0
        /**
         * offset from base to tangent block
         */
        StructField(index = 8)
        var tangentDataStart: Int = 0

        override fun toString(): String {
            return MessageFormat.format("\nid: {0}\nv: {1}\ncksum: {2}\nlods: {3}\nfixups: {4}\nfixoff: {5}\nvertoff: {6}\ntanoff: {7}", id, version, checksum, Arrays.toString(numLODVertexes), numFixups, fixupTableStart, vertexDataStart, tangentDataStart)
        }
    }

    class object {

        private val LOG = Logger.getLogger(javaClass<VVD>().getName())
        private val VERBOSITY = Level.FINE

        throws(javaClass<IOException>())
        public fun load(file: File): VVD? {
            LOG.log(Level.INFO, "Loading VVD {0}", file)
            return load(ByteBufferInputStream(DataUtils.mapFile(file)))
        }

        throws(javaClass<IOException>())
        public fun load(`in`: InputStream): VVD? {
            try {
                return VVD(BufferedInputStream(`in`))
            } catch (ex: InstantiationException) {
                LOG.log(Level.SEVERE, null, ex)
            } catch (ex: IllegalAccessException) {
                LOG.log(Level.SEVERE, null, ex)
            }

            return null
        }
    }
}
