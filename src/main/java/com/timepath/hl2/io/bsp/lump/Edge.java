package com.timepath.hl2.io.bsp.lump;

import com.timepath.io.struct.StructField;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class Edge {

    private static final Logger LOG = Logger.getLogger(Edge.class.getName());
    /**
     * Vertex indices
     */
    @NotNull
    @StructField
    public short[] v = new short[2];

    public Edge() {
    }
}
