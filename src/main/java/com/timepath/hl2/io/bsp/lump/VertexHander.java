package com.timepath.hl2.io.bsp.lump;

import com.timepath.hl2.io.bsp.Lump;
import com.timepath.hl2.io.bsp.LumpHandler;
import com.timepath.io.OrderedInputStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
class VertexHander implements LumpHandler<FloatBuffer> {

    private static final Logger LOG = Logger.getLogger(VertexHander.class.getName());
    private static final int MAX_MAP_VERTS = 65536;

    VertexHander() {
    }

    @NotNull
    @Override
    public FloatBuffer handle(@NotNull Lump l, @NotNull OrderedInputStream in) throws IOException {
        ByteBuffer verts = ByteBuffer.allocateDirect(l.length);
        @NotNull byte[] vertBuf = new byte[l.length];
        in.readFully(vertBuf);
        verts.put(vertBuf);
        verts.flip();
        return verts.asFloatBuffer();
    }
}
