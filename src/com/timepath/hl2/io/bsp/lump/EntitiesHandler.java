package com.timepath.hl2.io.bsp.lump;

import com.timepath.hl2.io.bsp.Lump;
import com.timepath.hl2.io.bsp.LumpHandler;
import com.timepath.io.OrderedInputStream;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
class EntitiesHandler implements LumpHandler<String> {

    private static final Logger LOG = Logger.getLogger(EntitiesHandler.class.getName());

    public String handle(Lump l, OrderedInputStream in) throws IOException {
        return in.readString();
    }
}
