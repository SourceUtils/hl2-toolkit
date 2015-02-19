package com.timepath.hl2.io.bsp.lump

import com.timepath.hl2.io.bsp.Lump
import com.timepath.hl2.io.bsp.LumpHandler
import com.timepath.io.OrderedInputStream

import java.io.IOException
import java.text.MessageFormat
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author TimePath
 */
SuppressWarnings("rawtypes")
public enum class LumpType(public val ID: Int, public val handler: LumpHandler<*>? = null) {
    /**
     * Map entities
     */
    LUMP_ENTITIES : LumpType(0, EntitiesHandler())
    /**
     * Plane array
     */
    LUMP_PLANES : LumpType(1, PlaneHandler())
    /**
     * Index to texture names
     */
    LUMP_TEXDATA : LumpType(2)
    /**
     * Vertex array
     */
    LUMP_VERTEXES : LumpType(3, VertexHander())
    /**
     * Compressed visibility bit arrays
     */
    LUMP_VISIBILITY : LumpType(4)
    /**
     * BSP tree nodes
     */
    LUMP_NODES : LumpType(5)
    /**
     * Face texture array
     */
    LUMP_TEXINFO : LumpType(6)
    /**
     * Face array
     */
    LUMP_FACES : LumpType(7, FaceHandler())
    /**
     * Lightmap samples
     */
    LUMP_LIGHTING : LumpType(8)
    /**
     * Occlusion polygons and vertices
     */
    LUMP_OCCLUSION : LumpType(9)
    /**
     * BSP tree leaf nodes
     */
    LUMP_LEAFS : LumpType(10)
    /**
     * Correlates between dfaces and Hammer face IDs. Also used as random seed for detail prop placement.
     */
    LUMP_FACEIDS : LumpType(11)
    /**
     * Edge array
     */
    LUMP_EDGES : LumpType(12, EdgeHandler())
    /**
     * Index of edges
     */
    LUMP_SURFEDGES : LumpType(13, SurfaceEdgeHandler())
    /**
     * Brush models (geometry of brush entities)
     */
    LUMP_MODELS : LumpType(14)
    /**
     * Internal world lights converted from the entity lump
     */
    LUMP_WORLDLIGHTS : LumpType(15)
    /**
     * Index to faces in each leaf
     */
    LUMP_LEAFFACES : LumpType(16)
    /**
     * Index to brushes in each leaf
     */
    LUMP_LEAFBRUSHES : LumpType(17)
    /**
     * Brush array
     */
    LUMP_BRUSHES : LumpType(18)
    /**
     * Brushside array
     */
    LUMP_BRUSHSIDES : LumpType(19)
    /**
     * Area array
     */
    LUMP_AREAS : LumpType(20)
    /**
     * Portals between areas
     */
    LUMP_AREAPORTALS : LumpType(21)
    /**
     *
     */
    LUMP_UNUSED0 : LumpType(22)
    /**
     *
     */
    LUMP_UNUSED1 : LumpType(23)
    /**
     *
     */
    LUMP_UNUSED2 : LumpType(24)
    /**
     *
     */
    LUMP_UNUSED3 : LumpType(25)
    /**
     * Displacement surface array
     */
    LUMP_DISPINFO : LumpType(26)
    /**
     * Brush faces array before splitting
     */
    LUMP_ORIGINALFACES : LumpType(27)
    /**
     * Displacement physics collision data
     */
    LUMP_PHYSDISP : LumpType(28)
    /**
     * Physics collision data
     */
    LUMP_PHYSCOLLIDE : LumpType(29)
    /**
     * Face plane normals
     */
    LUMP_VERTNORMALS : LumpType(30)
    /**
     * Face plane normal index array
     */
    LUMP_VERTNORMALINDICES : LumpType(31)
    /**
     * Displacement lightmap alphas (unused/empty since Source 2006)
     */
    LUMP_DISP_LIGHTMAP_ALPHAS : LumpType(32)
    /**
     * Vertices of displacement surface meshes
     */
    LUMP_DISP_VERTS : LumpType(33)
    /**
     * Displacement lightmap sample positions
     */
    LUMP_DISP_LIGHTMAP_SAMPLE_POSITIONS : LumpType(34)
    /**
     * Game-specific data lump
     */
    LUMP_GAME_LUMP : LumpType(35)
    /**
     * Data for leaf nodes that are inside water
     */
    LUMP_LEAFWATERDATA : LumpType(36)
    /**
     * Water polygon data
     */
    LUMP_PRIMITIVES : LumpType(37)
    /**
     * Water polygon vertices
     */
    LUMP_PRIMVERTS : LumpType(38)
    /**
     * Water polygon vertex index array
     */
    LUMP_PRIMINDICES : LumpType(39)
    /**
     * Embedded uncompressed Zip-format file
     */
    LUMP_PAKFILE : LumpType(40, PakfileHandler())
    /**
     * Clipped portal polygon vertices
     */
    LUMP_CLIPPORTALVERTS : LumpType(41)
    /**
     * env_cubemap location array
     */
    LUMP_CUBEMAPS : LumpType(42)
    /**
     * Texture name data
     */
    LUMP_TEXDATA_STRING_DATA : LumpType(43)
    /**
     * Index array into texdata string data
     */
    LUMP_TEXDATA_STRING_TABLE : LumpType(44)
    /**
     * info_overlay data array
     */
    LUMP_OVERLAYS : LumpType(45)
    /**
     * Distance from leaves to water
     */
    LUMP_LEAFMINDISTTOWATER : LumpType(46)
    /**
     * Macro texture info for faces
     */
    LUMP_FACE_MACRO_TEXTURE_INFO : LumpType(47)
    /**
     * Displacement surface triangles
     */
    LUMP_DISP_TRIS : LumpType(48)
    /**
     * Compressed win32-specific Havok terrain surface collision data. Deprecated and no longer used.
     */
    LUMP_PHYSCOLLIDESURFACE : LumpType(49)
    /**
     * info_overlay's on water faces?
     */
    LUMP_WATEROVERLAYS : LumpType(50)
    /**
     * Index of LUMP_LEAF_AMBIENT_LIGHTING_HDR
     */
    LUMP_LEAF_AMBIENT_INDEX_HDR : LumpType(51)
    /**
     * Index of LUMP_LEAF_AMBIENT_LIGHTING
     */
    LUMP_LEAF_AMBIENT_INDEX : LumpType(52)
    /**
     * HDR lightmap samples
     */
    LUMP_LIGHTING_HDR : LumpType(53)
    /**
     * Internal HDR world lights converted from the entity lump
     */
    LUMP_WORLDLIGHTS_HDR : LumpType(54)
    /**
     * HDR related leaf lighting data?
     */
    LUMP_LEAF_AMBIENT_LIGHTING_HDR : LumpType(55)
    /**
     * HDR related leaf lighting data?
     */
    LUMP_LEAF_AMBIENT_LIGHTING : LumpType(56)
    /**
     * XZip version of pak file for Xbox. Deprecated.
     */
    LUMP_XZIPPAKFILE : LumpType(57)
    /**
     * HDR maps may have different face data
     */
    LUMP_FACES_HDR : LumpType(58)
    /**
     * Extended level-wide flags. Not present in all levels.
     */
    LUMP_MAP_FLAGS : LumpType(59)
    /**
     * Fade distances for overlays
     */
    LUMP_OVERLAY_FADES : LumpType(60)
    /**
     * System level settings (min/max CPU & GPU to render this overlay)
     */
    LUMP_OVERLAY_SYSTEM_LEVELS : LumpType(61)
    /**
     *
     */
    LUMP_PHYSLEVEL : LumpType(62)
    /**
     * Displacement multiblend info
     */
    LUMP_DISP_MULTIBLEND : LumpType(63)

    private val LOG = Logger.getLogger(javaClass<LumpType>().getName())

    override fun toString(): String {
        return MessageFormat.format("{0} ({1})", name(), ID)
    }

//    SuppressWarnings("unchecked")
//    throws(javaClass<IOException>())
    public fun <T> handle(l: Lump, `in`: OrderedInputStream): T? {
        if (handler == null) {
            LOG.log(Level.WARNING, "No handler for {0}", this)
            return null
        }
        return handler!!.handle(l, `in`) as T
    }
}
