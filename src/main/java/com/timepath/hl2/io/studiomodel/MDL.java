package com.timepath.hl2.io.studiomodel;

import com.timepath.DataUtils;
import com.timepath.hl2.io.util.Vector3f;
import com.timepath.io.ByteBufferInputStream;
import com.timepath.io.OrderedInputStream;
import com.timepath.io.struct.Struct;
import com.timepath.io.struct.StructField;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.timepath.hl2.io.studiomodel.StudioModel.MAX_NUM_LODS;

public class MDL {

    private static final Logger LOG = Logger.getLogger(MDL.class.getName());
    public final StudioHeader header;
    final List<MStudioBodyParts> mdlBodyParts;
    private final List<MStudioTextureDir> textureDirs;
    private final List<MStudioTexture> textures;
    private final OrderedInputStream is;
    private Level verbosity = Level.FINE;

    private MDL(InputStream in) throws IOException, InstantiationException, IllegalAccessException {
        is = new OrderedInputStream(in);
        is.mark(Integer.MAX_VALUE);
        is.order(ByteOrder.LITTLE_ENDIAN);
        header = is.readStruct(new StudioHeader());
        LOG.log(verbosity, "StudioHeader header = \"{0}\";", header.toString());
        LOG.log(verbosity, "MStudioTexture[] textures = new MStudioTexture[{0}];", header.numtextures);
        position(header.textureindex);
        textures = new ArrayList<>(header.numtextures);
        for (int i = 0; i < header.numtextures; i++) {
            int offset = is.position();
            MStudioTexture tex = is.readStruct(new MStudioTexture());
            textures.add(tex);
            position(offset + tex.sznameindex);
            tex.textureName = is.readString();
            position(offset + Struct.sizeof(tex));
            LOG.log(verbosity, "textures[{0}] = \"{1}\";", new Object[]{i, tex.textureName});
        }
        LOG.log(verbosity, "MStudioTextureDir[] textureDirs = new MStudioTextureDir[{0}];", header.numcdtextures);
        position(header.cdtextureindex);
        textureDirs = new ArrayList<>(header.numcdtextures);
        for (int i = 0; i < header.numcdtextures; i++) {
            int offset = is.position();
            MStudioTextureDir texDir = is.readStruct(new MStudioTextureDir());
            textureDirs.add(texDir);
            position(texDir.diroffset);
            texDir.textureDir = is.readString();
            position(offset + Struct.sizeof(texDir));
            LOG.log(verbosity, "textureDirs[{0}] = \"{1}\";", new Object[]{i, texDir.textureDir});
        }
        LOG.log(verbosity, "int[] skinTable = new int[{0}];", header.numskinref * header.numskinfamilies);
        position(header.skinindex);
        int[] skinTable = new int[header.numskinref * header.numskinfamilies];
        for (int i = 0; i < skinTable.length; i++) {
            skinTable[i] = is.readShort();
            LOG.log(verbosity, "skinTable[{0}] = {1};", new Object[]{i, skinTable[i]});
        }
        LOG.log(verbosity, "MStudioBodyParts[]");
        position(header.bodypartindex);
        mdlBodyParts = new ArrayList<>(header.numbodyparts);
        for (int i = 0; i < header.numbodyparts; i++) {
            MStudioBodyParts bodyPart = is.readStruct(new MStudioBodyParts());
            mdlBodyParts.add(bodyPart);
            LOG.log(verbosity, "MStudioBodyParts[{0}/{1}].models[]", new Object[]{
                    1 + i, header.numbodyparts
            });
            position(bodyPart.offset + bodyPart.modelindex);
            bodyPart.models = new ArrayList<>(bodyPart.nummodels);
            for (int j = 0; j < bodyPart.nummodels; j++) {
                MStudioModel model = is.readStruct(new MStudioModel());
                bodyPart.models.add(model);
                LOG.log(verbosity, "MStudioBodyParts[{0}/{1}].models[{2}/{3}].meshes[]", new Object[]{
                        1 + i, header.numbodyparts, 1 + j, bodyPart.nummodels
                });
                position(model.offset + model.meshindex);
                model.meshes = new ArrayList<>(model.nummeshes);
                for (int k = 0; k < model.nummeshes; k++) {
                    LOG.log(verbosity, "MStudioBodyParts[{0}/{1}].models[{2}/{3}].meshes[{4}/{5}]", new Object[]{
                            1 + i, header.numbodyparts, 1 + j, bodyPart.nummodels, 1 + k, model.nummeshes
                    });
                    MStudioMesh mesh = is.readStruct(new MStudioMesh());
                    model.meshes.add(mesh);
                }
                position(model.offset + Struct.sizeof(model));
            }
            position(bodyPart.offset + Struct.sizeof(bodyPart));
        }
    }

    public static MDL load(File file) throws IOException {
        LOG.log(Level.INFO, "Loading MDL {0}", file);
        return load(new ByteBufferInputStream(DataUtils.mapFile(file)));
    }

    public static MDL load(InputStream in) throws IOException {
        try {
            return new MDL(new BufferedInputStream(in));
        } catch (InstantiationException | IllegalAccessException ex) {
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
        } catch (IOException ex) {
            return -1;
        }
    }

    public static class StudioHeader {

