package com.timepath.hl2.io;

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
 * Studiomodel
 *
 * @author timepath
 */
public class MDL {

    private static final Logger LOG = Logger.getLogger(MDL.class.getName());
    private VVD vvd;

    public float[] getverts() {
        return vvd.verts;
    }

    public float[] getnorm() {
        return vvd.norm;
    }

    public float[] gettangents() {
        return vvd.tangents;
    }

    public float[] getuv() {
        return vvd.uv;
    }
    private VTX vtx;

    public static MDL load(String fileName) throws IOException {
        LOG.info(fileName);
        MDL mdl = MDL.load(new File(fileName + ".mdl"));
        mdl.vvd = VVD.load(new File(fileName + ".vvd"));
        mdl.vtx = VTX.load(new File(fileName + ".sw.vtx"));
        return mdl;
    }

    private static MDL load(File file) throws IOException {
        MappedByteBuffer mbb = new FileInputStream(file).getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
        mbb.order(ByteOrder.LITTLE_ENDIAN);
        return load(mbb);
    }

    private static MDL load(ByteBuffer buf) {
        MDL mdl = new MDL();
        return mdl;
    }

    /**
     * http://blog.tojicode.com/2011/09/source-engine-in-webgl-tech-talk.html
     *
     * "the indicies don't point directly at a vertex offset, but instead give
     * an index into a "vertex table", which itself contains the actual index.
     * Even that number, however, is not a true index since you also have to
     * manually calculate another offset into the vertex array based on the
     * number of vertices in all prior meshes."
     *
     * @author timepath
     */
    private static class VTX {

        private static final Logger LOG = Logger.getLogger(VTX.class.getName());

        private VTX() {
        }

        private static VTX load(File file) throws IOException {
            MappedByteBuffer mbb = new FileInputStream(file).getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            mbb.order(ByteOrder.LITTLE_ENDIAN);
            return load(mbb);
        }

        private static VTX load(ByteBuffer buf) {
            VTX vtx = new VTX();
            // file version as defined by OPTIMIZED_MODEL_FILE_VERSION (currently 7)
            int version = buf.getInt(); // 0

            // hardware params that affect how the model is to be optimized.
            int vertCacheSize = buf.getInt(); // 4
            long maxBonesPerStrip = buf.getShort(); // 8
            long maxBonesPerFace = buf.getShort(); // 10
            int maxBonesPerVert = buf.getInt(); // 12

            // must match checkSum in the .mdl
            long checkSum = buf.getLong(); // 16

            int lodCount = buf.getInt(); // 24 // garymcthack - this is also specified in ModelHeader_t and should match

            // this is an offset to an array of 8 MaterialReplacementListHeader_t's, one of these for each LOD
            int materialReplacementListOffset = buf.getInt(); // 28

            int numBodyParts = buf.getInt(); // 32
            int bodyPartOffset = buf.getInt(); // 36 // offset to an array of BodyPartHeader_t's

            // 40
            LOG.log(Level.INFO, "{0},{1},{2},{3},{4},{5},{6},{7},{8},{9}", new Object[] {version, vertCacheSize, maxBonesPerStrip, maxBonesPerFace, maxBonesPerVert, checkSum, lodCount, materialReplacementListOffset, numBodyParts, bodyPartOffset});

            long vertCount = buf.getInt(); // 40
            long vertTableOffset = buf.getInt(); // 44
            long vertFaceCount = buf.getInt(); // 48
            long vertFaceTableOffset = buf.getInt(); // 52
            long meshCount = buf.getInt(); // 56
            long meshTableOffset = 52 + buf.getInt(); // 60
            byte unknown = buf.get(); // 64
            // 65

            long pos1 = meshTableOffset;
            for(int l = 0; l < lodCount; l++) {
                ArrayList<Long> smesh = new ArrayList<Long>();
                for(int n = 0; n < meshCount; n++) {
                    smesh.add(pos1);
                    buf.position((int) pos1);
                    smesh.add((long) buf.getInt());
                    pos1 += 4;
                    buf.position((int) pos1);
                    smesh.add((long) buf.getInt());
                    pos1 += 5;
                }
                long pos2 = 0;
                ArrayList<Long> faces = new ArrayList<Long>();
                for(int m = 0; m < meshCount * 3; m += 3) {
                    long offset = smesh.get(m) + smesh.get(m + 2);
                    long parts = smesh.get(m + 1);
                    for(int y = 0; y < parts; y++) {
                        ArrayList<Long> vert_data = new ArrayList<Long>();
                        pos2 = offset;
                        faces.add(parts);
                        buf.position((int) pos2);
                        long vert_num = buf.getInt();
                        faces.add(vert_num);
                        pos2 += 4;
                        buf.position((int) pos2);
                        long vert_pos = offset + buf.getInt();
                        long vert_l = vert_pos + (vert_num * 9);
                        while(vert_l > vert_pos) {//[byte] ID of vert from VVD binary.
                            vert_pos += 4;
                            buf.position((int) vert_pos);
                            vert_data.add((long) buf.getShort());
                            vert_pos += 5;
                        }
                        pos2 += +4;
                        buf.position((int) pos2);
                        long tri_num = buf.getInt();
                        pos2 += +4;
                        buf.position((int) pos2);
                        long tri_pos = offset + buf.getInt();
                        long tri_l = tri_num * 2;
//                        long vert_seq = buf.sub($tri_pos, $tri_l); //[byte] Vert sequence to draw faces in correct order.
//                        foreach($vert_seq as $vid) //Arrange vert IDs in correct order to be outputted to SMD.
//                        {
//                                array_push($faces, $vert_data[$vid]);
//                        }
//                        m_data.add(faces);
//                        faces = array();
                        offset += 25;
                    }
                }
            }

            LOG.log(Level.INFO, "Underflow: {0}", new Object[] {buf.remaining()});

            return vtx;
        }
    }

