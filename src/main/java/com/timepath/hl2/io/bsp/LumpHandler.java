package com.timepath.hl2.io.bsp;

import com.timepath.io.OrderedInputStream;

import java.io.IOException;

/**
 * @param <T> The return type of this handler
 * @author TimePath
 */
public interface LumpHandler<T> {

    T handle(Lump l, OrderedInputStream in) throws IOException;
}
