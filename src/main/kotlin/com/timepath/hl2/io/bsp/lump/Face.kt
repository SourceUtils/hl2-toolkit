package com.timepath.hl2.io.bsp.lump

import com.timepath.hl2.io.bsp.Lump
import com.timepath.hl2.io.bsp.LumpHandler
import com.timepath.io.OrderedInputStream
import com.timepath.io.struct.Struct
import com.timepath.io.struct.StructField

private val MAXLIGHTMAPS = 4
private val MAX_MAP_FACES = 65536

public class Face(
        /** The plane number. unsigned */
        @StructField(0) val planenum: Short = 0,
        /** faces opposite to the node's plane direction */
        @StructField(1) val side: Byte = 0,
        /** 1 of on node, 0 if in leaf */
        StructField(2) val onNode: Byte = 0,
        /** index into surfedges */
        StructField(3) val firstedge: Int = 0,
        /** number of surfedges */
        StructField(4) val numedges: Short = 0,
        /** texture info */
        StructField(5) val texinfo: Short = 0,
        /** displacement info */
        StructField(6) val dispinfo: Short = 0,
        /** ? */
        StructField(7) val surfaceFogVolumeID: Short = 0,
        /** switchable lighting info */
        StructField(8) val styles: ByteArray = ByteArray(MAXLIGHTMAPS),
        /** offset into lightmap lump */
        StructField(9) val lightofs: Int = 0,
        /** face area in units^2 */
        StructField(10) val area: Float = 0f,
        /** texture lighting info */
        StructField(11) val m_LightmapTextureMinsInLuxels: IntArray = IntArray(2),
        /** texture lighting info */
        StructField(12) val m_LightmapTextureSizeInLuxels: IntArray = IntArray(2),
        /** original face this was split from */
        StructField(13) val origFace: Int = 0,
        /** primitives. unsigned */
        StructField(14) val m_NumPrims: Short = 0,
        /** unsigned */
        StructField(15) val firstPrimID: Short = 0,
        /** lightmap smoothing group. unsigned */
        StructField(16) val smoothingGroups: Int = 0
) {
    class Handler : LumpHandler<List<Face>> {
        override fun invoke(l: Lump, ois: OrderedInputStream): List<Face> {
            return (0..(l.length / Struct.sizeof(Face())) - 1).map {
                ois.readStruct<Face>(Face())
            }
        }
    }
}
