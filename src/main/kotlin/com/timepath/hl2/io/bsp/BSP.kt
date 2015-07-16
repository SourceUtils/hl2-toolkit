package com.timepath.hl2.io.bsp

import com.timepath.Logger
import com.timepath.hl2.io.bsp.lump.LumpType
import com.timepath.io.OrderedInputStream
import com.timepath.io.struct.StructField
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer

/**
 * @see <a>https://developer.valvesoftware.com/wiki/Source_BSP_File_Format</a>
 * @see <a>https://github.com/TimePath/webgl-source/blob/master/js/source-bsp.js</a>
 * @see <a>https://github.com/TimePath/webgl-source/blob/master/js/source-bsp-struct.js</a>
 * @see <a>https://github.com/TimePath/webgl-source/blob/master/js/source-bsp-tree.js</a>
 */
public abstract class BSP(
        val header: BSP.Header,
        val input: OrderedInputStream
) {
    public var indices: IntBuffer? = null
    public var vertices: FloatBuffer? = null

    /**
     * Examples:
     * <br/>
     * {@code String ents = b.<String>getLump(LumpType.LUMP_ENTITIES);}
     * <br/>
     * {@code String ents = (String) b.getLump(LumpType.LUMP_ENTITIES);}
     * <br/>
     * {@code String ents = b.getLump(LumpType.LUMP_ENTITIES);}
     *
     * @param <T>  Expected return type. TODO: Wouldn't it be nice if we just knew at compile time?
     */
    public fun <T : Any> getLump(type: LumpType): T? {
        return getLump(type, type.handler as LumpHandler<T>)
    }

    /**
     * Intended for overriding to change handler functionality
     */
    protected fun <T : Any> getLump(type: LumpType, handler: LumpHandler<T>?): T? {
        if (handler == null) return null
        val lump = header.lumps[type.ID]!!
        if (lump.isEmpty()) return null
        input.reset()
        input.skipBytes(lump.offset)
        return handler.invoke(lump, input)
    }

    val revision: Int get() = header.mapRevision

    abstract fun process()

    class Header(
            /** BSP file identifier: VBSP */
            @StructField(index = 0) var ident: Int = 0,
            /** BSP file version */
            @StructField(index = 1) var version: Int = 0,
            /** BSP file identifier: VBSP */
            @StructField(index = 2) var lumps: Array<Lump?> = arrayOfNulls<Lump>(64),
            /** The map's revision (iteration, version) number */
            @StructField(index = 3) var mapRevision: Int = 0
    )

    companion object {

        private val LOG = Logger()

        public fun load(`is`: InputStream): BSP? {
            val input = OrderedInputStream(BufferedInputStream(`is`))
            input.order(ByteOrder.LITTLE_ENDIAN)
            input.mark(input.available())
            val header = input.readStruct<Header>(Header())
            // TODO: Other BSP types
            val bsp = VBSP(header, input)
            // TODO: Struct parser callbacks
            for (i in 0..header.lumps.size() - 1) {
                header.lumps[i]!!.type = LumpType.values()[i]
            }
            LOG.info { "Processing map..." }
            bsp.process()
            return bsp
        }
    }
}
