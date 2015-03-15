package com.timepath.hl2.io.studiomodel

import com.timepath.DataUtils
import com.timepath.hl2.io.util.Vector3f
import com.timepath.io.ByteBufferInputStream
import com.timepath.io.OrderedInputStream
import com.timepath.io.struct.Struct
import com.timepath.io.struct.StructField

import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteOrder
import java.util.ArrayList
import java.util.logging.Level
import java.util.logging.Logger

import kotlin.properties.Delegates

public class MDL [throws(javaClass<IOException>(), javaClass<InstantiationException>(), javaClass<IllegalAccessException>())]
private(`in`: InputStream) {
    public val header: StudioHeader
    val mdlBodyParts: MutableList<MStudioBodyParts>
    private val textureDirs: MutableList<MStudioTextureDir>
    private val textures: MutableList<MStudioTexture>
    private val `is`: OrderedInputStream
    private val verbosity = Level.FINE

    {
        `is` = OrderedInputStream(`in`)
        `is`.mark(Integer.MAX_VALUE)
        `is`.order(ByteOrder.LITTLE_ENDIAN)
        header = `is`.readStruct<StudioHeader>(StudioHeader())
        LOG.log(verbosity, "StudioHeader header = \"{0}\";", header.toString())
        LOG.log(verbosity, "MStudioTexture[] textures = new MStudioTexture[{0}];", header.numtextures)
        position(header.textureindex)
        textures = ArrayList<MStudioTexture>(header.numtextures)
        for (i in 0..header.numtextures - 1) {
            val offset = `is`.position()
            val tex = `is`.readStruct<MStudioTexture>(MStudioTexture())
            textures.add(tex)
            position(offset + tex.sznameindex)
            tex.textureName = `is`.readString()
            position(offset + Struct.sizeof(tex))
            LOG.log(verbosity, "textures[{0}] = \"{1}\";", array<Any>(i, tex.textureName!!))
        }
        LOG.log(verbosity, "MStudioTextureDir[] textureDirs = new MStudioTextureDir[{0}];", header.numcdtextures)
        position(header.cdtextureindex)
        textureDirs = ArrayList<MStudioTextureDir>(header.numcdtextures)
        for (i in 0..header.numcdtextures - 1) {
            val offset = `is`.position()
            val texDir = `is`.readStruct<MStudioTextureDir>(MStudioTextureDir())
            textureDirs.add(texDir)
            position(texDir.diroffset)
            texDir.textureDir = `is`.readString()
            position(offset + Struct.sizeof(texDir))
            LOG.log(verbosity, "textureDirs[{0}] = \"{1}\";", array<Any>(i, texDir.textureDir!!))
        }
        LOG.log(verbosity, "int[] skinTable = new int[{0}];", header.numskinref * header.numskinfamilies)
        position(header.skinindex)
        val skinTable = IntArray(header.numskinref * header.numskinfamilies)
        for (i in skinTable.indices) {
            skinTable[i] = `is`.readShort().toInt()
            LOG.log(verbosity, "skinTable[{0}] = {1};", array<Any>(i, skinTable[i]))
        }
        LOG.log(verbosity, "MStudioBodyParts[]")
        position(header.bodypartindex)
        mdlBodyParts = ArrayList<MStudioBodyParts>(header.numbodyparts)
        for (i in 0..header.numbodyparts - 1) {
            val bodyPart = `is`.readStruct<MStudioBodyParts>(MStudioBodyParts())
            mdlBodyParts.add(bodyPart)
            LOG.log(verbosity, "MStudioBodyParts[{0}/{1}].models[]", array<Any>(1 + i, header.numbodyparts))
            position(bodyPart.offset + bodyPart.modelindex)
            bodyPart.models = ArrayList<MStudioModel>(bodyPart.nummodels)
            for (j in 0..bodyPart.nummodels - 1) {
                val model = `is`.readStruct<MStudioModel>(MStudioModel())
                bodyPart.models!!.add(model)
                LOG.log(verbosity, "MStudioBodyParts[{0}/{1}].models[{2}/{3}].meshes[]", array<Any>(1 + i, header.numbodyparts, 1 + j, bodyPart.nummodels))
                position(model.offset + model.meshindex)
                model.meshes = ArrayList<MStudioMesh>(model.nummeshes)
                for (k in 0..model.nummeshes - 1) {
                    LOG.log(verbosity, "MStudioBodyParts[{0}/{1}].models[{2}/{3}].meshes[{4}/{5}]", array<Any>(1 + i, header.numbodyparts, 1 + j, bodyPart.nummodels, 1 + k, model.nummeshes))
                    val mesh = `is`.readStruct<MStudioMesh>(MStudioMesh())
                    model.meshes!!.add(mesh)
                }
                position(model.offset + Struct.sizeof(model))
            }
            position(bodyPart.offset + Struct.sizeof(bodyPart))
        }
    }

    private fun position(index: Int) {
        //        LOG.log(verbosity, "seeking to {0}", index);
        try {
            `is`.reset()
            `is`.skipBytes(index - `is`.position())
        } catch (ex: IOException) {
            LOG.log(Level.SEVERE, null, ex)
        }

    }

    private fun position(): Int {
        try {
            return `is`.position()
        } catch (ex: IOException) {
            return -1
        }

    }

    public class StudioHeader {

        StructField(index = 3, limit = 64)
        public var name: String? = null
        StructField(index = 0)
        var id: Int = 0
        StructField(index = 1)
        var version: Int = 0
        StructField(index = 2)
        var checksum: Int = 0
        StructField(index = 4)
        var length: Int = 0
        StructField(index = 5)
        var eyePosition: Vector3f? = null
        StructField(index = 6)
        var illumPosition: Vector3f? = null
        StructField(index = 7)
        var hullMin: Vector3f? = null
        StructField(index = 7)
        var hullMax: Vector3f? = null
        StructField(index = 8)
        var view_bbmin: Vector3f? = null
        StructField(index = 8)
        var view_bbmax: Vector3f? = null
        StructField(index = 9)
        var flags: Int = 0
        StructField(index = 10)
        var numBones: Int = 0
        StructField(index = 10)
        var boneIndex: Int = 0
        StructField(index = 11)
        var numBoneControllers: Int = 0
        StructField(index = 11)
        var boneControllerIndex: Int = 0
        StructField(index = 12)
        var numhitboxsets: Int = 0
        StructField(index = 12)
        var hitboxsetindex: Int = 0
        StructField(index = 13)
        var numlocalanim: Int = 0
        StructField(index = 13)
        var localanimindex: Int = 0
        StructField(index = 14)
        var numlocalseq: Int = 0
        StructField(index = 14)
        var localseqindex: Int = 0
        StructField(index = 15)
        var activitylistversion: Int = 0
        StructField(index = 16)
        var eventsindexed: Int = 0
        StructField(index = 17)
        var numtextures: Int = 0
        StructField(index = 17)
        var textureindex: Int = 0
        StructField(index = 18)
        var numcdtextures: Int = 0
        StructField(index = 18)
        var cdtextureindex: Int = 0
        StructField(index = 19)
        var numskinref: Int = 0
        StructField(index = 19)
        var numskinfamilies: Int = 0
        StructField(index = 19)
        var skinindex: Int = 0
        StructField(index = 20)
        var numbodyparts: Int = 0
        StructField(index = 20)
        var bodypartindex: Int = 0
        StructField(index = 21)
        var numlocalattachments: Int = 0
        StructField(index = 21)
        var localattachmentindex: Int = 0
        StructField(index = 22)
        var numlocalnodes: Int = 0
        StructField(index = 22)
        var localnodeindex: Int = 0
        StructField(index = 23)
        var localnodenameindex: Int = 0
        StructField(index = 24)
        var numflexdesc: Int = 0
        StructField(index = 24)
        var flexdescindex: Int = 0
        StructField(index = 25)
        var numflexcontrollers: Int = 0
        StructField(index = 25)
        var flexcontrollerindex: Int = 0
        StructField(index = 26)
        var numflexrules: Int = 0
        StructField(index = 26)
        var flexruleindex: Int = 0
        StructField(index = 27)
        var numikchains: Int = 0
        StructField(index = 27)
        var ikchainindex: Int = 0
        StructField(index = 28)
        var nummouths: Int = 0
        StructField(index = 28)
        var mouthindex: Int = 0
        StructField(index = 29)
        var numlocalposeparameters: Int = 0
        StructField(index = 29)
        var localposeparamindex: Int = 0
        StructField(index = 30)
        var surfacepropindex: Int = 0
        StructField(index = 31)
        var keyvalueindex: Int = 0
        StructField(index = 31)
        var keyvaluesize: Int = 0
        StructField(index = 32)
        var numlocalikautoplaylocks: Int = 0
        StructField(index = 32)
        var localikautoplaylockindex: Int = 0
        StructField(index = 33)
        var mass: Float = 0.toFloat()
        StructField(index = 34)
        var contents: Int = 0
        StructField(index = 35)
        var numincludemodels: Int = 0
        StructField(index = 35)
        var includemodelindex: Int = 0
        StructField(index = 36)
        var virtualModel: Int = 0 // skip
        StructField(index = 37)
        var szanimblocknameindex: Int = 0
        StructField(index = 38)
        var numanimblocks: Int = 0
        StructField(index = 38)
        var animblockindex: Int = 0
        StructField(index = 39)
        var animblockModel: Int = 0 // skip
        StructField(index = 40)
        var bonetablebynameindex: Int = 0
        StructField(index = 41)
        var pVertexBase: Int = 0
        StructField(index = 41)
        var pIndexBase: Int = 0 // skip
        StructField(index = 42)
        var constdirectionallightdot: Byte = 0
        StructField(index = 43)
        var rootLOD: Byte = 0
        StructField(index = 43)
        var numAllowedRootLODs: Byte = 0
        StructField(index = 44, skip = 5)
        var dummy1: Any? = null
        StructField(index = 45)
        var numflexcontrollerui: Int = 0
        StructField(index = 45)
        var flexcontrolleruiindex: Int = 0
        StructField(index = 46, skip = 8)
        var dummy2: Any? = null
        StructField(index = 47)
        var studiohdr2index: Int = 0
        StructField(index = 48, skip = 4)
        var dummy3: Any? = null

        override fun toString() = name!!
    }

    class MStudioTexture {

        StructField(index = 0)
        var sznameindex: Int = 0
        StructField(index = 0)
        var flags: Int = 0
        StructField(index = 0)
        var used: Int = 0
        StructField(index = 1, skip = 52)
        var dummy: Any? = null
        var textureName: String? = null
    }

    class MStudioTextureDir {

        StructField(index = 0)
        var diroffset: Int = 0
        var textureDir: String? = null
    }

    class MStudioMeshVertexData {

        StructField(index = 0, skip = 4)
        var dummy: Any? = null
        StructField(index = 1)
        var numLODVertexes = IntArray(StudioModel.MAX_NUM_LODS)
    }

    class MStudioMesh {

        StructField(index = 0)
        var material: Int = 0
        StructField(index = 1)
        var modelindex: Int = 0
        StructField(index = 2)
        var numvertices: Int = 0
        StructField(index = 2)
        var vertexoffset: Int = 0
        StructField(index = 3)
        var numflexes: Int = 0
        StructField(index = 3)
        var flexindex: Int = 0
        StructField(index = 4)
        var materialtype: Int = 0
        StructField(index = 5)
        var materialparam: Int = 0
        StructField(index = 6)
        var meshid: Int = 0
        StructField(index = 7)
        var center: Vector3f? = null
        StructField(index = 8)
        var vertexdata = MStudioMeshVertexData()
        StructField(index = 9, skip = 32)
        var dummy: Any? = null
    }

    inner class MStudioBodyParts {

        StructField(index = 0)
        var sznameindex: Int = 0
        StructField(index = 0)
        var nummodels: Int = 0
        StructField(index = 0)
        var base: Int = 0
        StructField(index = 0)
        var modelindex: Int = 0
        var models: MutableList<MStudioModel>? = null
        var offset = position()
    }

    inner class MStudioModel {

        StructField(index = 0, limit = 64)
        var name: String? = null
        StructField(index = 1)
        var type: Int = 0
        StructField(index = 2)
        var boundingradius: Float = 0.toFloat()
        StructField(index = 3)
        var nummeshes: Int = 0
        StructField(index = 3)
        var meshindex: Int = 0
        StructField(index = 4)
        var numvertices: Int = 0
        StructField(index = 4)
        var vertexindex: Int = 0
        StructField(index = 4)
        var tangentsindex: Int = 0
        StructField(index = 5)
        var numattachments: Int = 0
        StructField(index = 5)
        var attachmentindex: Int = 0
        StructField(index = 6)
        var numeyeballs: Int = 0
        StructField(index = 6)
        var eyeballindex: Int = 0
        StructField(index = 7, skip = 40)
        var dummy: Any? = null
        var meshes: MutableList<MStudioMesh>? = null
        var vertexoffset: Int = 0
        var offset = position()
    }

    class object {

        private val LOG = Logger.getLogger(javaClass<MDL>().getName())

        throws(javaClass<IOException>())
        public fun load(file: File): MDL? {
            LOG.log(Level.INFO, "Loading MDL {0}", file)
            return load(ByteBufferInputStream(DataUtils.mapFile(file)))
        }

        throws(javaClass<IOException>())
        public fun load(`in`: InputStream): MDL? {
            try {
                return MDL(BufferedInputStream(`in`))
            } catch (ex: InstantiationException) {
                LOG.log(Level.SEVERE, null, ex)
            } catch (ex: IllegalAccessException) {
                LOG.log(Level.SEVERE, null, ex)
            }

            return null
        }
    }
}
