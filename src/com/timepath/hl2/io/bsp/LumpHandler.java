package com.timepath.hl2.io.bsp;

import com.timepath.io.OrderedInputStream;
import java.io.IOException;

/**
 *
 * @author TimePath
 * @param <T> The return type of this handler
 */
public interface LumpHandler<T> {
    
    T handle(Lump t, OrderedInputStream in) throws IOException;
    
}
