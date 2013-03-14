package com.timepath.plaf;

import com.timepath.plaf.linux.DesktopLauncher;
import java.awt.Toolkit;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public enum OS {

    Windows, Mac, Linux, Other;
    
    private static final Logger LOG = Logger.getLogger(OS.class.getName());
    
    private static OS system;
    
    static {
        //<editor-fold defaultstate="collapsed" desc="OS detection">
        String osVer = System.getProperty("os.name").toLowerCase();
        if(osVer.indexOf("windows") != -1) {
            system = OS.Windows;
        } else if(osVer.indexOf("mac os x") != -1 || osVer.indexOf("OS X") != -1 || osVer.indexOf("mac") != -1) {
            system = OS.Mac;
        } else if(osVer.indexOf("Linux") != -1 || osVer.indexOf("nix") != -1 || osVer.indexOf("nux") != -1) {
            system = OS.Linux;
        } else {
            system = OS.Other;
            LOG.log(Level.WARNING, "Unrecognised OS: {0}", osVer);
        }
        //</editor-fold>
    }
    
    public static OS get() {
        return system;
    }
    
    public static boolean isWindows() {
        return system == Windows;
    }
    
    public static boolean isMac() {
        return system == Mac;
    }
    
    public static boolean isLinux() {
        return system == Linux;
    }
    
    public static class WindowToolkit {
    
        public static String projectName;

        private static String windowClass;
        
        public static String getWindowClass() {
            return WindowToolkit.windowClass;
        }
        
        /**
         * Doesn't seem to work all the time (under Linux)
         * @param windowClass 
         */
        public static void setWindowClass(String windowClass) {
            WindowToolkit.windowClass = windowClass;
            
            if(OS.isLinux()) {
                System.setProperty("jayatana.startupWMClass", windowClass);
//                boolean force = "Unity".equals(System.getenv("XDG_CURRENT_DESKTOP")); // UBUNTU_MENUPROXY=libappmenu.so
//                if(force) {
//                    System.setProperty("jayatana.force", "true");
//                }
            }
            
            try {
                Toolkit xToolkit = Toolkit.getDefaultToolkit();
                Field awtAppClassNameField = xToolkit.getClass().getDeclaredField("awtAppClassName");
                awtAppClassNameField.setAccessible(true);
                awtAppClassNameField.set(xToolkit, windowClass);
            } catch(Exception ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
            
            DesktopLauncher.create(windowClass, "/com/timepath/tf2/hudeditor/resources",
                                   new String[]{"Icon.png", "Icon.svg"},
                                   new String[]{projectName, projectName});
        }
    
    }
    
}
