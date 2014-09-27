package com.timepath.hl2.io.studiomodel;

import com.timepath.DataUtils;
import com.timepath.io.ByteBufferInputStream;
import com.timepath.io.OrderedInputStream;
import com.timepath.io.struct.Struct;
import com.timepath.io.struct.StructField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.timepath.hl2.io.studiomodel.StudioModel.MAX_NUM_BONES_PER_VERT;

class VTX {

    private static final Logger LOG = Logger.getLogger(VTX.class.getName());
    private static final Level verbosity = Level.FINE;
    @NotNull
    final List<BodyPart> bodyParts;
    @NotNull
    private final VtxHeader header;
    @NotNull
    private final OrderedInputStream is;

    private VTX(@NotNull InputStream in) throws IOException, InstantiationException, IllegalAccessException {
        is = new OrderedInputStream(in);
        is.mark(Integer.MAX_VALUE);
        is.order(ByteOrder.LITTLE_ENDIAN);
        header = is.readStruct(new VtxHeader());
        LOG.log(verbosity, header.toString());
        // Parts
        bodyParts = new ArrayList<>(header.numBodyParts);
        //        LOG.log(verbosity,
        //                "\t\t\tparts[] = {2}: {0} vs {1}",
        //                new Object[] {is.position(), offset, header.numBodyParts});
        position(header.bodyPartOffset);
        for (int partIdx = 0; partIdx < header.numBodyParts; partIdx++) {
            @NotNull BodyPart part = is.readStruct(new BodyPart());
            bodyParts.add(part);
            // Models
            part.models = new ArrayList<>(part.numModels);
            //            LOG.log(verbosity,
            //                    "\t\t\tparts[{3}].models[] = {2}: {0} vs {1}",
            //                    new Object[] {is.position(),  offset + part.modelOffset, part.numModels, partIdx});
            position(part.offset + part.modelOffset);
            for (int modelIdx = 0; modelIdx < part.numModels; modelIdx++) {
                @NotNull Model model = is.readStruct(new Model());
                part.models.add(model);
                // LODs
                model.lods = new ArrayList<>(model.numLODs);
                //                LOG.log(verbosity,
                //                        "\t\t\tparts[{3}].models[{4}].lods[] = {2}: {0} vs {1}",
                //                        new Object[] {is.position(), offset + model.lodOffset, model.numLODs,
                //                                      partIdx, modelIdx});
                position(model.offset + model.lodOffset);
                for (int lodIdx = 0; lodIdx < model.numLODs; lodIdx++) {
                    @NotNull ModelLOD lod = is.readStruct(new ModelLOD());
                    model.lods.add(lod);
                    // Meshes
                    lod.meshes = new ArrayList<>(lod.numMeshes);
                    //                    LOG.log(verbosity,
                    //                            "\t\t\tparts[{3}].model[{4}].lod[{5}].meshes[] = {2}: {0} vs {1}",
                    //                            new Object[] {is.position(), offset + lod.meshOffset, lod.numMeshes,
                    //                                          partIdx, modelIdx, lodIdx});
                    position(lod.offset + lod.meshOffset);
                    for (int meshIdx = 0; meshIdx < lod.numMeshes; meshIdx++) {
                        @NotNull Mesh mesh = is.readStruct(new Mesh());
                        lod.meshes.add(mesh);
                        // Strip groups
                        mesh.stripGroups = new ArrayList<>(mesh.numStripGroups);
                        //                        LOG.log(verbosity,
                        //                                "\t\t\tparts[{3}].model[{4}].lod[{5}].meshes[{6}].stripGroups[] =
                        // {2}: {0} vs {1}",
                        //                                new Object[] {is.position(), offset + mesh.stripGroupHeaderOffset,
                        // mesh.numStripGroups,
                        //                                              partIdx, modelIdx, lodIdx, meshIdx});
                        position(mesh.offset + mesh.stripGroupHeaderOffset);
                        for (int groupIdx = 0; groupIdx < mesh.numStripGroups; groupIdx++) {
                            @NotNull StripGroup stripGroup = is.readStruct(new StripGroup());
                            mesh.stripGroups.add(stripGroup);
                            LOG.log(verbosity, "\t\t\tOffset:{0} stripOff: {1}, vertOff: {2}, indOff: {3},", new Object[]{
                                            stripGroup.offset,
                                            stripGroup.offset + stripGroup.vertOffset,
                                            stripGroup.offset + stripGroup.stripOffset,
                                            stripGroup.offset + stripGroup.indexOffset
                                    }
                            );
                            // Strips
                            stripGroup.strips = new ArrayList<>(stripGroup.numStrips);
                            //                            LOG.log(verbosity,
                            //                                    "\t\t\tparts[{3}].model[{4}].lod[{5}].meshes[{6}]
                            // .stripGroups[{7}].strips[] = {2}: {0} vs {1}",
                            //                                    new Object[] {is.position(), offset + stripGroup.stripOffset,
                            // stripGroup.numStrips,
                            //                                                  partIdx, modelIdx, lodIdx, meshIdx, groupIdx});
                            position(stripGroup.offset + stripGroup.stripOffset);
                            for (int stripIdx = 0; stripIdx < stripGroup.numStrips; stripIdx++) {
                                @NotNull Strip strip = is.readStruct(new Strip());
                                stripGroup.strips.add(strip);
                            }
                            // Verts
                            stripGroup.verts = new ArrayList<>(stripGroup.numVerts);
                            //                            LOG.log(verbosity,
                            //                                    "\t\t\tparts[{3}].model[{4}].lod[{5}].meshes[{6}]
                            // .stripGroups[{7}].verts[] = {2}: {0} vs {1}",
                            //                                    new Object[] {is.position(), offset + stripGroup.vertOffset,
                            // stripGroup.numVerts,
                            //                                                  partIdx, modelIdx, lodIdx, meshIdx, groupIdx});
                            position(stripGroup.offset + stripGroup.vertOffset);
                            for (int vertIdx = 0; vertIdx < stripGroup.numVerts; vertIdx++) {
                                @NotNull Vertex vert = is.readStruct(new Vertex());
                                stripGroup.verts.add(vert);
                            }
                            // Indices
                            @NotNull byte[] indicesBuf = new byte[stripGroup.numIndices * 2];
                            //                            LOG.log(verbosity,
                            //                                    "\t\t\tparts[{3}].model[{4}].lod[{5}].meshes[{6}]
                            // .stripGroups[{7}].indices[] = {2}: {0} vs {1}",
                            //                                    new Object[] {is.position(), offset + stripGroup.indexOffset,
                            // stripGroup.numIndices,
                            //                                                  partIdx, modelIdx, lodIdx, meshIdx, groupIdx});
                            position(stripGroup.offset + stripGroup.indexOffset);
                            is.readFully(indicesBuf);
                            stripGroup.indexBuffer = ByteBuffer.allocateDirect(indicesBuf.length).put(indicesBuf);
                            stripGroup.indexBuffer.flip();
                            position(stripGroup.offset + Struct.sizeof(stripGroup));
                        }
                        position(mesh.offset + Struct.sizeof(mesh));
                    }
                    position(lod.offset + Struct.sizeof(lod));
                }
                position(model.offset + Struct.sizeof(model));
            }
            position(part.offset + Struct.sizeof(part));
        }
        LOG.log(verbosity, "Underflow: {0}", new Object[]{is.available()});
    }

