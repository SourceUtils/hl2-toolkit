package com.timepath.hl2.io.bsp

import com.timepath.hl2.io.bsp.lump.LumpType
import com.timepath.io.OrderedInputStream
import com.timepath.io.struct.StructField

import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.properties.Delegates

/**
 * @author TimePath
 * @see <a>https://developer.valvesoftware.com/wiki/Source_BSP_File_Format</a>
 * @see <a>https://github.com/TimePath/webgl-source/blob/master/js/source-bsp.js</a>
 * @see <a>https://github.com/TimePath/webgl-source/blob/master/js/source-bsp-struct.js</a>
 * @see <a>https://github.com/TimePath/webgl-source/blob/master/js/source-bsp-tree.js</a>
 */
public abstract class BSP {
    var header: BSPHeader by Delegates.notNull()
    var `in`: OrderedInputStream by Delegates.notNull()
    public var indices: IntBuffer by Delegates.notNull()
    public var vertices: FloatBuffer by Delegates.notNull()

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
     * @param type The lump
     * @return The lump
     * @throws IOException
     */
    SuppressWarnings("unchecked")
    throws(javaClass<IOException>())
    public fun <T> getLump(type: LumpType): T? {
        return getLump(type, type.handler as LumpHandler<T>)
    }

    /**
     * Intended for overriding to change handler functionality
     *
     * @param <T>
     * @param type
     * @param handler
     * @return
     * @throws IOException
     */
    throws(javaClass<IOException>())
    protected fun <T> getLump(type: LumpType, handler: LumpHandler<T>?): T? {
        if (handler == null) {
            return null
        }
        val lump = header.lumps[type.ID]!!
        if (lump.isEmpty()) {
            return null
        }
        `in`.reset()
        `in`.skipBytes(lump.offset)
        return handler.handle(lump, `in`)
    }

    /**
     * @return The map revision
     */
    fun getRevision(): Int {
        return header.mapRevision
    }

    throws(javaClass<IOException>())
    abstract fun process()

    private class BSPHeader () {

        /**
         * BSP file identifier: VBSP
         */
        StructField(index = 0)
        var ident: Int = 0
        /**
         * BSP file identifier: VBSP
         */
        StructField(index = 2)
        var lumps = arrayOfNulls<Lump>(64)
        /**
         * The map's revision (iteration, version) number
         */
        StructField(index = 3)
        var mapRevision: Int = 0
        /**
         * BSP file version
         */
        StructField(index = 1)
        var version: Int = 0
    }

    class object {

        private val LOG = Logger.getLogger(javaClass<BSP>().getName())

        throws(javaClass<IOException>())
        public fun load(`is`: InputStream): BSP? {
            try {
                val `in` = OrderedInputStream(BufferedInputStream(`is`))
                `in`.order(ByteOrder.LITTLE_ENDIAN)
                `in`.mark(`in`.available())
                val header = `in`.readStruct<BSPHeader>(BSPHeader())
                // TODO: Other BSP types
                val bsp = VBSP()
                bsp.`in` = `in`
                bsp.header = header
                // TODO: Struct parser callbacks
                for (i in header.lumps.size().indices) {
                    header.lumps[i]!!.type = LumpType.values()[i]
                }
                LOG.info("Processing map...")
                bsp.process()
                return bsp
            } catch (ex: InstantiationException) {
                LOG.log(Level.SEVERE, null, ex)
            } catch (ex: IllegalAccessException) {
                LOG.log(Level.SEVERE, null, ex)
            }

            return null
        }
    }
}
