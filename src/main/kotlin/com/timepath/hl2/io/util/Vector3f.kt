package com.timepath.hl2.io.util

import com.timepath.io.struct.StructField

public class Vector3f(x: Float = 0f, y: Float = 0f, z: Float = 0f) {
    StructField(index = 0)
    private val x: Float = x
    StructField(index = 1)
    private val y: Float = x
    StructField(index = 2)
    private val z: Float = x

    override fun toString(): String {
        return java.lang.String.format("vec3(%s, %s, %s)", x, y, z)
    }
}
