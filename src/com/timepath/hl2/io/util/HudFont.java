package com.timepath.hl2.io.util;

import com.timepath.steam.io.util.Property;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class HudFont {

    public HudFont() {
    }

    private String name;

    private String _name;

    private int tall;

    private boolean aa;

    public HudFont(String font, Element node) {
        this.name = font;
        for(int i = 0; i < node.getProps().size(); i++) {
            Property p = node.getProps().get(i);
            String key = p.getKey().replaceAll("\"", "").toLowerCase();
            String val = p.getValue().replaceAll("\"", "").toLowerCase();
            if("name".equals(key)) {
                this._name = val;
            } else if("tall".equals(key)) {
                this.tall = Integer.parseInt(val);
            } else if("antialias".equals(key)) {
                this.aa = Integer.parseInt(val) == 1;
            }
        }
    }

    public Font getFont() {
        int screenRes = Toolkit.getDefaultToolkit().getScreenResolution();
        int fontSize = (int) Math.round(tall * screenRes / 72.0);

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        String[] fontFamilies = ge.getAvailableFontFamilyNames();
        if(Arrays.asList(fontFamilies).contains(_name)) { // System font
            return new Font(_name, Font.PLAIN, fontSize);
        }

        Font f1 = null;
        try {
            LOG.log(Level.INFO, "Loading font: {0}... ({1})", new Object[]{name, _name});
            f1 = fontFileForName(_name);
            if(f1 == null) {
                return null;
            }
            ge.registerFont(f1); // for some reason, this works but the bottom return does not
            return new Font(name, Font.PLAIN, fontSize);
        } catch(Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        LOG.log(Level.INFO, "Loaded {0}", name);
        return f1.deriveFont(fontSize);
    }

    public static Font fontFileForName(String name) throws Exception {
        File[] files = new File("/home/andrew/TF2 HUDS/frankenhudr47/resource/").listFiles(new FilenameFilter() { // XXX: hardcoded
            public boolean accept(File file, String string) {
                return string.endsWith(".ttf");
            }
        });
        if(files != null) {
            for(int t = 0; t < files.length; t++) {
                Font f = Font.createFont(Font.TRUETYPE_FONT, files[t]);
                //            System.out.println(f.getFamily().toLowerCase());
                if(f.getFamily().toLowerCase().equals(name.toLowerCase())) {
                    LOG.log(Level.INFO, "Found font for {0}", name);
                    return f;
                }
            }
        }
        return null;
    }

    private static final Logger LOG = Logger.getLogger(HudFont.class.getName());
}