        @StructField(index = 3, limit = 64)
        public String name;
        @StructField(index = 0)
        int id;
        @StructField(index = 1)
        int version;
        @StructField(index = 2)
        int checksum;
        @StructField(index = 4)
        int length;
        @StructField(index = 5)
        Vector3f eyePosition;
        @StructField(index = 6)
        Vector3f illumPosition;
        @StructField(index = 7)
        Vector3f hullMin, hullMax;
        @StructField(index = 8)
        Vector3f view_bbmin, view_bbmax;
        @StructField(index = 9)
        int flags;
        @StructField(index = 10)
        int numBones, boneIndex;
        @StructField(index = 11)
        int numBoneControllers, boneControllerIndex;
        @StructField(index = 12)
        int numhitboxsets, hitboxsetindex;
        @StructField(index = 13)
        int numlocalanim, localanimindex;
        @StructField(index = 14)
        int numlocalseq, localseqindex;
        @StructField(index = 15)
        int activitylistversion;
        @StructField(index = 16)
        int eventsindexed;
        @StructField(index = 17)
        int numtextures, textureindex;
        @StructField(index = 18)
        int numcdtextures, cdtextureindex;
        @StructField(index = 19)
        int numskinref, numskinfamilies, skinindex;
        @StructField(index = 20)
        int numbodyparts, bodypartindex;
        @StructField(index = 21)
        int numlocalattachments, localattachmentindex;
        @StructField(index = 22)
        int numlocalnodes, localnodeindex;
        @StructField(index = 23)
        int localnodenameindex;
        @StructField(index = 24)
        int numflexdesc, flexdescindex;
        @StructField(index = 25)
        int numflexcontrollers, flexcontrollerindex;
        @StructField(index = 26)
        int numflexrules, flexruleindex;
        @StructField(index = 27)
        int numikchains, ikchainindex;
        @StructField(index = 28)
        int nummouths, mouthindex;
        @StructField(index = 29)
        int numlocalposeparameters, localposeparamindex;
        @StructField(index = 30)
        int surfacepropindex;
        @StructField(index = 31)
        int keyvalueindex, keyvaluesize;
        @StructField(index = 32)
        int numlocalikautoplaylocks, localikautoplaylockindex;
        @StructField(index = 33)
        float mass;
        @StructField(index = 34)
        int contents;
        @StructField(index = 35)
        int numincludemodels, includemodelindex;
        @StructField(index = 36)
        int virtualModel; // skip
        @StructField(index = 37)
        int szanimblocknameindex;
        @StructField(index = 38)
        int numanimblocks, animblockindex;
        @StructField(index = 39)
        int animblockModel; // skip
        @StructField(index = 40)
        int bonetablebynameindex;
        @StructField(index = 41)
        int pVertexBase, pIndexBase; // skip
        @StructField(index = 42)
        byte constdirectionallightdot;
        @StructField(index = 43)
        byte rootLOD, numAllowedRootLODs;
        @StructField(index = 44, skip = 5)
        Object dummy1;
        @StructField(index = 45)
        int numflexcontrollerui, flexcontrolleruiindex;
        @StructField(index = 46, skip = 8)
        Object dummy2;
        @StructField(index = 47)
        int studiohdr2index;
        @StructField(index = 48, skip = 4)
        Object dummy3;

        public StudioHeader() {
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static class MStudioTexture {

        @StructField(index = 0)
        int sznameindex, flags, used;
        @StructField(index = 1, skip = 52)
        Object dummy;
        String textureName;

        MStudioTexture() {
        }
    }

    static class MStudioTextureDir {

        @StructField(index = 0)
        int diroffset;
        String textureDir;

        MStudioTextureDir() {
        }
    }

    static class MStudioMeshVertexData {

        @StructField(index = 0, skip = 4)
        Object dummy;
        @StructField(index = 1)
        int[] numLODVertexes = new int[MAX_NUM_LODS];

        MStudioMeshVertexData() {
        }
    }

    static class MStudioMesh {

        @StructField(index = 0)
        int material;
        @StructField(index = 1)
        int modelindex;
        @StructField(index = 2)
        int numvertices, vertexoffset;
        @StructField(index = 3)
        int numflexes, flexindex;
        @StructField(index = 4)
        int materialtype;
        @StructField(index = 5)
        int materialparam;
        @StructField(index = 6)
        int meshid;
        @StructField(index = 7)
        Vector3f center;
        @StructField(index = 8)
        MStudioMeshVertexData vertexdata = new MStudioMeshVertexData();
        @StructField(index = 9, skip = 32)
        Object dummy;

        MStudioMesh() {
        }
    }

    class MStudioBodyParts {

        @StructField(index = 0)
        int sznameindex, nummodels, base, modelindex;
        List<MStudioModel> models;
        int offset = position();

        MStudioBodyParts() {
        }
    }

    class MStudioModel {

        @StructField(index = 0, limit = 64)
        String name;
        @StructField(index = 1)
        int type;
        @StructField(index = 2)
        float boundingradius;
        @StructField(index = 3)
        int nummeshes, meshindex;
        @StructField(index = 4)
        int numvertices, vertexindex, tangentsindex;
        @StructField(index = 5)
        int numattachments, attachmentindex;
        @StructField(index = 6)
        int numeyeballs, eyeballindex;
        @StructField(index = 7, skip = 40)
        Object dummy;
        List<MStudioMesh> meshes;
        int vertexoffset;
        int offset = position();

        MStudioModel() {
        }
    }
}
