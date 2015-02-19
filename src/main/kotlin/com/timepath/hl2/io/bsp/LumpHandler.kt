package com.timepath.hl2.io.bsp

import com.timepath.io.OrderedInputStream

import java.io.IOException

/**
 * @param <T> The return type of this handler
 * @author TimePath
 */
public trait LumpHandler<T> {

    throws(javaClass<IOException>())
    public fun handle(l: Lump, `in`: OrderedInputStream): T
}
