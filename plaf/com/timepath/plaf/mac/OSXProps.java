package com.timepath.plaf.mac;

import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class OSXProps {

    public static void metal(boolean flag) {
        System.setProperty("apple.awt.brushMetalLook", Boolean.toString(flag));
    }

    public static void name(String str) {
        System.setProperty("com.apple.mrj.application.apple.menu.about.name", str);
    }

    public static void growBox(boolean flag) {
        System.setProperty("apple.awt.showGrowBox", Boolean.toString(flag));
    }

    public static void growBoxIntrudes(boolean flag) {
        System.setProperty("com.apple.mrj.application.growbox.intrudes", Boolean.toString(flag));
    }

    public static void quartz(boolean flag) {
        System.setProperty("apple.awt.graphics.EnableQ2DX", Boolean.toString(flag));
    }

    public static void globalMenu(boolean flag) {
        System.setProperty("apple.laf.useScreenMenuBar", Boolean.toString(flag));
    }

    public static void smallTabs(boolean flag) {
        System.setProperty("com.apple.macos.smallTabs", Boolean.toString(flag));
    }

    public static void fileDialogPackages(boolean flag) {
        System.setProperty("com.apple.macos.use-file-dialog-packages", Boolean.toString(flag));
    }

    public static void liveResize(boolean flag) {
        System.setProperty("com.apple.mrj.application.live-resize", Boolean.toString(flag));
    }

    public static void fileDialogDirectoryMode(boolean flag) {
        System.setProperty("apple.awt.fileDialogForDirectories", Boolean.toString(flag));
    }

    private static final Logger LOG = Logger.getLogger(OSXProps.class.getName());

    private OSXProps() {
    }
}
