package com.timepath.steam;

import com.timepath.plaf.OS;
import com.timepath.steam.io.VDF;
import java.io.File;
import java.math.BigInteger;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author timepath
 */
public class SteamUtils {

    private static final Logger LOG = Logger.getLogger(SteamUtils.class.getName());

    private SteamUtils() {
    }

    /*
     * https://developer.valvesoftware.com/wiki/SteamID
     * http://api.steampowered.com/ISteamWebAPIUtil/GetSupportedAPIList/v0001/?key=303E8E7C12216D62FD8F522602CE141C&format=vdf
     * http://forums.alliedmods.net/showthread.php?t=60899
     * http://forums.alliedmods.net/showthread.php?p=750532
     * http://sapi.techieanalyst.net/
     */
    
    public static class SteamID {
        public SteamID(String user, String id64, String uid, String sid) {
            this.user = user;
            this.id64 = id64;
            this.uid = uid;
            this.sid = sid;
        }
        public String user, id64, uid, sid;

        @Override
        public String toString() {
            return "[" + user + ", " + id64 + ", " + uid + ", " + sid + "]";
        }
        
    }
    
    public static SteamID getUser() {
        String username = VDF.load(new File(SteamUtils.getSteam(), "config/SteamAppData.vdf")).get("SteamAppData").get("AutoLoginUser").getValue();
        String id64 = VDF.load(new File(SteamUtils.getSteam(), "config/config.vdf")).get("InstallConfigStore").get("Software").get("Valve").get("Steam").get("Accounts").get(username).get("SteamID").getValue();
        String uid = SteamUtils.ID64toUID(id64);
        String sid = SteamUtils.UIDtoSID(uid);
        return new SteamID(username, id64, uid, sid);
    }
    
    /**
     * Steam_#
     * 0 from HL to TF2, 1 from L4D to CS:GO
     */
    public static final Pattern SID = Pattern.compile("STEAM_([0-9]):([0-9]):([0-9]{4,})");

    /**
     * http://steamcommunity.com/profiles/[uid]
     */
    public static final Pattern UID = Pattern.compile("U:([0-9]):([0-9]{4,})");

    /**
     * http://steamcommunity.com/profiles/id64
     */
    public static final Pattern ID64 = Pattern.compile("([0-9]{17,})");
    
    /**
     * The 4 is because hexadecimal; sqrt 16? 2^4 = 16? Probably that
     */
    private static final BigInteger id64Offset = BigInteger.valueOf(0x01100001).shiftLeft(8 * 4);

    public static String SIDto64ID(String steam) {
        return UIDtoID64(SIDtoUID(steam));
    }
    
    public static String SIDtoUID(String steam) {
        Matcher m = SID.matcher(steam);
        if(!m.matches()) {
            return null;
        }
        BigInteger id = new BigInteger(m.group(3)).multiply(BigInteger.valueOf(2)).add(new BigInteger(m.group(2)));
        return "U:1:" + id.toString();
    }

    public static String UIDtoSID(String steam) {
        Matcher m = UID.matcher(steam);
        if(!m.matches()) {
            return null;
        }
        BigInteger[] id = new BigInteger(m.group(2)).divideAndRemainder(BigInteger.valueOf(2));
        return "STEAM_0:" + id[1] + ":" + id[0];
    }

    public static String UIDtoID64(String steam) {
        Matcher m = UID.matcher(steam);
        if(!m.matches()) {
            return null;
        }
        BigInteger id = new BigInteger(m.group(2)).add(id64Offset);
        return id.toString();
    }
    
    public static String ID64toUID(String steam) {
        Matcher m = ID64.matcher(steam);
        if(!m.matches()) {
            return null;
        }
        BigInteger id = new BigInteger(m.group(1)).subtract(id64Offset);
        return "U:1:" + id.toString();
    }
    
    public static String ID64toSID(String steam) {
        return UIDtoSID(ID64toUID(steam));
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
