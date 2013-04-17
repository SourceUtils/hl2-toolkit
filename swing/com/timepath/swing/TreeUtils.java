package com.timepath.swing;

import com.timepath.steam.io.BVDF;
import java.util.Enumeration;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author timepath
 */
public class TreeUtils {
    
    public static void expand(JTree tree) {
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) tree.getModel().getRoot();
        Enumeration e = root.breadthFirstEnumeration();
        while(e.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.nextElement();
            if(node.isLeaf()) {
                continue;
            }
            int row = tree.getRowForPath(new TreePath(node.getPath()));
            tree.expandRow(row);
        }
    }

    /**
     * Sometimes this doesn't work. TODO: find out why
     * @param source
     * @param dest 
     */
    public static void moveChildren(DefaultMutableTreeNode source, DefaultMutableTreeNode dest) {
        boolean safe = true;
        if(safe) {
            dest.add(source);
        } else {
            @SuppressWarnings("unchecked")
            Enumeration<DefaultMutableTreeNode> e = source.children();
            while(e.hasMoreElements()) {
                DefaultMutableTreeNode node = e.nextElement();
                node.removeFromParent();
                dest.add(node);
            }
        }
    }
    
}
