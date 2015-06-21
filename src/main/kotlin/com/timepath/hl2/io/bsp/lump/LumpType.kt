package com.timepath.hl2.io.bsp.lump

import com.timepath.Logger
import com.timepath.hl2.io.bsp.Lump
import com.timepath.hl2.io.bsp.LumpHandler
import com.timepath.io.OrderedInputStream
import java.text.MessageFormat

public enum class LumpType(public val ID: Int, public val handler: LumpHandler<*>? = null) {
    /**
     * Map entities
     */
    LUMP_ENTITIES(0, EntitiesHandler()),
    /**
     * Plane array
     */
    LUMP_PLANES(1, PlaneHandler()),
    /**
     * Index to texture names
     */
    LUMP_TEXDATA(2),
    /**
     * Vertex array
     */
    LUMP_VERTEXES(3, VertexHander()),
    /**
     * Compressed visibility bit arrays
     */
    LUMP_VISIBILITY(4),
    /**
     * BSP tree nodes
     */
    LUMP_NODES(5),
    /**
     * Face texture array
     */
    LUMP_TEXINFO(6),
    /**
     * Face array
     */
    LUMP_FACES(7, FaceHandler()),
    /**
     * Lightmap samples
     */
    LUMP_LIGHTING(8),
    /**
     * Occlusion polygons and vertices
     */
    LUMP_OCCLUSION(9),
    /**
     * BSP tree leaf nodes
     */
    LUMP_LEAFS(10),
    /**
     * Correlates between dfaces and Hammer face IDs. Also used as random seed for detail prop placement.
     */
    LUMP_FACEIDS(11),
    /**
     * Edge array
     */
    LUMP_EDGES(12, EdgeHandler()),
    /**
     * Index of edges
     */
    LUMP_SURFEDGES(13, SurfaceEdgeHandler()),
    /**
     * Brush models (geometry of brush entities)
     */
    LUMP_MODELS(14),
    /**
     * Internal world lights converted from the entity lump
     */
    LUMP_WORLDLIGHTS(15),
    /**
     * Index to faces in each leaf
     */
    LUMP_LEAFFACES(16),
    /**
     * Index to brushes in each leaf
     */
    LUMP_LEAFBRUSHES(17),
    /**
     * Brush array
     */
    LUMP_BRUSHES(18),
    /**
     * Brushside array
     */
    LUMP_BRUSHSIDES(19),
    /**
     * Area array
     */
    LUMP_AREAS(20),
    /**
     * Portals between areas
     */
    LUMP_AREAPORTALS(21),
    /**
     *
     */
    LUMP_UNUSED0(22),
    /**
     *
     */
    LUMP_UNUSED1(23),
    /**
     *
     */
    LUMP_UNUSED2(24),
    /**
     *
     */
    LUMP_UNUSED3(25),
    /**
     * Displacement surface array
     */
    LUMP_DISPINFO(26),
    /**
     * Brush faces array before splitting
     */
    LUMP_ORIGINALFACES(27),
    /**
     * Displacement physics collision data
     */
    LUMP_PHYSDISP(28),
    /**
     * Physics collision data
     */
    LUMP_PHYSCOLLIDE(29),
    /**
     * Face plane normals
     */
    LUMP_VERTNORMALS(30),
    /**
     * Face plane normal index array
     */
    LUMP_VERTNORMALINDICES(31),
    /**
     * Displacement lightmap alphas (unused/empty since Source 2006)
     */
    LUMP_DISP_LIGHTMAP_ALPHAS(32),
    /**
     * Vertices of displacement surface meshes
     */
    LUMP_DISP_VERTS(33),
    /**
     * Displacement lightmap sample positions
     */
    LUMP_DISP_LIGHTMAP_SAMPLE_POSITIONS(34),
    /**
     * Game-specific data lump
     */
    LUMP_GAME_LUMP(35),
    /**
     * Data for leaf nodes that are inside water
     */
    LUMP_LEAFWATERDATA(36),
    /**
     * Water polygon data
     */
    LUMP_PRIMITIVES(37),
    /**
     * Water polygon vertices
     */
    LUMP_PRIMVERTS(38),
    /**
     * Water polygon vertex index array
     */
    LUMP_PRIMINDICES(39),
    /**
     * Embedded uncompressed Zip-format file
     */
    LUMP_PAKFILE(40, PakfileHandler()),
    /**
     * Clipped portal polygon vertices
     */
    LUMP_CLIPPORTALVERTS(41),
    /**
     * env_cubemap location array
     */
    LUMP_CUBEMAPS(42),
    /**
     * Texture name data
     */
    LUMP_TEXDATA_STRING_DATA(43),
    /**
     * Index array into texdata string data
     */
    LUMP_TEXDATA_STRING_TABLE(44),
    /**
     * info_overlay data array
     */
    LUMP_OVERLAYS(45),
    /**
     * Distance from leaves to water
     */
    LUMP_LEAFMINDISTTOWATER(46),
    /**
     * Macro texture info for faces
     */
    LUMP_FACE_MACRO_TEXTURE_INFO(47),
    /**
     * Displacement surface triangles
     */
    LUMP_DISP_TRIS(48),
    /**
     * Compressed win32-specific Havok terrain surface collision data. Deprecated and no longer used.
     */
    LUMP_PHYSCOLLIDESURFACE(49),
    /**
     * info_overlay's on water faces?
     */
    LUMP_WATEROVERLAYS(50),
    /**
     * Index of LUMP_LEAF_AMBIENT_LIGHTING_HDR
     */
    LUMP_LEAF_AMBIENT_INDEX_HDR(51),
    /**
     * Index of LUMP_LEAF_AMBIENT_LIGHTING
     */
    LUMP_LEAF_AMBIENT_INDEX(52),
    /**
     * HDR lightmap samples
     */
    LUMP_LIGHTING_HDR(53),
    /**
     * Internal HDR world lights converted from the entity lump
     */
    LUMP_WORLDLIGHTS_HDR(54),
    /**
     * HDR related leaf lighting data?
     */
    LUMP_LEAF_AMBIENT_LIGHTING_HDR(55),
    /**
     * HDR related leaf lighting data?
     */
    LUMP_LEAF_AMBIENT_LIGHTING(56),
    /**
     * XZip version of pak file for Xbox. Deprecated.
     */
    LUMP_XZIPPAKFILE(57),
    /**
     * HDR maps may have different face data
     */
    LUMP_FACES_HDR(58),
    /**
     * Extended level-wide flags. Not present in all levels.
     */
    LUMP_MAP_FLAGS(59),
    /**
     * Fade distances for overlays
     */
    LUMP_OVERLAY_FADES(60),
    /**
     * System level settings (min/max CPU & GPU to render this overlay)
     */
    LUMP_OVERLAY_SYSTEM_LEVELS(61),
    /**
     *
     */
    LUMP_PHYSLEVEL(62),
    /**
     * Displacement multiblend info
     */
    LUMP_DISP_MULTIBLEND(63);

    private val LOG = Logger()

    override fun toString(): String {
        return MessageFormat.format("{0} ({1})", name(), ID)
    }

    public fun <T : Any> handle(l: Lump, `in`: OrderedInputStream): T? {
        if (handler == null) {
            LOG.warning { "No handler for ${this}" }
            return null
        }
        return handler.handle(l, `in`) as T
    }
}
