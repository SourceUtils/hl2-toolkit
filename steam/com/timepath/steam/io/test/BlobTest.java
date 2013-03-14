package com.timepath.steam.io.test;

import com.timepath.steam.io.Blob;
import java.io.File;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class BlobTest {

    public static void main(String... args) {
        System.out.println(new Blob(new File("/home/timepath/.local/share/Steam/AppUpdateStats.blob")).toString());
//        System.out.println(new Blob(new File("/home/timepath/.local/share/Steam/ClientRegistry.blob")).toString());
    }

    private static final Logger LOG = Logger.getLogger(BlobTest.class.getName());
}
