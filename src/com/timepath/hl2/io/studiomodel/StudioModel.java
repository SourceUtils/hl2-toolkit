package com.timepath.hl2.io.studiomodel;

import java.io.*;
import java.nio.*;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StudioModel {

    private static final Logger LOG = Logger.getLogger(StudioModel.class.getName());

    static final int MAX_NUM_BONES_PER_VERT = 3;

    static final int MAX_NUM_LODS = 8;

    public final MDL mdl;

    public final VTX vtx;

    public final VVD vvd;

    private final ByteBuffer indexBuffer;

    public StudioModel(InputStream mdlStream, InputStream vvdStream, InputStream vtxStream) throws IOException {
        mdl = MDL.load(mdlStream);
        vvd = VVD.load(vvdStream);
        vtx = VTX.load(vtxStream);
        int lod = 0;
        setRootLOD(lod);
        indexBuffer = buildIndices(lod);
    }

    public IntBuffer getIndices() {
        return indexBuffer.asIntBuffer();
    }

    public FloatBuffer getNormals() {
        return vvd.normals.asFloatBuffer();
    }

    public FloatBuffer getTangents() {
        return vvd.tangents.asFloatBuffer();
    }

    public FloatBuffer getTextureCoordinates() {
        return vvd.uv;
    }

    public FloatBuffer getVertices() {
        return vvd.verts.asFloatBuffer();
    }

    private ByteBuffer buildIndices(int lodId) {
        ByteArrayOutputStream indices = new ByteArrayOutputStream();
        int indexOffset = 0;
        int vertTableIndex;

        for(int i = 0; i < vtx.bodyParts.size(); i++) {
            VTX.BodyPart bodyPart = vtx.bodyParts.get(i);
            MDL.MStudioBodyParts mdlBodyPart = mdl.mdlBodyParts.get(i);

            if(bodyPart.models.isEmpty()) {
                continue;
            }
            VTX.Model model = bodyPart.models.get(0);
            MDL.MStudioModel mdlModel = mdlBodyPart.models.get(0);

            VTX.ModelLOD lod = model.lods.get(lodId);

            for(int j = 0; j < lod.meshes.size(); j++) {
                VTX.Mesh mesh = lod.meshes.get(j);
                MDL.MStudioMesh mdlMesh = mdlModel.meshes.get(j);

                for(VTX.StripGroup stripGroup : mesh.stripGroups) {
                    List<VTX.Vertex> vertTable = stripGroup.verts;
                    stripGroup.indexOffset = indexOffset++;
                    ShortBuffer sb = stripGroup.indexBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
                    for(int l = 0; l < stripGroup.numIndices; l++) {
                        vertTableIndex = sb.get();
                        int index = vertTable.get(vertTableIndex).origMeshVertID
                                        + mdlModel.vertexoffset + mdlMesh.vertexoffset;
                        short s = (short) index;
                        try {
                            indices.write(new byte[] {
                                (byte) (s & 0xFF),
                                (byte) ((s & 0xFF00) >> 8),
                                (byte) ((s & 0xFF0000) >> 16),
                                (byte) ((s & 0xFF000000) >> 24),});
                        } catch(IOException ex) {
                            LOG.log(Level.SEVERE, null, ex);
                            return null;
                        }
                    }
                }
            }
        }
        byte[] bytes = indices.toByteArray();
        ByteBuffer buf = ByteBuffer.allocateDirect(bytes.length);
        buf.put(bytes).flip();
        return buf;
    }

    private void setRootLOD(int rootLOD) {
        MDL.StudioHeader header = mdl.header;
        List<MDL.MStudioBodyParts> bodyParts = mdl.mdlBodyParts;

        if(header.numAllowedRootLODs > 0 && rootLOD >= header.numAllowedRootLODs) {
            rootLOD = header.numAllowedRootLODs - 1;
        }

        int vertexoffset = 0;

        for(MDL.MStudioBodyParts bodyPart : bodyParts) {
            for(MDL.MStudioModel model : bodyPart.models) {
                int totalMeshVertices = 0;
                for(int meshId = 0; meshId < model.meshes.size(); ++meshId) {
                    MDL.MStudioMesh mesh = model.meshes.get(meshId);

                    mesh.numvertices = mesh.vertexdata.numLODVertexes[rootLOD];
                    mesh.vertexoffset = totalMeshVertices;
                    totalMeshVertices += mesh.numvertices;
                }

                model.numvertices = totalMeshVertices;
                model.vertexoffset = vertexoffset;
                vertexoffset += totalMeshVertices;
            }
        }
    }

}
