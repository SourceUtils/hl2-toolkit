/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.timepath.plaf.linux;

import com.timepath.plaf.OS;
import java.awt.Toolkit;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class WindowToolkit {

    private static String windowClass;

    public static String getWindowClass() {
        return WindowToolkit.windowClass;
    }

    /**
     * Doesn't seem to work all the time
     *
     * @param windowClass
     */
    public static void setWindowClass(String windowClass) {
        WindowToolkit.windowClass = windowClass;

        System.setProperty("jayatana.startupWMClass", windowClass);
//                boolean force = "Unity".equals(System.getenv("XDG_CURRENT_DESKTOP")); // UBUNTU_MENUPROXY=libappmenu.so
//                if(force) {
//                    System.setProperty("jayatana.force", "true");
//                }
        try {
            Toolkit xToolkit = Toolkit.getDefaultToolkit();
            Field awtAppClassNameField = xToolkit.getClass().getDeclaredField("awtAppClassName");
            awtAppClassNameField.setAccessible(true);
            awtAppClassNameField.set(xToolkit, windowClass);
        } catch(Exception ex) {
            LOG.log(Level.WARNING, null, ex);
        }

        DesktopLauncher.create(windowClass, "/com/timepath/tf2/hudeditor/resources",
                               new String[]{"Icon.png", "Icon.svg"},
                               new String[]{windowClass, windowClass});
    }

    private WindowToolkit() {
    }

    private static final Logger LOG = Logger.getLogger(WindowToolkit.class.getName());

}