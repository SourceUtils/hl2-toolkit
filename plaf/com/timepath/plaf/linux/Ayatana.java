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
    
    private static final Logger LOG = Logger.getLogger(Ayatana.class.getName());
    
    public static boolean installMenu(JFrame frame, JMenuBar jmb) {
        if(!AyatanaDesktop.isSupported()) {
            LOG.info("Ayatana: Unsupported");
            return false;
        }
        try {
            boolean worked = ApplicationMenu.tryInstall(frame, jmb);
            LOG.log(Level.INFO, "Ayatana: {0}", worked);
            if(worked) {
                frame.setJMenuBar(null);
                return true;
            }
        } catch(UnsupportedClassVersionError e) { // crashes earlier versions of the JVM - particularly old macs
            LOG.info("Ayatana: JVM not recent enough");
        }
        LOG.info("Ayatana: Failed");
        return false;
    }
    
}
