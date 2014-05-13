package com.timepath.hl2.io.bsp.lump;

import com.timepath.hl2.io.bsp.Lump;
import com.timepath.hl2.io.bsp.LumpHandler;
import com.timepath.io.OrderedInputStream;
import com.timepath.io.struct.Struct;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

class PlaneHandler implements LumpHandler<Plane[]> {

    private static final Logger LOG            = Logger.getLogger(PlaneHandler.class.getName());
    private static final int    MAX_MAP_PLANES = 65536;

    PlaneHandler() {}

    @Override
    public Plane[] handle(Lump l, OrderedInputStream in) throws IOException {
        try {
            Plane[] e = new Plane[l.length / Struct.sizeof(new Plane())];
            for(int i = 0; i < e.length; i++) {
                e[i] = in.readStruct(new Plane());
            }
            return e;
        } catch(InstantiationException | IllegalAccessException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return new Plane[0];
    }
}
