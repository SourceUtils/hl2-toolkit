package com.timepath.hl2.io;

import com.timepath.DataUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * https://github.com/toji/webgl-source/tree/master/js
 * https://github.com/toji/webgl-source/blob/master/js/source-mdl-struct.js
 * https://github.com/toji/webgl-source/blob/master/js/source-mdl.js
 * http://trac.openscenegraph.org/projects/osg/browser/OpenSceneGraph/trunk/src/osgPlugins/mdl
 * https://github.com/w23/OpenSource/tree/master/src
 *
 * @author TimePath
 */
public class StudioModel {

    private static final Logger LOG = Logger.getLogger(StudioModel.class.getName());

    private static final int MAX_NUM_LODS = 8;

    private static final int MAX_NUM_BONES_PER_VERT = 3;

    private static final int MAX_NUM_BONES_PER_TRI = 9;

    private static final int VERTEX_STRIDE = 64;

    public static StudioModel load(String fileName) throws IOException {
        LOG.log(Level.INFO, "\n\nLoading StudioModel {0}", fileName);
        StudioModel s = new StudioModel();
        
        File mdl = new File(fileName + ".mdl");
        if(mdl.exists()) {
            s.mdl = MDL.load(mdl);
        }
        File vvd = new File(fileName + ".vvd");
        if(vvd.exists()) {
            s.vvd = VVD.load(vvd);
        }
        File vtx = new File(fileName + ".dx90.vtx");
        if(vtx.exists()) {
            s.vtx = VTX.load(vtx);
        }
        
        return s;
    }

    private MDL mdl;
    
    private VVD vvd;

    private VTX vtx;

    public float[] getVertices() {
        return vvd.verts;
    }

    public float[] getNormals() {
        return vvd.norm;
    }

    public float[] getTangents() {
        return vvd.tangents;
    }

    public float[] getTextureCoordinates() {
        return vvd.uv;
    }

    public int[] getIndices() {
        if(vtx == null) {
            return null;
        }
        int[] b = new int[vtx.indices.size()];
        for(int i = 0; i < vtx.indices.size(); i++) {
            Integer l = vtx.indices.get(i);
            b[i] = l.intValue();
        }
        return b;
    }

    private static class MDL {
        
        private static final Logger LOG = Logger.getLogger(MDL.class.getName());

        private static MDL load(File file) throws IOException {
            LOG.log(Level.INFO, "\n\nLoading MDL {0}", file);
            return load(DataUtils.mapFile(file));
        }

        private static MDL load(ByteBuffer buf) {
            MDL mdl = new MDL();
            
            

            return mdl;
        }

        private MDL() {
        }

    }

    private static class VTX {

        private static final Logger LOG = Logger.getLogger(VTX.class.getName());

        private static VTX load(File file) throws IOException {
            LOG.log(Level.INFO, "\n\nLoading VTX {0}", file);
            return load(DataUtils.mapFile(file));
        }

