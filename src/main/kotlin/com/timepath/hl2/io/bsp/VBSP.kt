package com.timepath.hl2.io.bsp

import com.timepath.Logger
import com.timepath.hl2.io.bsp.lump.Edge
import com.timepath.hl2.io.bsp.lump.Face
import com.timepath.hl2.io.bsp.lump.LumpType
import com.timepath.io.OrderedInputStream
import com.timepath.steam.io.storage.ACF
import com.timepath.vfs.provider.zip.ZipFileProvider
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.util.HashMap
import java.util.LinkedList
import kotlin.platform.platformStatic

public class VBSP(header: BSP.Header, input: OrderedInputStream) : BSP(header, input) {

    override fun process() = processBasic()

    private fun done(vertices: List<Float>?, indices: List<Int>?) {
        if ((indices != null) && indices.isNotEmpty()) {
            val new = ByteBuffer.allocateDirect(indices.size() * 4).asIntBuffer()
            indices.forEach { new.put(it) }
            new.flip()
            this.indices = new
            LOG.info { "Map: indices ${indices.size()}, triangles ${indices.size() / 3}" }
        }
        if ((vertices != null) && !vertices.isEmpty()) {
            val new = ByteBuffer.allocateDirect(vertices.size() * 4).asFloatBuffer()
            vertices.forEach { new.put(it) }
            new.flip()
            this.vertices = new
            LOG.info { "Map: vertices ${vertices.size()}" }
        }
    }

    fun processBasic() {
        val vertices = LinkedList<Float>()
        val bspVertices = getLump<FloatBuffer>(LumpType.LUMP_VERTEXES)
        while (bspVertices!!.hasRemaining()) {
            vertices.add(bspVertices.get())
        }
        done(vertices, null)
    }

    /** https://github.com/toji/webgl-source/blob/master/js/source-bsp.js#L567 */
    public fun processToji() {
        val bspVertices = getLump<FloatBuffer>(LumpType.LUMP_VERTEXES)!!
        LOG.info { "Vertices: ${bspVertices.capacity()}" }
        val bspEdges = getLump<Array<Edge>>(LumpType.LUMP_EDGES)!!
        LOG.info { "Edges: ${bspEdges.size()}" }
        val bspSurfEdges = getLump<IntArray>(LumpType.LUMP_SURFEDGES)!!
        LOG.info { "Surfedges: ${bspSurfEdges.size()}" }
        val bspFaces = getLump<Array<Face>>(LumpType.LUMP_FACES)!!
        LOG.info { "Faces: ${bspFaces.size()}" }
        val vertices = LinkedList<Float>()
        val indices = LinkedList<Int>()
        val vertexBase = 0
        var rootPoint = 0
        var rootVertId = 0

        fun compileGpuVertex(verts: FloatBuffer, pos: Int, vertices: MutableCollection<Float>): Int {
            val index = vertices.size() / 3
            verts.position(pos)
            repeat(3) { vertices.add(verts.get()) }
            return index
        }

        for (face in bspFaces) {
            val edgeId = face.firstedge
            val vertLookupTable = HashMap<Int, Int>(0)
            for (i in 0..face.numedges.toInt() - 1) {
                val surfEdge = bspSurfEdges[edgeId + 1]
                val edge = bspEdges[Math.abs(surfEdge)]
                val reverse = surfEdge >= 0
                var vertId: Int
                val pointB: Int
                if (i == 0) {
                    rootVertId = edge.v[if (reverse) 0 else 1].toInt()
                    rootPoint = compileGpuVertex(bspVertices, rootVertId, vertices)
                    vertLookupTable.put(rootVertId, rootPoint)
                    vertId = edge.v[if (reverse) 1 else 0].toInt()
                    pointB = compileGpuVertex(bspVertices, vertId, vertices)
                    vertLookupTable.put(vertId, pointB)
                } else {
                    vertId = edge.v[if (reverse) 0 else 1].toInt()
                    if (vertId == rootVertId) {
                        continue
                    }
                    val pointA: Int
                    if (vertLookupTable.containsKey(vertId)) {
                        pointA = vertLookupTable[vertId]
                    } else {
                        pointA = compileGpuVertex(bspVertices, vertId, vertices)
                        vertLookupTable.put(vertId, pointA)
                    }
                    vertId = edge.v[if (reverse) 1 else 0].toInt()
                    if (vertId == rootVertId) {
                        continue
                    }
                    if (vertLookupTable.containsKey(vertId)) {
                        pointB = vertLookupTable[vertId]
                    } else {
                        pointB = compileGpuVertex(bspVertices, vertId, vertices)
                        vertLookupTable.put(vertId, pointB)
                    }
                    indices.add(rootPoint - vertexBase)
                    indices.add(pointA - vertexBase)
                    indices.add(pointB - vertexBase)
                }
            }
        }
        done(vertices, indices)
    }

    /** https://github.com/w23/OpenSource/blob/master/src/BSP.cpp#L222 */
    public fun processW23() {
        val bspVertices = getLump<FloatBuffer>(LumpType.LUMP_VERTEXES)!!
        LOG.info({ "Vertices: ${bspVertices.capacity()}" })
        val bspEdges = getLump<Array<Edge>>(LumpType.LUMP_EDGES)!!
        LOG.info({ "Edges: ${bspEdges.size()}" })
        val bspSurfEdges = getLump<IntArray>(LumpType.LUMP_SURFEDGES)!!
        LOG.info({ "Surfedges: ${bspSurfEdges.size()}" })
        // https://github.com/w23/OpenSource/blob/master/src/BSP.cpp#L253
        for (i in bspSurfEdges.indices) {
            bspSurfEdges[i] = (if (bspSurfEdges[i] >= 0) bspEdges[bspSurfEdges[i]].v[0] else bspEdges[-bspSurfEdges[i]].v[1]).toInt()
        }
        val bspFaces = getLump<Array<Face>>(LumpType.LUMP_FACES)!!
        LOG.info({ "Faces: ${bspFaces.size()}" })
        val vertices = LinkedList<Float>()
        val indices = LinkedList<Int>()
        for (face in bspFaces) {
            val edgeId = face.firstedge
            // https://github.com/w23/OpenSource/blob/master/src/BSP.cpp#L347
            val index_shift = vertices.size()
            for (i in 0..face.numedges.toInt() - 1) {
                bspVertices.position(bspSurfEdges[edgeId + i] * 3)
                run {
                    var j = 0
                    while (j++ < 3) {
                        vertices.add(bspVertices.get())
                    }
                }
                if (i >= 2) {
                    indices.add(index_shift)
                    indices.add((index_shift + i) - 1)
                    indices.add(index_shift + i)
                }
            }
        }
        done(vertices, indices)
    }

    companion object {

        private val LOG = Logger()

        public platformStatic fun main(args: Array<String>) {
            val map = BSP.load(ACF.fromManifest(440).query("tf/maps/ctf_2fort.bsp")!!.openStream()!!)!!
            LOG.info { "Revision: ${map.revision}" }
            map.getLump<String>(LumpType.LUMP_ENTITIES)?.let {
                LOG.info { it }
            }
            map.getLump<ZipFileProvider>(LumpType.LUMP_PAKFILE)?.let {
                LOG.info { it.name }
            }
            map.getLump<Any>(LumpType.LUMP_VERTEXES)
        }

    }
}
