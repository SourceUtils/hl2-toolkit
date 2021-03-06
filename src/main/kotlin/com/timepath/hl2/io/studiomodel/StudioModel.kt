package com.timepath.hl2.io.studiomodel


import com.timepath.Logger
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.logging.Level

public class StudioModel @throws(IOException::class) constructor
(mdlStream: InputStream, vvdStream: InputStream, vtxStream: InputStream) {
    public val mdl: MDL?
    private val vtx: VTX?
    private val vvd: VVD?
    private val indexBuffer: ByteBuffer?

    init {
        mdl = MDL.load(mdlStream)
        vvd = VVD.load(vvdStream)
        vtx = VTX.load(vtxStream)
        val lod = 0
        setRootLOD(lod)
        indexBuffer = buildIndices(lod)
    }

    private fun buildIndices(lodId: Int): ByteBuffer? {
        val indices = ByteArrayOutputStream()
        var indexOffset = 0
        for (i in vtx!!.bodyParts.indices) {
            val bodyPart = vtx.bodyParts[i]
            val mdlBodyPart = mdl!!.mdlBodyParts[i]
            if (bodyPart.models.isEmpty()) {
                continue
            }
            val model = bodyPart.models[0]
            val mdlModel = mdlBodyPart.models!![0]
            val lod = model.lods[lodId]
            for (j in lod.meshes.indices) {
                val mesh = lod.meshes[j]
                val mdlMesh = mdlModel.meshes!![j]
                for (stripGroup in mesh.stripGroups) {
                    val vertTable = stripGroup.verts
                    stripGroup.indexOffset = indexOffset++
                    val sb = stripGroup.indexBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    for (l in 0..stripGroup.numIndices - 1) {
                        val vertTableIndex = sb.get().toInt()
                        val index = vertTable[vertTableIndex].origMeshVertID.toInt() + mdlModel.vertexoffset + mdlMesh.vertexoffset
                        val s = index.toShort()
                        try {
                            indices.write(byteArrayOf((s.toInt() and 0xFF).toByte(), ((s.toInt() and 0xFF00) shr 8).toByte(), ((s.toInt() and 0xFF0000) shr 16).toByte(), ((s.toInt() and -16777216) shr 24).toByte()))
                        } catch (ex: IOException) {
                            LOG.log(Level.SEVERE, { null }, ex)
                            return null
                        }

                    }
                }
            }
        }
        val bytes = indices.toByteArray()
        val buf = ByteBuffer.allocateDirect(bytes.size())
        buf.put(bytes).flip()
        return buf
    }

    private fun setRootLOD(rootLOD: Int) {
        @suppress("NAME_SHADOWING")
        var rootLOD = rootLOD
        mdl!!
        val header = mdl.header
        val bodyParts = mdl.mdlBodyParts
        if ((header.numAllowedRootLODs > 0) && (rootLOD >= header.numAllowedRootLODs)) {
            rootLOD = header.numAllowedRootLODs.toInt() - 1
        }
        var vertexoffset = 0
        for (bodyPart in bodyParts) {
            for (model in bodyPart.models!!) {
                var totalMeshVertices = 0
                for (meshId in model.meshes!!.indices) {
                    val mesh = model.meshes!![meshId]
                    mesh.numvertices = mesh.vertexdata.numLODVertexes[rootLOD]
                    mesh.vertexoffset = totalMeshVertices
                    totalMeshVertices += mesh.numvertices
                }
                model.numvertices = totalMeshVertices
                model.vertexoffset = vertexoffset
                vertexoffset += totalMeshVertices
            }
        }
    }

    public val indices: IntBuffer
        get() = indexBuffer!!.asIntBuffer()

    public val normals: FloatBuffer
        get() = vvd!!.normalBuffer.asFloatBuffer()

    public val tangents: FloatBuffer
        get() = vvd!!.tangentBuffer.asFloatBuffer()

    public val textureCoordinates: FloatBuffer
        get() = vvd!!.uvBuffer

    public val vertices: FloatBuffer
        get() = vvd!!.vertexBuffer.asFloatBuffer()

    companion object {

        val MAX_NUM_BONES_PER_VERT = 3
        val MAX_NUM_LODS = 8
        private val LOG = Logger()
    }
}
