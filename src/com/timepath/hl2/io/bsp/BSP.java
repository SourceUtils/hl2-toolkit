package com.timepath.hl2.io.bsp;

import com.timepath.hl2.io.bsp.lump.LumpType;
import com.timepath.io.OrderedInputStream;
import com.timepath.io.struct.StructField;
import java.io.*;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * https://developer.valvesoftware.com/wiki/Source_BSP_File_Format
 * https://github.com/TimePath/webgl-source/blob/master/js/source-bsp.js
 * https://github.com/TimePath/webgl-source/blob/master/js/source-bsp-struct.js
 * https://github.com/TimePath/webgl-source/blob/master/js/source-bsp-tree.js
 * <p/>
 * @author TimePath
 */
public class BSP {

    private static final Logger LOG = Logger.getLogger(BSP.class.getName());

    public static BSP load(InputStream is) throws IOException {
        try {
            OrderedInputStream in = new OrderedInputStream(new BufferedInputStream(is));
            in.order(ByteOrder.LITTLE_ENDIAN);
            in.mark(in.available());
            BSPHeader header = in.readStruct(new BSPHeader());

            // TODO: Other BSP types
            VBSP bsp = new VBSP();
            bsp.in = in;
            bsp.header = header;

            // TODO: Struct parser callbacks
            for(int i = 0; i < header.lumps.length; i++) {
                header.lumps[i].type = LumpType.values()[i];
            }
            return bsp;
        } catch(InstantiationException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch(IllegalAccessException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return null;
    }

    protected BSPHeader header;

    protected OrderedInputStream in;

    protected BSP() {
    }

    /**
     *
     * Examples:
     * <br/>
     * {@code String ents = b.<String>getLump(LumpType.LUMP_ENTITIES);}
     * <br/>
     * {@code String ents = (String) b.getLump(LumpType.LUMP_ENTITIES);}
     * <br/>
     * {@code String ents = b.getLump(LumpType.LUMP_ENTITIES);}
     * <p/>
     * @param <T>  Expected return type. TODO: Wouldn't it be nice if we just knew at compile time?
     * @param type The lump
     * <p/>
     * @return The lump
     * <p/>
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public <T> T getLump(LumpType type) throws IOException {
        return this.<T>getLump(type, (LumpHandler<T>) type.getHandler());
    }

    /**
     * Intended for overriding to change handler functionality
     * <p/>
     * @param <T>
     * @param type
     * @param handler
     * <p/>
     * @return
     * <p/>
     * @throws IOException
     */
    public <T> T getLump(LumpType type, LumpHandler<T> handler) throws IOException {
        if(handler == null) {
            return null;
        }
        Lump lump = header.lumps[type.getID()];
        if(lump.isEmpty()) {
            return null;
        }
        in.reset();
        in.skipBytes(lump.offset);
        return handler.handle(lump, in);
    }

    /**
     *
     * @return The map revision
     */
    public int getRevision() {
        return header.mapRevision;
    }

    public FloatBuffer getVertices() {
        try {
            return getLump(LumpType.LUMP_VERTEXES);
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
            return null;
        }
    }

    private static class BSPHeader {

        /**
         * BSP file identifier: VBSP
         */
        @StructField(index = 0)
        int ident;

        /**
         * BSP file identifier: VBSP
         */
        @StructField(index = 2)
        Lump[] lumps = new Lump[64];

        /**
         * The map's revision (iteration, version) number
         */
        @StructField(index = 3)
        int mapRevision;

        /**
         * BSP file version
         */
        @StructField(index = 1)
        int version;

    }

}
