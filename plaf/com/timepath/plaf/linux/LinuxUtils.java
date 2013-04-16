package com.timepath.plaf.linux;

import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class LinuxUtils {

    public static String getLinuxStore() {
        String root = System.getenv("XDG_DATA_HOME");
        if(root == null) {
            root = System.getenv("HOME") + "/.local/share/";
        }
        return root;
    }

    private LinuxUtils() {
    }

    private static final Logger LOG = Logger.getLogger(LinuxUtils.class.getName());

}
