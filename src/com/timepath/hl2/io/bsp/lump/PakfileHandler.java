/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.timepath.hl2.io.bsp.lump;

import com.timepath.hl2.io.bsp.Lump;
import com.timepath.hl2.io.bsp.LumpHandler;
import com.timepath.io.OrderedInputStream;
import com.timepath.vfs.ZipFS;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author TimePath
 */
public class PakfileHandler implements LumpHandler<ZipFS> {

    private static final Logger LOG = Logger.getLogger(PakfileHandler.class.getName());

    public ZipFS handle(Lump l, OrderedInputStream in) throws IOException {
        LOG.log(Level.INFO, "Unzipping {0}", new Object[] {l});
        byte[] data = new byte[l.length];
        in.readFully(data);
        return new ZipFS(data);
    }

}
