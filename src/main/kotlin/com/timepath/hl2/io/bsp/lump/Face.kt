package com.timepath.hl2.io.bsp.lump

import com.timepath.io.struct.StructField

public class Face {
    /**
     * switchable lighting info
     */
    StructField(index = 8)
    public var styles: ByteArray = ByteArray(MAXLIGHTMAPS)
    /**
     * The plane number. unsigned
     */
    StructField(index = 0)
    public var planenum: Short = 0
    /**
     * faces opposite to the node's plane direction
     */
    StructField(index = 1)
    public var side: Byte = 0
    /**
     * 1 of on node, 0 if in leaf
     */
    StructField(index = 2)
    public var onNode: Byte = 0
    /**
     * index into surfedges
     */
    StructField(index = 3)
    public var firstedge: Int = 0
    /**
     * number of surfedges
     */
    StructField(index = 4)
    public var numedges: Short = 0
    /**
     * texture info
     */
    StructField(index = 5)
    public var texinfo: Short = 0
    /**
     * displacement info
     */
    StructField(index = 6)
    public var dispinfo: Short = 0
    /**
     * ?
     */
    StructField(index = 7)
    public var surfaceFogVolumeID: Short = 0
    /**
     * offset into lightmap lump
     */
    StructField(index = 9)
    public var lightofs: Int = 0
    /**
     * face area in units^2
     */
    StructField(index = 10)
    public var area: Float = 0f
    /**
     * texture lighting info
     */
    StructField(index = 11)
    public var m_LightmapTextureMinsInLuxels: IntArray = IntArray(2)
    /**
     * texture lighting info
     */
    StructField(index = 12)
    public var m_LightmapTextureSizeInLuxels: IntArray = IntArray(2)
    /**
     * original face this was split from
     */
    StructField(index = 13)
    public var origFace: Int = 0
    /**
     * primitives. unsigned
     */
    StructField(index = 14)
    public var m_NumPrims: Short = 0
    /**
     * unsigned
     */
    StructField(index = 15)
    public var firstPrimID: Short = 0
    /**
     * lightmap smoothing group. unsigned
     */
    StructField(index = 16)
    public var smoothingGroups: Int = 0

    companion object {

        private val MAXLIGHTMAPS = 4
    }
}
