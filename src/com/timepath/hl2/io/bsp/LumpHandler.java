package com.timepath.hl2.io.bsp;

import com.timepath.io.OrderedInputStream;
import java.io.IOException;

/**
 *
 * @author TimePath
 */
interface LumpHandler {
    
    void handle(Lump t, OrderedInputStream in) throws IOException;
    
}
