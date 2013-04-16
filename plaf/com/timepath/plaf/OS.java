package com.timepath.plaf;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public enum OS {

    Windows, OSX, Linux, Other;

    private static final Logger LOG = Logger.getLogger(OS.class.getName());

    private final static OS system;

    static {
        //<editor-fold defaultstate="collapsed" desc="OS detection">
        String osVer = System.getProperty("os.name").toLowerCase();
        if(osVer.indexOf("windows") != -1) {
            system = OS.Windows;
        } else if(osVer.indexOf("mac os x") != -1 || osVer.indexOf("OS X") != -1 || osVer.indexOf("mac") != -1) {
            system = OS.OSX;
        } else if(osVer.indexOf("Linux") != -1 || osVer.indexOf("nix") != -1 || osVer.indexOf("nux") != -1) {
            system = OS.Linux;
        } else {
            system = OS.Other;
            LOG.log(Level.INFO, "OS string: {0}", osVer);
        }
        LOG.log(Level.INFO, "OS: {0}", system);
        //</editor-fold>
    }

    public static OS get() {
        return system;
    }

    public static boolean isWindows() {
        return system == Windows;
    }

    public static boolean isMac() {
        return system == OSX;
    }

    public static boolean isLinux() {
        return system == Linux;
    }
}
