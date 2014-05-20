package com.timepath.hl2.io;

import com.timepath.hl2.io.util.HudFont;
import com.timepath.steam.io.VDF1;
import com.timepath.steam.io.util.VDFNode1;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * If there are multiple values with platform tags, all the values become the last loaded value
 * tag, but only if the variable is recognized
 * Some tags:
 * $WINDOWS
 * $WIN32
 * $X360
 * $POSIX
 * $OSX
 * $LINUX
 *
 * @author TimePath
 * @see <a>https://code.google.com/p/hl2sb-src/source/browse/#svn%2Ftrunk%2Fsrc%2Fgame%2Fclient%2Fgame_controls</a>
 */
public class RES extends VDF1 {

    public static final  Map<String, HudFont> fonts = new HashMap<>(0);
    private static final Logger               LOG   = Logger.getLogger(RES.class.getName());

    public RES() {}

    /**
     * TODO
     *
     * @param props
     */
    private static void clientScheme(DefaultMutableTreeNode props) {
        LOG.info("Found clientscheme");
        if(!( props instanceof VDFNode1 )) {
            LOG.log(Level.WARNING, "TreeNode not instanceof VDFNode", props);
            return;
        }
        VDFNode1 root = ( (VDFNode1) props ).get("Scheme").get("Fonts");
        for(int i = 0; i < root.getChildCount(); i++) {
            TreeNode node = root.getChildAt(i);
            if(!( node instanceof VDFNode1 )) {
                continue;
            }
            VDFNode1 fontNode = (VDFNode1) node;
            VDFNode1 detailNode = null;
            for(int j = 0; j < fontNode.getChildCount(); j++) {
                TreeNode detail = fontNode.getChildAt(j);
                if(!( detail instanceof VDFNode1 )) {
                    continue;
                }
                detailNode = (VDFNode1) detail;
                break; // XXX: hardcoded detail level (the first one)
            }
            if(detailNode == null) {
                continue;
            }
            String fontName = fontNode.getKey();
            //            fonts.put(fontName, new HudFont(fontName, detailFont));
            LOG.log(Level.INFO, "TODO: Load font {0}", fontName);
        }
        LOG.info("Loaded clientscheme");
    }

    @Override
    public void readExternal(InputStream in, String encoding) {
        super.readExternal(in, encoding);
        //        clientScheme(root);
    }
}