        private static VTX load(ByteBuffer buf) {
            VTX vtx = new VTX();
            //<editor-fold defaultstate="collapsed" desc="VTX Header">
            // file version as defined by OPTIMIZED_MODEL_FILE_VERSION (currently 7)
            int version = buf.getInt(); // 0
            // hardware params that affect how the model is to be optimized.
            int vertCacheSize = buf.getInt(); // 4
            long maxBonesPerStrip = buf.getShort(); // 8
            long maxBonesPerTri = buf.getShort(); // 10
            int maxBonesPerVert = buf.getInt(); // 12
            // must match checkSum in the .mdl
            long checkSum = buf.getInt(); // 16
            int numLODs = buf.getInt(); // 20 // garymcthack - this is also specified in ModelHeader_t and should match
            // this is an offset to an array of 8 MaterialReplacementListHeader_t's, one of these for each LOD
            int materialReplacementListOffset = buf.getInt(); // 24
            int numBodyParts = buf.getInt(); // 28
            int bodyPartOffset = buf.getInt(); // 32 // offset to an array of BodyPartHeader_t's
            //</editor-fold>
            LOG.log(Level.INFO,
                    "\t\t\tver:{0}, vertCache:{1}, bones/strip:{2}, bones/tri:{3}, bones/vert:{4}, check:{5}, lods:{6}, replOff:{7}, parts:{8}, partOff:{9}",
                    new Object[] {version, vertCacheSize, maxBonesPerStrip, maxBonesPerTri,
                                  maxBonesPerVert, checkSum, numLODs, materialReplacementListOffset,
                                  numBodyParts, bodyPartOffset});

            int totalIndices = 0;

            LOG.log(Level.INFO, "parts[] = {2}: {0} vs {1}", new Object[] {buf.position(),
                                                                           bodyPartOffset,
                                                                           numBodyParts});
            buf.position(bodyPartOffset);
            for(int part = 0; part < numBodyParts; part++) {
                int part_numModels = buf.getInt();
                int part_modelOffset = bodyPartOffset + buf.getInt();

                LOG.log(Level.INFO, "parts[{3}].models[] = {2}: {0} vs {1}", new Object[] {
                    buf.position(), part_modelOffset, part_numModels, part});
                buf.position(part_modelOffset);
                for(int model = 0; model < part_numModels; model++) {
                    int mdl_numLODs = buf.getInt();
                    int mdl_lodOffset = part_modelOffset + buf.getInt();

                    LOG.log(Level.INFO, "parts[{4}].models[{3}].lods[] = {2}: {0} vs {1}",
                            new Object[] {buf.position(), mdl_lodOffset, mdl_numLODs, model, part});
                    buf.position(mdl_lodOffset);
                    mdl_numLODs = 1; // XXX: Temporarily load one LOD
                    for(int lod = 0; lod < mdl_numLODs; lod++) {
                        int lod_numMeshes = buf.getInt();
                        int lod_meshOffset = mdl_lodOffset + buf.getInt();
                        float lod_switchPoint = buf.getFloat();

                        LOG.log(Level.INFO,
                                "parts[{5}].model[{4}].lod[{3}].meshes[] = {2}: {0} vs {1}",
                                new Object[] {buf.position(), lod_meshOffset, lod_numMeshes, lod,
                                              model, part});
                        buf.position(lod_meshOffset);
                        for(int mesh = 0; mesh < lod_numMeshes; mesh++) {
                            int mesh_numStripGroups = buf.getInt();
                            int mesh_stripGroupHeaderOffset = lod_meshOffset + buf.getInt();
                            short mesh_flags = buf.get();

                            LOG.log(Level.INFO,
                                    "parts[{6}].model[{5}].lod[{4}].meshes[{3}].stripGroups[] = {2}: {0} vs {1}",
                                    new Object[] {buf.position(), mesh_stripGroupHeaderOffset,
                                                  mesh_numStripGroups, mesh, lod, model, part});
                            buf.position(mesh_stripGroupHeaderOffset);
                            for(int group = 0; group < mesh_numStripGroups; group++) {
                                int group_numVerts = buf.getInt();
                                int group_vertOffset = mesh_stripGroupHeaderOffset + buf.getInt();
                                int group_numIndices = buf.getInt();
                                int group_indexOffset = mesh_stripGroupHeaderOffset + buf.getInt();
                                int group_numStrips = buf.getInt();
                                int group_stripOffset = mesh_stripGroupHeaderOffset + buf.getInt();
                                short group_flags = buf.get();

                                totalIndices += group_numIndices;

                                LOG.log(Level.INFO,
                                        "\t\t\tstripOff: {2}, vertOff: {0}, indOff: {1},",
                                        new Object[] {group_vertOffset, group_indexOffset,
                                                      group_stripOffset});

                                LOG.log(Level.INFO,
                                        "parts[{7}].model[{6}].lod[{5}].meshes[{4}].stripGroups[{3}].strips[] = {2}: {0} vs {1}",
                                        new Object[] {buf.position(), group_stripOffset,
                                                      group_numStrips, group, mesh, lod, model, part});
                                buf.position(group_stripOffset);
                                for(int strip = 0; strip < group_numStrips; strip++) {
                                    int strip_numIndices = buf.getInt();
                                    int strip_indexOffset = buf.getInt();
                                    int strip_numVerts = buf.getInt();
                                    int strip_vertOffset = buf.getInt();
                                    short strip_numBones = buf.getShort();
                                    short strip_flags = buf.get();
                                    int strip_numBoneStateChanges = buf.getInt();
                                    int strip_boneStateChangeOffset = buf.getInt();
//                                    buf.position(strip_boneStateChangeOffset);
//                                    for(int bone = 0; bone < strip_numBoneStateChanges; bone++) {
//                                        int bone_hardwareID = buf.getInt();
//                                        int bone_newBoneID = buf.getInt();
//                                    }
                                }

                                LOG.log(Level.INFO,
                                        "parts[{7}].model[{6}].lod[{5}].meshes[{4}].stripGroups[{3}].verts[] = {2}: {0} vs {1}",
                                        new Object[] {buf.position(), group_vertOffset,
                                                      group_numVerts, group, mesh, lod, model, part});
                                buf.position(group_vertOffset);
                                int[] vert_origMeshVertIDs = new int[group_numVerts];
                                for(int vert = 0; vert < group_numVerts; vert++) {
                                    short[] vert_boneWeightIndex = new short[MAX_NUM_BONES_PER_VERT];
                                    for(int bw = 0; bw < MAX_NUM_BONES_PER_VERT; bw++) {
                                        vert_boneWeightIndex[bw] = buf.get();
                                    }
                                    short vert_numBones = buf.get();
                                    vert_origMeshVertIDs[vert] = buf.getShort();
                                    short[] vert_boneID = new short[MAX_NUM_BONES_PER_VERT];
                                    for(int bi = 0; bi < MAX_NUM_BONES_PER_VERT; bi++) {
                                        vert_boneID[bi] = buf.get();
                                    }
                                }

                                LOG.log(Level.INFO,
                                        "parts[{7}].model[{6}].lod[{5}].meshes[{4}].stripGroups[{3}].indices[] = {2}: {0} vs {1}",
                                        new Object[] {buf.position(), group_indexOffset,
                                                      group_numIndices, group, mesh, lod, model,
                                                      part});
                                buf.position(group_indexOffset);
                                int[] indices = new int[group_numIndices];
                                for(int index = 0; index < group_numIndices; index++) {
                                    indices[index] = buf.getShort();
                                    vtx.indices.add(vert_origMeshVertIDs[indices[index]]);// + model_vertexoffset + mesh_vertexOffset);
                                }
                            }
                        }
                    }
                }
            }

            vtx.indices.ensureCapacity(totalIndices);

            LOG.log(Level.INFO, "Underflow: {0}", new Object[] {buf.remaining()});

            return vtx;
        }

