package com.timepath.hl2.io.bsp.lump;

import com.timepath.io.struct.StructField;
import java.util.logging.Logger;

public class Face {

    private static final Logger LOG = Logger.getLogger(Face.class.getName());

    private static final int MAXLIGHTMAPS = 4;

    /**
     * The plane number. unsigned
     */
    @StructField(index = 0) public short planenum;

    /**
     * faces opposite to the node's plane direction
     */
    @StructField(index = 1) public byte side;

    /**
     * 1 of on node, 0 if in leaf
     */
    @StructField(index = 2) public byte onNode;

    /**
     * index into surfedges
     */
    @StructField(index = 3) public int firstedge;

    /**
     * number of surfedges
     */
    @StructField(index = 4) public short numedges;

    /**
     * texture info
     */
    @StructField(index = 5) public short texinfo;

    /**
     * displacement info
     */
    @StructField(index = 6) public short dispinfo;

    /**
     * ?
     */
    @StructField(index = 7) public short surfaceFogVolumeID;

    /**
     * switchable lighting info
     */
    @StructField(index = 8) public byte[] styles = new byte[MAXLIGHTMAPS];

    /**
     * offset into lightmap lump
     */
    @StructField(index = 9) public int lightofs;

    /**
     * face area in units^2
     */
    @StructField(index = 10) public float area;

    /**
     * texture lighting info
     */
    @StructField(index = 11) public int[] m_LightmapTextureMinsInLuxels = new int[2];

    /**
     * texture lighting info
     */
    @StructField(index = 12) public int[] m_LightmapTextureSizeInLuxels = new int[2];

    /**
     * original face this was split from
     */
    @StructField(index = 13) public int origFace;

    /**
     * primitives. unsigned
     */
    @StructField(index = 14) public short m_NumPrims;

    /**
     * unsigned
     */
    @StructField(index = 15) public short firstPrimID;

    /**
     * lightmap smoothing group. unsigned
     */
    @StructField(index = 16) public int smoothingGroups;

}
