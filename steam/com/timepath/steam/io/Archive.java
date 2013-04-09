package com.timepath.steam.io;

import java.io.InputStream;

/**
 *
 * @author timepath
 */
public interface Archive {

    public InputStream get(int index);

}
