package com.timepath.steam.io;

import com.timepath.hl2.io.util.Element;
import com.timepath.hl2.io.util.HudFont;
import com.timepath.hl2.io.util.Property;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 *
 * TODO: Threading. This class can probably be executed as a thread.
 * If there are multiple values with platform tags, all the values become the last loaded value tag, but only if the variable is recognized
 *
 * Some tags:
 * $WINDOWS
 * $WIN32
 * $X360
 * $POSIX
 * $OSX
 * $LINUX
 *
 * @author timepath
 */
public class RES extends VDF {

    private static final Logger LOG = Logger.getLogger(RES.class.getName());

    private String hudFolder;

    public RES(String hudFolder) {
        this.hudFolder = hudFolder;
    }

    public static HashMap<String, HudFont> fonts = new HashMap<String, HudFont>();

    // TODO: Special exceptions for *scheme.res, hudlayout.res,
    public static void analyze(final File file, final DefaultMutableTreeNode top) {
        if(file.isDirectory()) {
            return;
        }

        Scanner s = null;
        try {
            RandomAccessFile rf = new RandomAccessFile(file.getPath(), "r");
            s = new Scanner(rf.getChannel());
            processAnalyze(s, top, new ArrayList<Property>(), file);
            if(file.getName().equalsIgnoreCase("ClientScheme.res")) {
                clientScheme(top);
            }
        } catch(FileNotFoundException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } finally {
            if(s != null) {
                s.close();
            }
        }
    }

    private static void clientScheme(DefaultMutableTreeNode props) {
        LOG.info("Found clientscheme");
        TreeNode fontNode = null;
        for(int i = 0; i < props.getChildCount(); i++) {
            DefaultMutableTreeNode c = ((DefaultMutableTreeNode)props.getChildAt(0).getChildAt(i));
            LOG.log(Level.INFO, "Checking: {0}", ((Element)c.getUserObject()).getName());
            if(((Element)c.getUserObject()).getName().equalsIgnoreCase("Font")) {
                fontNode = c;
                LOG.log(Level.INFO, "Found font node: {0}", fontNode);
                break;
            }
        }
        if(fontNode == null) {
            return;
        }
        for(int i = 0; i < fontNode.getChildCount(); i++) {
            TreeNode font = fontNode.getChildAt(i);
            TreeNode detailFont = font.getChildAt(0); // XXX: hardcoded detail level
            Element fontElement = (Element) ((DefaultMutableTreeNode) detailFont).getUserObject();
            String fontName = font.toString().replaceAll("\"", ""); // Some use quotes.. oh well
            fonts.put(fontName, new HudFont(fontName, fontElement));
        }
        LOG.info("Loaded clientscheme");
    }
    
}