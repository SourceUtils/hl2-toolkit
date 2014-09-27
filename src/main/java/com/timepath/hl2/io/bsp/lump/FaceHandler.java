package com.timepath.hl2.io.bsp.lump;

import com.timepath.hl2.io.bsp.Lump;
import com.timepath.hl2.io.bsp.LumpHandler;
import com.timepath.io.OrderedInputStream;
import com.timepath.io.struct.Struct;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

class FaceHandler implements LumpHandler<Face[]> {

    private static final Logger LOG = Logger.getLogger(FaceHandler.class.getName());
    private static final int MAX_MAP_FACES = 65536;

    FaceHandler() {
    }

    @NotNull
    @Override
    public Face[] handle(@NotNull Lump l, @NotNull OrderedInputStream in) throws IOException {
        try {
            @NotNull Face[] e = new Face[l.length / Struct.sizeof(new Face())];
            for (int i = 0; i < e.length; i++) {
                e[i] = in.readStruct(new Face());
            }
            return e;
        } catch (@NotNull InstantiationException | IllegalAccessException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        return new Face[0];
    }
}
