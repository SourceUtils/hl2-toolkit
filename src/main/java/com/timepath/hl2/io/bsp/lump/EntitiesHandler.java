package com.timepath.hl2.io.bsp.lump;

import com.timepath.hl2.io.bsp.Lump;
import com.timepath.hl2.io.bsp.LumpHandler;
import com.timepath.io.OrderedInputStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
class EntitiesHandler implements LumpHandler<String> {

    private static final Logger LOG = Logger.getLogger(EntitiesHandler.class.getName());

    EntitiesHandler() {
    }

    @NotNull
    @Override
    public String handle(Lump l, @NotNull OrderedInputStream in) throws IOException {
        return in.readString();
    }
}
