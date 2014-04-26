package com.timepath.hl2.io.bsp;

import com.timepath.io.OrderedInputStream;
import com.timepath.io.struct.StructField;
import com.timepath.steam.io.storage.ACF;
import com.timepath.vfs.ZipFS;
import java.io.*;
import java.nio.ByteOrder;
import java.util.EnumMap;
import java.util.Map;
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
public class BSP implements LumpHandler {

    private static int HEADER_LUMPS = 64;

    private static final Logger LOG = Logger.getLogger(BSP.class.getName());

    private static final Map<LumpType, LumpHandler> handlers;

    static {
        handlers = new EnumMap<LumpType, LumpHandler>(LumpType.class);

        handlers.put(LumpType.LUMP_ENTITIES, new LumpHandler() {

            public void handle(Lump l, OrderedInputStream in) throws IOException {
                String s = in.readString();
            }
        });

        handlers.put(LumpType.LUMP_PAKFILE, new LumpHandler() {

            public void handle(Lump l, OrderedInputStream in) throws IOException {
                LOG.log(Level.INFO, "Unzipping {0}", new Object[] {l});
                byte[] data = new byte[l.length];
                in.readFully(data);
                ZipFS zfs = new ZipFS(data);
            }
        });
    }

    public static void main(String[] args) throws Exception {
        BSP b = new BSP(ACF.fromManifest(440).get("tf/maps/ctf_2fort.bsp").stream());

        for(Lump l : b.header.lumps) {
            b.readLump(l);
        }
    }

    BSPHeader header;

    OrderedInputStream in;

    public BSP(InputStream is) throws IOException, InstantiationException, IllegalAccessException {
        in = new OrderedInputStream(new BufferedInputStream(is));
        in.order(ByteOrder.LITTLE_ENDIAN);
        in.mark(in.available());
        header = in.readStruct(new BSPHeader());

        // TODO: Struct parser callbacks
        for(int i = 0; i < header.lumps.length; i++) {
            header.lumps[i].type = LumpType.values()[i];
        }
    }

    private BSP() {
    }

    public void handle(Lump l, OrderedInputStream in) throws IOException {
        LumpHandler handler = handlers.get(l.type);
        if(handler == null) {
            LOG.log(Level.FINE, "No handler for {0}", l);
        } else {
            handler.handle(l, in);
        }
    }

    void readLump(Lump l) throws IOException {
        if(l.isEmpty()) {
            return;
        }
        in.reset();
        in.skipBytes(l.offset);

        handle(l, in);
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
        Lump[] lumps = new Lump[HEADER_LUMPS];

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
