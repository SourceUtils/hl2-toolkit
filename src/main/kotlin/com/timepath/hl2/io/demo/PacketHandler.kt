package com.timepath.hl2.io.demo

import com.timepath.Pair
import com.timepath.io.BitBuffer

/**
 * @author TimePath
 */
trait PacketHandler {

    public fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int = -1): Boolean = false
}
