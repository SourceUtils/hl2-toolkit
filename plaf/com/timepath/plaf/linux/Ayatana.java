package com.timepath.plaf.linux;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import org.java.ayatana.ApplicationMenu;
import org.java.ayatana.AyatanaDesktop;

/**
 *
 * @author timepath
 */
public class Ayatana {

    public static boolean setMenuBar(JFrame jFrame, JMenuBar menubar) {
        try {
            if(!AyatanaDesktop.isSupported()) {
                LOG.info("Ayatana: unsupported");
                return false;
            }
            boolean worked = ApplicationMenu.tryInstall(jFrame, menubar);
            LOG.log(Level.INFO, "Ayatana: {0}", worked);
            if(worked) {
                jFrame.setJMenuBar(null);
                return true;
            }
        } catch(UnsupportedClassVersionError e) { // crashes earlier versions of the JVM - particularly old macs
            e.printStackTrace();
        }
        return false;
    }
    private static final Logger LOG = Logger.getLogger(Ayatana.class.getName());

    private Ayatana() {
    }
    
}
