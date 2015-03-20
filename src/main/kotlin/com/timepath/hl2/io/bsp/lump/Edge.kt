package com.timepath.hl2.io.bsp.lump

import com.timepath.io.struct.StructField

import java.util.logging.Logger

public class Edge {
    /**
     * Vertex indices
     */
    StructField
    public var v: ShortArray = ShortArray(2)

    companion object {

        private val LOG = Logger.getLogger(javaClass<Edge>().getName())
    }
}
