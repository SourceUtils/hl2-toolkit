package com.timepath.hl2.io.demo;

import com.timepath.io.BitBuffer;
import java.util.List;

/**
 *
 * @author TimePath
 */
public abstract class PacketHandler {

    boolean read(BitBuffer bb, List<Object> l) {
        return false;
    }

    boolean read(BitBuffer bb, List<Object> l, HL2DEM demo) {
        return read(bb, l);
    }

}
