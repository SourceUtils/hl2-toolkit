package com.timepath.hl2.io.bsp.lump;

import com.timepath.hl2.io.bsp.Lump;
import com.timepath.hl2.io.bsp.LumpHandler;
import com.timepath.io.OrderedInputStream;
import com.timepath.io.struct.Struct;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

class EdgeHandler implements LumpHandler<Edge[]> {

    private static final Logger LOG           = Logger.getLogger(EdgeHandler.class.getName());
    private static final int    MAX_MAP_EDGES = 256000;

    public Edge[] handle(Lump l, OrderedInputStream in) throws IOException {
        try {
            Edge[] e = new Edge[l.length / Struct.sizeof(new Edge())]; for(int i = 0; i < e.length; i++) {
                e[i] = in.readStruct(new Edge());
            } return e;
        } catch(InstantiationException | IllegalAccessException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } return new Edge[0];
    }
}
