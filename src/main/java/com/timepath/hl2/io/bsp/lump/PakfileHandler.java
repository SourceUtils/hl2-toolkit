package com.timepath.hl2.io.bsp.lump;

import com.timepath.hl2.io.bsp.Lump;
import com.timepath.hl2.io.bsp.LumpHandler;
import com.timepath.io.OrderedInputStream;
import com.timepath.vfs.ZipFS;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
class PakfileHandler implements LumpHandler<ZipFS> {

    private static final Logger LOG = Logger.getLogger(PakfileHandler.class.getName());

    PakfileHandler() {
    }

    @NotNull
    @Override
    public ZipFS handle(@NotNull Lump l, @NotNull OrderedInputStream in) throws IOException {
        LOG.log(Level.INFO, "Unzipping {0}", new Object[]{l});
        @NotNull byte[] data = new byte[l.length];
        in.readFully(data);
        return new ZipFS(data);
    }
}
