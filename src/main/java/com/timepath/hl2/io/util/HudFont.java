package com.timepath.hl2.io.util;

import com.timepath.steam.io.util.Property;

import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class HudFont {

    private static final Logger LOG = Logger.getLogger(HudFont.class.getName());
    private String  name;
    private String  _name;
    private int     tall;
    private boolean aa;

    private HudFont() {
    }

    private HudFont(String font, Element node) {
        name = font;
        for(int i = 0; i < node.getProps().size(); i++) {
            Property p = node.getProps().get(i);
            String key = p.getKey().replaceAll("\"", "").toLowerCase();
            String val = p.getValue().replaceAll("\"", "").toLowerCase();
            switch(key) {
                case "name":
                    _name = val;
                    break;
                case "tall":
                    tall = Integer.parseInt(val);
                    break;
                case "antialias":
                    aa = Integer.parseInt(val) == 1;
                    break;
            }
        }
    }

    public Font getFont() {
        int screenRes = Toolkit.getDefaultToolkit().getScreenResolution();
        int fontSize = (int) Math.round(( tall * screenRes ) / 72.0);
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontFamilies = ge.getAvailableFontFamilyNames();
        if(Arrays.asList(fontFamilies).contains(_name)) { // System font
            return new Font(_name, Font.PLAIN, fontSize);
        }
        Font f1 = null;
        try {
            LOG.log(Level.INFO, "Loading font: {0}... ({1})", new Object[] { name, _name });
            f1 = fontFileForName(_name);
            if(f1 == null) {
                return null;
            }
            ge.registerFont(f1); // For some reason, this works but the bottom return does not
            return new Font(name, Font.PLAIN, fontSize);
        } catch(Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        if(f1 == null) {
            return null;
        }
        LOG.log(Level.INFO, "Loaded {0}", name);
        return f1.deriveFont((float) fontSize);
    }

    private static Font fontFileForName(String name) throws Exception {
        File[] files = new File("").listFiles(new FilenameFilter() { // XXX: hardcoded
                                                  @Override
                                                  public boolean accept(File file, String string) {
                                                      return string.endsWith(".ttf");
                                                  }
                                              }
                                             );
        if(files != null) {
            for(File file : files) {
                Font f = Font.createFont(Font.TRUETYPE_FONT, file);
                //            System.out.println(f.getFamily().toLowerCase());
                if(f.getFamily().toLowerCase().equals(name.toLowerCase())) {
                    LOG.log(Level.INFO, "Found font for {0}", name);
                    return f;
                }
            }
        }
        return null;
    }
}
