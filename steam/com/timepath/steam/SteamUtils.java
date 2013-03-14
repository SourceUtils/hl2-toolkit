package com.timepath.steam;

import java.util.logging.Logger;

import com.timepath.plaf.OS;

/**
 *
 * @author timepath
 */
public class SteamUtils {

    private static final Logger LOG = Logger.getLogger(SteamUtils.class.getName());

    private SteamUtils() {
    }

    public static String locateSteamAppsDirectory() {
        if(OS.isWindows()) {
            String str = System.getenv("PROGRAMFILES(x86)");
            if(str == null) {
                str = System.getenv("PROGRAMFILES");
            }
            return str + "/Steam/steamapps/";
        } else if(OS.isMac()) {
            return "~/Library/Application Support/Steam/SteamApps/";
        } else if(OS.isLinux()) {
            return System.getenv("HOME") + "/.steam/root/SteamApps/";
        } else {
            return null;
        }
    }

    
    
}
