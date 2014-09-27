package com.timepath.hl2.io.bsp;

import com.timepath.io.OrderedInputStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * @param <T> The return type of this handler
 * @author TimePath
 */
public interface LumpHandler<T> {

    @NotNull
    T handle(Lump l, OrderedInputStream in) throws IOException;
}
