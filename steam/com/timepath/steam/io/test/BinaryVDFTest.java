package com.timepath.steam.io.test;

import com.timepath.steam.SteamUtils;
import com.timepath.steam.io.BinaryVDF;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class BinaryVDFTest {

    public static void main(String... args) throws IOException {
//        System.out.println(new BinaryVDF(Utils.locateSteamAppsDirectory() + "../appcache/packageinfo.vdf"));
        BinaryVDF bvdf = new BinaryVDF(SteamUtils.locateSteamAppsDirectory() + "../appcache/appinfo.vdf");
        LOG.log(Level.INFO, "{0}", bvdf);
    }

    private static final Logger LOG = Logger.getLogger(BinaryVDFTest.class.getName());
}