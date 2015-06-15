package com.timepath.hl2.io.bsp

import com.timepath.hl2.io.bsp.lump.Edge
import com.timepath.hl2.io.bsp.lump.Face
import com.timepath.hl2.io.bsp.lump.LumpType
import com.timepath.steam.io.storage.ACF
import com.timepath.vfs.provider.zip.ZipFileProvider
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.util.HashMap
import java.util.LinkedList
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.platform.platformStatic

/**
 * @author TimePath
 */
public class VBSP : BSP() {

    /**
     * @throws java.io.IOException
     * @see @see <a>https://github.com/toji/webgl-source/blob/master/js/source-bsp.js#L567</a>
     */
    throws(IOException::class)
    public fun processToji() {
        val bspVertices = getLump<FloatBuffer>(LumpType.LUMP_VERTEXES)
        LOG.log(Level.INFO, "Vertices: {0}", bspVertices!!.capacity())
        val bspEdges = getLump<Array<Edge>>(LumpType.LUMP_EDGES)
        LOG.log(Level.INFO, "Edges: {0}", bspEdges!!.size())
        val bspSurfEdges = getLump<IntArray>(LumpType.LUMP_SURFEDGES)
        LOG.log(Level.INFO, "Surfedges: {0}", bspSurfEdges!!.size())
        val bspFaces = getLump<Array<Face>>(LumpType.LUMP_FACES)
        LOG.log(Level.INFO, "Faces: {0}", bspFaces!!.size())
        val vertices = LinkedList<Float>()
        val indices = LinkedList<Int>()
        val vertexBase = 0
        var rootPoint = 0
        var rootVertId = 0
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

    private fun done(vertices: List<Float>?, indices: List<Int>?) {
        if ((indices != null) && !indices.isEmpty()) {
            val new = ByteBuffer.allocateDirect(indices.size() * 4).asIntBuffer()
            for (i in indices) {
                new.put(i)
            }
            new.flip()
            this.indices = new
            LOG.log(Level.INFO, "Map: indices {0}, triangles {1}", arrayOf<Any>(indices.size(), indices.size() / 3))
        }
        if ((vertices != null) && !vertices.isEmpty()) {
            val new = ByteBuffer.allocateDirect(vertices.size() * 4).asFloatBuffer()
            for (v in vertices) {
                new.put(v)
            }
            new.flip()
            this.vertices = new
            LOG.log(Level.INFO, "Map: vertices {0}", arrayOf<Any>(vertices.size()))
        }
    }

    /**
     * @throws java.io.IOException
     * @see <a>https://github.com/w23/OpenSource/blob/master/src/BSP.cpp#L222</a>
     */
    SuppressWarnings("empty-statement", "StatementWithEmptyBody")
    throws(IOException::class)
    public fun processW23() {
        val bspVertices = getLump<FloatBuffer>(LumpType.LUMP_VERTEXES)
        LOG.log(Level.INFO, "Vertices: {0}", bspVertices!!.capacity())
        val bspEdges = getLump<Array<Edge>>(LumpType.LUMP_EDGES)
        LOG.log(Level.INFO, "Edges: {0}", bspEdges!!.size())
        val bspSurfEdges = getLump<IntArray>(LumpType.LUMP_SURFEDGES)
        LOG.log(Level.INFO, "Surfedges: {0}", bspSurfEdges!!.size())
        // https://github.com/w23/OpenSource/blob/master/src/BSP.cpp#L253
        for (i in bspSurfEdges.indices) {
            bspSurfEdges[i] = (if (bspSurfEdges[i] >= 0) bspEdges[bspSurfEdges[i]].v[0] else bspEdges[-bspSurfEdges[i]].v[1]).toInt()
        }
        val bspFaces = getLump<Array<Face>>(LumpType.LUMP_FACES)
        LOG.log(Level.INFO, "Faces: {0}", bspFaces!!.size())
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

    throws(IOException::class)
    override fun process() {
        processBasic()
    }

    SuppressWarnings("empty-statement", "StatementWithEmptyBody")
    throws(IOException::class)
    fun processBasic() {
        val vertices = LinkedList<Float>()
        val bspVertices = getLump<FloatBuffer>(LumpType.LUMP_VERTEXES)
        while (bspVertices!!.hasRemaining()) {
            vertices.add(bspVertices.get())
        }
        done(vertices, null)
    }

    companion object {

        private val LOG = Logger.getLogger(javaClass<VBSP>().getName())

        throws(Exception::class)
        public platformStatic fun main(args: Array<String>) {
            val b = BSP.load(ACF.fromManifest(440).query("tf/maps/ctf_2fort.bsp")!!.openStream()!!)
            LOG.log(Level.INFO, "Revision: {0}", b!!.revision)
            // val ents = b.getLump<String>(LumpType.LUMP_ENTITIES)
            // System.out.println(ents);
            val z = b.getLump<ZipFileProvider>(LumpType.LUMP_PAKFILE)
            z?.let {
                LOG.info(it.name)
            }
            b.getLump<Any>(LumpType.LUMP_VERTEXES)
        }

        SuppressWarnings("empty-statement", "StatementWithEmptyBody")
        private fun compileGpuVertex(verts: FloatBuffer, pos: Int, vertices: MutableCollection<Float>): Int {
            val index = vertices.size() / 3
            verts.position(pos)
            run {
                var i = 0
                while (i++ < 3) {
                    vertices.add(verts.get())
                }
            }
            return index
        }
    }
}
