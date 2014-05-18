package com.timepath.hl2.io.util;

import com.timepath.io.struct.StructField;

/**
 * @author TimePath
 */
public class Vector3f {

    @StructField(index = 0)
    private float x;
    @StructField(index = 1)
    private float y;
    @StructField(index = 2)
    private float z;

    public Vector3f() {
    }

    public Vector3f(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString() {
        return String.format("vec3(%s, %s, %s)", x, y, z);
    }
}
