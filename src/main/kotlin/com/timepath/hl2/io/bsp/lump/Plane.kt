package com.timepath.hl2.io.bsp.lump

import com.timepath.hl2.io.util.Vector3f
import com.timepath.io.struct.StructField

import java.util.logging.Logger
import kotlin.properties.Delegates

public class Plane {
    /**
     * Normal vector
     */
    StructField(index = 0)
    public var normal: Vector3f by Delegates.notNull()
    /**
     * Distance from origin
     */
    StructField(index = 1)
    public var dist: Float = 0.toFloat()
    /**
     * Plane axis identifier
     */
    StructField(index = 2)
    public var type: Int = 0

    class object {

        private val LOG = Logger.getLogger(javaClass<Plane>().getName())
    }
}
