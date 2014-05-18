package com.timepath.hl2.io.demo;

import com.timepath.Pair;
import com.timepath.io.BitBuffer;

import java.util.List;

/**
 * @author TimePath
 */
abstract class PacketHandler {

    protected PacketHandler() {}

    boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
        return read(bb, l, demo, -1);
    }

    boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo, int lengthBits) {
        return false;
    }
}
