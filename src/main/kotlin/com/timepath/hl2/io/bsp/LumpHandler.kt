package com.timepath.hl2.io.bsp

import com.timepath.io.OrderedInputStream

/** @param <T> The return type of this handler */
public interface LumpHandler<T> {
    public fun invoke(l: Lump, ois: OrderedInputStream): T
}
