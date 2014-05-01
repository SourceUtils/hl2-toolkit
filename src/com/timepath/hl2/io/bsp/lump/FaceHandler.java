package com.timepath.hl2.io.bsp.lump;

import com.timepath.hl2.io.bsp.Lump;
import com.timepath.hl2.io.bsp.LumpHandler;
import com.timepath.io.OrderedInputStream;
import com.timepath.io.struct.Struct;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

class FaceHandler implements LumpHandler<Face[]> {

    private static final Logger LOG = Logger.getLogger(FaceHandler.class.getName());
    
    private static final int MAX_MAP_FACES = 65536;

    public Face[] handle(Lump l, OrderedInputStream in) throws IOException {
        try {
            Face[] e = new Face[l.length / Struct.sizeOf(new Face())];
            for(int i = 0; i < e.length; i++) {
                e[i] = in.readStruct(new Face());
            }
            return e;
        } catch(InstantiationException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch(IllegalAccessException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return new Face[0];
    }

}
