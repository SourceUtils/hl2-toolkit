package com.timepath.hl2.io.bsp

import com.timepath.io.OrderedInputStream

import java.io.IOException

/**
 * @param <T> The return type of this handler
 */
public interface LumpHandler<T> {

    throws(IOException::class)
    public fun handle(l: Lump, `in`: OrderedInputStream): T
}