    /**
     * Valve Studio Model Vertex Data File
     *
     * @author timepath
     */
    private static class VVD {

        private static final int MAX_NUM_LODS = 8;
        private static final int MAX_NUM_BONES_PER_VERT = 3;
        public float[] verts, norm, uv, tangents;
        private static final Logger LOG = Logger.getLogger(VVD.class.getName());

        private VVD() {
        }

        public static VVD load(File file) throws IOException {
            MappedByteBuffer mbb = new FileInputStream(file).getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            mbb.order(ByteOrder.LITTLE_ENDIAN);
            return load(mbb);
        }

        public static VVD load(ByteBuffer buf) {
            VVD vvd = new VVD();
            // Header
            int id = buf.getInt();                        // MODEL_VERTEX_FILE_ID
            int version = buf.getInt();                   // MODEL_VERTEX_FILE_VERSION
            long checksum = buf.getLong();                // same as studiohdr_t, ensures sync
//            int numLODs = buf.getInt();                     // num of valid lods
            int[] numLODVertexes = new int[MAX_NUM_LODS]; // num verts for desired root lod
            for(int l = 0; l < numLODVertexes.length; l++) {
                numLODVertexes[l] = buf.getInt();
            }
            int numFixups = buf.getInt();              // num of vertexFileFixup_t
            int fixupTableStart = buf.getInt();        // offset from base to fixup table
            int vertexDataStart = buf.getInt();        // offset from base to vertex block
            int tangentDataStart = buf.getInt();       // offset from base to tangent block

            int vertexCount = (tangentDataStart - vertexDataStart) / 48;
            vvd.verts = new float[vertexCount * 3];
            vvd.norm = new float[vertexCount * 3];
            vvd.tangents = new float[vertexCount * 4];
            vvd.uv = new float[vertexCount * 2];

            LOG.log(Level.INFO, "{0},{1},{2},{3},{4},{5},{6},{7}", new Object[] {id, version, checksum, Arrays.toString(numLODVertexes), numFixups, fixupTableStart, vertexDataStart, tangentDataStart});

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
                float[] m_vecTangent = {buf.getFloat(), buf.getFloat(), buf.getFloat(), buf.getFloat()};
                System.arraycopy(m_vecTangent, 0, vvd.tangents, t * 4, m_vecTangent.length);
            }

            LOG.log(Level.INFO, "Underflow: {0}", new Object[] {buf.remaining()});

            return vvd;
        }
    }
}
