package com.timepath.hl2.io.studiomodel

import com.timepath.DataUtils
import com.timepath.io.ByteBufferInputStream
import com.timepath.io.OrderedInputStream
import com.timepath.io.struct.Struct
import com.timepath.io.struct.StructField

import java.io.BufferedInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.text.MessageFormat
import java.util.ArrayList
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.properties.Delegates

class VTX [throws(javaClass<IOException>(), javaClass<InstantiationException>(), javaClass<IllegalAccessException>())]
private(`in`: InputStream) {
    val bodyParts: MutableList<BodyPart>
    private val header: VtxHeader
    private val `is`: OrderedInputStream

    {
        `is` = OrderedInputStream(`in`)
        `is`.mark(Integer.MAX_VALUE)
        `is`.order(ByteOrder.LITTLE_ENDIAN)
        header = `is`.readStruct<VtxHeader>(VtxHeader())
        LOG.log(verbosity, header.toString())
        // Parts
        bodyParts = ArrayList<BodyPart>(header.numBodyParts)
        //        LOG.log(verbosity,
        //                "\t\t\tparts[] = {2}: {0} vs {1}",
        //                new Object[] {is.position(), offset, header.numBodyParts});
        position(header.bodyPartOffset)
        for (partIdx in header.numBodyParts.indices) {
            val part = `is`.readStruct<BodyPart>(BodyPart())
            bodyParts.add(part)
            // Models
            part.models = ArrayList<Model>(part.numModels)
            //            LOG.log(verbosity,
            //                    "\t\t\tparts[{3}].models[] = {2}: {0} vs {1}",
            //                    new Object[] {is.position(),  offset + part.modelOffset, part.numModels, partIdx});
            position(part.offset + part.modelOffset)
            for (modelIdx in part.numModels.indices) {
                val model = `is`.readStruct<Model>(Model())
                part.models.add(model)
                // LODs
                model.lods = ArrayList<ModelLOD>(model.numLODs)
                //                LOG.log(verbosity,
                //                        "\t\t\tparts[{3}].models[{4}].lods[] = {2}: {0} vs {1}",
                //                        new Object[] {is.position(), offset + model.lodOffset, model.numLODs,
                //                                      partIdx, modelIdx});
                position(model.offset + model.lodOffset)
                for (lodIdx in model.numLODs.indices) {
                    val lod = `is`.readStruct<ModelLOD>(ModelLOD())
                    model.lods.add(lod)
                    // Meshes
                    lod.meshes = ArrayList<Mesh>(lod.numMeshes)
                    //                    LOG.log(verbosity,
                    //                            "\t\t\tparts[{3}].model[{4}].lod[{5}].meshes[] = {2}: {0} vs {1}",
                    //                            new Object[] {is.position(), offset + lod.meshOffset, lod.numMeshes,
                    //                                          partIdx, modelIdx, lodIdx});
                    position(lod.offset + lod.meshOffset)
                    for (meshIdx in lod.numMeshes.indices) {
                        val mesh = `is`.readStruct<Mesh>(Mesh())
                        lod.meshes.add(mesh)
                        // Strip groups
                        mesh.stripGroups = ArrayList<StripGroup>(mesh.numStripGroups)
                        //                        LOG.log(verbosity,
                        //                                "\t\t\tparts[{3}].model[{4}].lod[{5}].meshes[{6}].stripGroups[] =
                        // {2}: {0} vs {1}",
                        //                                new Object[] {is.position(), offset + mesh.stripGroupHeaderOffset,
                        // mesh.numStripGroups,
                        //                                              partIdx, modelIdx, lodIdx, meshIdx});
                        position(mesh.offset + mesh.stripGroupHeaderOffset)
                        for (groupIdx in mesh.numStripGroups.indices) {
                            val stripGroup = `is`.readStruct<StripGroup>(StripGroup())
                            mesh.stripGroups.add(stripGroup)
                            LOG.log(verbosity, "\t\t\tOffset:{0} stripOff: {1}, vertOff: {2}, indOff: {3},", array<Any>(stripGroup.offset, stripGroup.offset + stripGroup.vertOffset, stripGroup.offset + stripGroup.stripOffset, stripGroup.offset + stripGroup.indexOffset))
                            // Strips
                            stripGroup.strips = ArrayList<Strip>(stripGroup.numStrips)
                            //                            LOG.log(verbosity,
                            //                                    "\t\t\tparts[{3}].model[{4}].lod[{5}].meshes[{6}]
                            // .stripGroups[{7}].strips[] = {2}: {0} vs {1}",
                            //                                    new Object[] {is.position(), offset + stripGroup.stripOffset,
                            // stripGroup.numStrips,
                            //                                                  partIdx, modelIdx, lodIdx, meshIdx, groupIdx});
                            position(stripGroup.offset + stripGroup.stripOffset)
                            for (stripIdx in stripGroup.numStrips.indices) {
                                val strip = `is`.readStruct<Strip>(Strip())
                                stripGroup.strips.add(strip)
                            }
                            // Verts
                            stripGroup.verts = ArrayList<Vertex>(stripGroup.numVerts)
                            //                            LOG.log(verbosity,
                            //                                    "\t\t\tparts[{3}].model[{4}].lod[{5}].meshes[{6}]
                            // .stripGroups[{7}].verts[] = {2}: {0} vs {1}",
                            //                                    new Object[] {is.position(), offset + stripGroup.vertOffset,
                            // stripGroup.numVerts,
                            //                                                  partIdx, modelIdx, lodIdx, meshIdx, groupIdx});
                            position(stripGroup.offset + stripGroup.vertOffset)
                            for (vertIdx in stripGroup.numVerts.indices) {
                                val vert = `is`.readStruct<Vertex>(Vertex())
                                stripGroup.verts.add(vert)
                            }
                            // Indices
                            val indicesBuf = ByteArray(stripGroup.numIndices * 2)
                            //                            LOG.log(verbosity,
                            //                                    "\t\t\tparts[{3}].model[{4}].lod[{5}].meshes[{6}]
                            // .stripGroups[{7}].indices[] = {2}: {0} vs {1}",
                            //                                    new Object[] {is.position(), offset + stripGroup.indexOffset,
                            // stripGroup.numIndices,
                            //                                                  partIdx, modelIdx, lodIdx, meshIdx, groupIdx});
                            position(stripGroup.offset + stripGroup.indexOffset)
                            `is`.readFully(indicesBuf)
                            stripGroup.indexBuffer = ByteBuffer.allocateDirect(indicesBuf.size).put(indicesBuf)
                            stripGroup.indexBuffer.flip()
                            position(stripGroup.offset + Struct.sizeof(stripGroup))
                        }
                        position(mesh.offset + Struct.sizeof(mesh))
                    }
                    position(lod.offset + Struct.sizeof(lod))
                }
                position(model.offset + Struct.sizeof(model))
            }
            position(part.offset + Struct.sizeof(part))
        }
        LOG.log(verbosity, "Underflow: {0}", array<Any>(`is`.available()))
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
        } catch (ignored: IOException) {
            return -1
        }

    }

    private class Strip () {

        StructField(index = 0)
        var numIndices: Int = 0
        StructField(index = 1)
        var indexOffset: Int = 0
        StructField(index = 2)
        var numVerts: Int = 0
        StructField(index = 3)
        var vertOffset: Int = 0
        StructField(index = 4)
        var numBones: Short = 0
        StructField(index = 5)
        var flags: Byte = 0
        StructField(index = 6)
        var numBoneStateChanges: Int = 0
        StructField(index = 7)
        var boneStateChangeOffset: Int = 0
    }

    class Vertex {

        StructField(index = 0)
        var boneWeightIndex = ByteArray(StudioModel.MAX_NUM_BONES_PER_VERT)
        StructField(index = 1)
        var numBones: Byte = 0
        StructField(index = 2)
        var origMeshVertID: Short = 0
        StructField(index = 3)
        var boneID = ByteArray(StudioModel.MAX_NUM_BONES_PER_VERT)
    }

    class VtxHeader {

        /**
         * File version as defined by OPTIMIZED_MODEL_FILE_VERSION (currently 7)
         */
        StructField(index = 0)
        var version: Int = 0
        /**
         * Hardware parameters that affect how the model is to be optimized
         */
        StructField(index = 1)
        var vertCacheSize: Int = 0
        StructField(index = 2)
        var maxBonesPerStrip: Short = 0
        StructField(index = 3)
        var maxBonesPerTri: Short = 0
        StructField(index = 4)
        var maxBonesPerVert: Int = 0
        /**
         * Must match checkSum in the MDL
         */
        StructField(index = 5)
        var checkSum: Int = 0
        /**
         * This is also specified in ModelHeader and should match
         */
        StructField(index = 6)
        var numLODs: Int = 0
        /**
         * This is an offset to an array of 8 MaterialReplacementListHeaders, one of these for each LOD
         */
        StructField(index = 7)
        var materialReplacementListOffset: Int = 0
        StructField(index = 8)
        var numBodyParts: Int = 0
        /**
         * Offset to an array of BodyPartHeaders
         */
        StructField(index = 9)
        var bodyPartOffset: Int = 0

        override fun toString(): String {
            return MessageFormat.format("\t\t\tver:{0}, vertCache:{1}, bones/strip:{2}, bones/tri:{3}, bones/vert:{4}, check:{5}, lods:{6}, replOff:{7}, parts:{8}, partOff:{9}", version, vertCacheSize, maxBonesPerStrip, maxBonesPerTri, maxBonesPerVert, checkSum, numLODs, materialReplacementListOffset, numBodyParts, bodyPartOffset)
        }
    }

    inner class BodyPart {

        var offset = position()
        var models: MutableList<Model> by Delegates.notNull()
        StructField(index = 0)
        var numModels: Int = 0
        StructField(index = 1)
        var modelOffset: Int = 0
    }

    inner class Mesh {

        var offset = position()
        var stripGroups: MutableList<StripGroup> by Delegates.notNull()
        StructField(index = 0)
        var numStripGroups: Int = 0
        StructField(index = 1)
        var stripGroupHeaderOffset: Int = 0
        StructField(index = 2)
        var flags: Byte = 0
    }

    inner class Model {

        var offset = position()
        var lods: MutableList<ModelLOD> by Delegates.notNull()
        StructField(index = 0)
        var numLODs: Int = 0
        StructField(index = 1)
        var lodOffset: Int = 0
    }

    inner class ModelLOD {

        var offset = position()
        var meshes: MutableList<Mesh> by Delegates.notNull()
        StructField(index = 0)
        var numMeshes: Int = 0
        StructField(index = 1)
        var meshOffset: Int = 0
        StructField(index = 2)
        var switchPoint: Float = 0f
    }

    inner class StripGroup {

        var offset = position()
        var indexBuffer: ByteBuffer by Delegates.notNull()
        var strips: MutableList<Strip> by Delegates.notNull()
        var verts: MutableList<Vertex> by Delegates.notNull()
        StructField(index = 0)
        var numVerts: Int = 0
        StructField(index = 1)
        var vertOffset: Int = 0
        StructField(index = 2)
        var numIndices: Int = 0
        StructField(index = 3)
        var indexOffset: Int = 0
        StructField(index = 4)
        var numStrips: Int = 0
        StructField(index = 5)
        var stripOffset: Int = 0
        StructField(index = 6)
        var flags: Byte = 0
    }

    class object {

        private val LOG = Logger.getLogger(javaClass<VTX>().getName())
        private val verbosity = Level.FINE

        throws(javaClass<IOException>())
        public fun load(file: File): VTX? {
            LOG.log(Level.INFO, "Loading VVD {0}", file)
            return load(ByteBufferInputStream(DataUtils.mapFile(file)))
        }

        throws(javaClass<IOException>())
        public fun load(`in`: InputStream): VTX? {
            try {
                return VTX(BufferedInputStream(`in`))
            } catch (ex: InstantiationException) {
                LOG.log(Level.SEVERE, null, ex)
            } catch (ex: IllegalAccessException) {
                LOG.log(Level.SEVERE, null, ex)
            }

            return null
        }
    }
}
