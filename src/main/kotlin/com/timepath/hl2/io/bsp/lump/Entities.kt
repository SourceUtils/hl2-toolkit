package com.timepath.hl2.io.bsp.lump

import com.timepath.hl2.io.bsp.Lump
import com.timepath.hl2.io.bsp.LumpHandler
import com.timepath.io.OrderedInputStream

class Entities {
    class Handler : LumpHandler<String> {
        override fun invoke(l: Lump, ois: OrderedInputStream): String {
            return ois.readString()
        }
    }
}
