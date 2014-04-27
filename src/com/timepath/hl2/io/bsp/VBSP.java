package com.timepath.hl2.io.bsp;

import com.timepath.hl2.io.bsp.lump.LumpType;
import com.timepath.steam.io.storage.ACF;
import com.timepath.vfs.ZipFS;
import java.util.logging.Logger;

/**
 *
 * @author TimePath
 */
public class VBSP extends BSP {

    private static final Logger LOG = Logger.getLogger(VBSP.class.getName());

    public static void main(String[] args) throws Exception {
        BSP b = BSP.load(ACF.fromManifest(440).get("tf/maps/ctf_2fort.bsp").stream());
        System.out.println(b.getRevision());
        String ents = b.getLump(LumpType.LUMP_ENTITIES);
//        System.out.println(ents);
        ZipFS z = b.getLump(LumpType.LUMP_PAKFILE);
        System.out.println(z.name);
        b.getLump(LumpType.LUMP_VERTEXES);
    }

}
