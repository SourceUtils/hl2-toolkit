package com.timepath.steam;

import com.timepath.plaf.OS;
import java.io.File;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class SteamUtils {

    private static final Logger LOG = Logger.getLogger(SteamUtils.class.getName());

    private SteamUtils() {
    }

    public static File getSteamApps() {
        File steam = getSteam();
        switch(OS.get()) {
            case Windows:
                return new File(steam, "steamapps");
            case OSX:
            case Linux:
                return new File(steam, "SteamApps");
            default:
                return null;
        }
    }

    public static File getSteam() {
        switch(OS.get()) {
            case Windows:
                String str = System.getenv("PROGRAMFILES(x86)");
                if(str == null) {
                    str = System.getenv("PROGRAMFILES");
                }
                return new File(str, "Steam");
            case OSX:
                return new File("~/Library/Application Support/Steam");
            case Linux:
                return new File(System.getenv("HOME") + "/.steam/steam");
            default:
                return null;
        }
    }
}
