package com.timepath.swing;

import java.awt.Component;
import java.util.logging.Logger;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 *
 * @author timepath
 */
@SuppressWarnings("serial")
public class DirectoryTreeCellRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {
        return super.getTreeCellRendererComponent(tree, value, sel, sel, false, row, hasFocus);
    }
    private static final Logger LOG = Logger.getLogger(DirectoryTreeCellRenderer.class.getName());
}