    @Nullable
    public static VTX load(@NotNull File file) throws IOException {
        LOG.log(Level.INFO, "Loading VVD {0}", file);
        return load(new ByteBufferInputStream(DataUtils.mapFile(file)));
    }

    @Nullable
    public static VTX load(@NotNull InputStream in) throws IOException {
        try {
            return new VTX(new BufferedInputStream(in));
        } catch (@NotNull InstantiationException | IllegalAccessException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private void position(int index) {
        //        LOG.log(verbosity, "seeking to {0}", index);
        try {
            is.reset();
            is.skipBytes(index - is.position());
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    private int position() {
        try {
            return is.position();
        } catch (IOException ignored) {
            return -1;
        }
    }

    private static class Strip {

        @StructField(index = 0)
        int numIndices;
        @StructField(index = 1)
        int indexOffset;
        @StructField(index = 2)
        int numVerts;
        @StructField(index = 3)
        int vertOffset;
        @StructField(index = 4)
        short numBones;
        @StructField(index = 5)
        byte flags;
        @StructField(index = 6)
        int numBoneStateChanges;
        @StructField(index = 7)
        int boneStateChangeOffset;

        private Strip() {
        }
    }

    static class Vertex {

        @NotNull
        @StructField(index = 0)
        byte[] boneWeightIndex = new byte[MAX_NUM_BONES_PER_VERT];
        @StructField(index = 1)
        byte numBones;
        @StructField(index = 2)
        short origMeshVertID;
        @NotNull
        @StructField(index = 3)
        byte[] boneID = new byte[MAX_NUM_BONES_PER_VERT];

        Vertex() {
        }
    }

    static class VtxHeader {

        /**
         * File version as defined by OPTIMIZED_MODEL_FILE_VERSION (currently 7)
         */
        @StructField(index = 0)
        int version;
        /**
         * Hardware parameters that affect how the model is to be optimized
         */
        @StructField(index = 1)
        int vertCacheSize;
        @StructField(index = 2)
        short maxBonesPerStrip;
        @StructField(index = 3)
        short maxBonesPerTri;
        @StructField(index = 4)
        int maxBonesPerVert;
        /**
         * Must match checkSum in the MDL
         */
        @StructField(index = 5)
        int checkSum;
        /**
         * This is also specified in ModelHeader and should match
         */
        @StructField(index = 6)
        int numLODs;
        /**
         * This is an offset to an array of 8 MaterialReplacementListHeaders, one of these for each LOD
         */
        @StructField(index = 7)
        int materialReplacementListOffset;
        @StructField(index = 8)
        int numBodyParts;
        /**
         * Offset to an array of BodyPartHeaders
         */
        @StructField(index = 9)
        int bodyPartOffset;

        VtxHeader() {
        }

        @NotNull
        @Override
        public String toString() {
            return MessageFormat.format(
                    "\t\t\tver:{0}, vertCache:{1}, bones/strip:{2}, bones/tri:{3}, bones/vert:{4}, check:{5}, lods:{6}, " +
                            "replOff:{7}, parts:{8}, partOff:{9}",
                    version,
                    vertCacheSize,
                    maxBonesPerStrip,
                    maxBonesPerTri,
                    maxBonesPerVert,
                    checkSum,
                    numLODs,
                    materialReplacementListOffset,
                    numBodyParts,
                    bodyPartOffset
            );
        }
    }

    class BodyPart {

        int offset = position();
        List<Model> models;
        @StructField(index = 0)
        int numModels;
        @StructField(index = 1)
        int modelOffset;

        BodyPart() {
        }
    }

    class Mesh {

        int offset = position();
        List<StripGroup> stripGroups;
        @StructField(index = 0)
        int numStripGroups;
        @StructField(index = 1)
        int stripGroupHeaderOffset;
        @StructField(index = 2)
        byte flags;

        Mesh() {
        }
    }

    class Model {

        int offset = position();
        List<ModelLOD> lods;
        @StructField(index = 0)
        int numLODs;
        @StructField(index = 1)
        int lodOffset;

        Model() {
        }
    }

    class ModelLOD {

        int offset = position();
        List<Mesh> meshes;
        @StructField(index = 0)
        int numMeshes;
        @StructField(index = 1)
        int meshOffset;
        @StructField(index = 2)
        float switchPoint;

        ModelLOD() {
        }
    }

    class StripGroup {

        int offset = position();
        ByteBuffer indexBuffer;
        List<Strip> strips;
        List<Vertex> verts;
        @StructField(index = 0)
        int numVerts;
        @StructField(index = 1)
        int vertOffset;
        @StructField(index = 2)
        int numIndices;
        @StructField(index = 3)
        int indexOffset;
        @StructField(index = 4)
        int numStrips;
        @StructField(index = 5)
        int stripOffset;
        @StructField(index = 6)
        byte flags;

        StripGroup() {
        }
    }
}
