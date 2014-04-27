package com.timepath.hl2.io.bsp.lump;

import com.timepath.hl2.io.bsp.Lump;
import com.timepath.hl2.io.bsp.LumpHandler;
import com.timepath.io.OrderedInputStream;
import java.io.IOException;
import java.util.logging.Logger;

class SurfaceEdgeHandler implements LumpHandler<int[]> {

    private static final Logger LOG = Logger.getLogger(SurfaceEdgeHandler.class.getName());

    public int[] handle(Lump l, OrderedInputStream in) throws IOException {
        int[] e = new int[l.length / 4];
        for(int i = 0; i < e.length; i++) {
            e[i] = in.readInt();
        }
        return e;
    }

}