        private ArrayList<Integer> indices = new ArrayList<Integer>();

        private VTX() {
        }

    }

    private static class VVD {

        private static final Logger LOG = Logger.getLogger(VVD.class.getName());

        public static VVD load(File file) throws IOException {
            LOG.log(Level.INFO, "\n\nLoading VVD {0}", file);
            MappedByteBuffer mbb = new FileInputStream(file).getChannel().map(
                    FileChannel.MapMode.READ_ONLY, 0, file.length());
            mbb.order(ByteOrder.LITTLE_ENDIAN);
            return load(mbb);
        }

        public static VVD load(ByteBuffer buf) {
            VVD vvd = new VVD();
            //<editor-fold defaultstate="collapsed" desc="Header">
            int id = buf.getInt();                        // MODEL_VERTEX_FILE_ID
            int version = buf.getInt();                   // MODEL_VERTEX_FILE_VERSION
            long checksum = buf.getInt();                 // same as studiohdr_t, ensures sync
            int numLODs = buf.getInt();                   // num of valid lods
            int[] numLODVertexes = new int[MAX_NUM_LODS]; // num verts for desired root lod
            for(int l = 0; l < numLODVertexes.length; l++) {
                numLODVertexes[l] = buf.getInt();
            }
            int numFixups = buf.getInt();              // num of vertexFileFixup_t
            int fixupTableStart = buf.getInt();        // offset from base to fixup table
            int vertexDataStart = buf.getInt();        // offset from base to vertex block
            int tangentDataStart = buf.getInt();       // offset from base to tangent block
            //</editor-fold>

            int vertexCount = (tangentDataStart - vertexDataStart) / 48;
            vvd.verts = new float[vertexCount * 3];
            vvd.norm = new float[vertexCount * 3];
            vvd.tangents = new float[vertexCount * 4];
            vvd.uv = new float[vertexCount * 2];

            LOG.log(Level.INFO, "{0},{1},{2},{3},{4},{5},{6},{7}", new Object[] {id, version,
                                                                                     checksum,
                                                                                     Arrays.toString(
                                                                                             numLODVertexes), numFixups, fixupTableStart, vertexDataStart, tangentDataStart});
            
            LOG.log(Level.INFO, "{0} vs {1}", new Object[] {buf.position(), fixupTableStart});

            // Fixup Table
            buf.position(fixupTableStart);
            for(int f = 0; f < numFixups; f++) {
                int lod = buf.getInt();            // used to skip culled root lod
                int sourceVertexID = buf.getInt(); // absolute index from start of vertex/tangent blocks
                int numVertexes = buf.getInt();
            }

            LOG.log(Level.INFO, "{0} vs {1}", new Object[] {buf.position(), vertexDataStart});

            // Vertex Data
            buf.position(vertexDataStart);
            for(int v = 0; v < vertexCount; v++) {
                float[] weight = new float[MAX_NUM_BONES_PER_VERT];
                for(int w = 0; w < weight.length; w++) {
                    weight[w] = buf.getFloat();
                }
                byte[] bone = new byte[MAX_NUM_BONES_PER_VERT];
                for(int b = 0; b < bone.length; b++) {
                    bone[b] = buf.get();
                }
                byte numbones = buf.get();

                float[] m_vecPosition = {buf.getFloat(), buf.getFloat(), buf.getFloat()};
                System.arraycopy(m_vecPosition, 0, vvd.verts, v * 3, m_vecPosition.length);
                float[] m_vecNormal = {buf.getFloat(), buf.getFloat(), buf.getFloat()};
                System.arraycopy(m_vecNormal, 0, vvd.norm, v * 3, m_vecNormal.length);
                float[] m_vecTexCoord = {buf.getFloat(), buf.getFloat()};
                System.arraycopy(m_vecTexCoord, 0, vvd.uv, v * 2, m_vecTexCoord.length);
            }

            LOG.log(Level.INFO, "{0} vs {1}", new Object[] {buf.position(), tangentDataStart});

            // Tangent Data
            buf.position(tangentDataStart);
            for(int t = 0; t < vertexCount; t++) {
                float[] m_vecTangent = {buf.getFloat(), buf.getFloat(), buf.getFloat(),
                                                                        buf.getFloat()};
                System.arraycopy(m_vecTangent, 0, vvd.tangents, t * 4, m_vecTangent.length);
            }

            LOG.log(Level.INFO, "Underflow: {0}", new Object[] {buf.remaining()});

            return vvd;
        }

        public float[] verts, norm, uv, tangents;

        private VVD() {
        }

    }

}
