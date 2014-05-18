package com.timepath.hl2.io.bsp.lump;

import com.timepath.hl2.io.util.Vector3f;
import com.timepath.io.struct.StructField;

import java.util.logging.Logger;

public class Plane {

    private static final Logger LOG = Logger.getLogger(Plane.class.getName());
    /**
     * Normal vector
     */
    @StructField(index = 0)
    public Vector3f normal;
    /**
     * Distance from origin
     */
    @StructField(index = 1)
    public float    dist;
    /**
     * Plane axis identifier
     */
    @StructField(index = 2)
    public int      type;

    public Plane() {}
}
