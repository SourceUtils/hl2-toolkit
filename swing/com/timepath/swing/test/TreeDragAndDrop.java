package com.timepath.swing.test;

import com.timepath.swing.ReorderableJTree;
import com.timepath.swing.TreeUtils;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.tree.TreeSelectionModel;

public class TreeDragAndDrop {

    public static void main(String[] args) {
        JFrame f = new JFrame("Draggable Tree Test");
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        
        JTree tree = new ReorderableJTree();
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        JTree tree2 = new ReorderableJTree();
        tree2.setModel(tree.getModel());
        tree2.getSelectionModel().setSelectionMode(TreeSelectionModel.CONTIGUOUS_TREE_SELECTION);
        
        TreeUtils.expand(tree);
        TreeUtils.expand(tree2);
        JSplitPane sp = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true, new JScrollPane(tree), new JScrollPane(tree2));
        sp.setDividerLocation(0.5);
        f.add(sp);
        f.setSize(400, 400);
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

}
