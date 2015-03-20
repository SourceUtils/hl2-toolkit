package com.timepath.hl2.io.util

import com.timepath.io.struct.StructField

public class Vector3f(x: Float = 0f, y: Float = 0f, z: Float = 0f) {
    StructField(index = 0)
    private val x: Float = x
    StructField(index = 1)
    private val y: Float = y
    StructField(index = 2)
    private val z: Float = z

    override fun toString() = "vec3($x, $y, $z)"
}
