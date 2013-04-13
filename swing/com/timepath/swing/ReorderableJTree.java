package com.timepath.swing;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import static javax.swing.TransferHandler.MOVE;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author timepath
 */
public class ReorderableJTree extends JTree {

    private static final Logger LOG = Logger.getLogger(ReorderableJTree.class.getName());

    private static final long serialVersionUID = 1L;

    public ReorderableJTree() {
        setDragEnabled(true);
        setDropMode(DropMode.ON_OR_INSERT);
        setTransferHandler(new TreeTransferHandler());
    }

    private class TreeTransferHandler extends TransferHandler {

        private static final long serialVersionUID = 1L;

        private DataFlavor nodesFlavor;

        private DataFlavor[] flavors;

        private DefaultMutableTreeNode[] nodesToRemove;
        
        //<editor-fold defaultstate="collapsed" desc="Helpers">
        private boolean haveCompleteNode(JTree tree) {
            int[] selRows = tree.getSelectionRows();
            TreePath path = tree.getPathForRow(selRows[0]);
            DefaultMutableTreeNode first = (DefaultMutableTreeNode) path.getLastPathComponent();
            int childCount = first.getChildCount();
            // first has children and no children are selected.
            if(childCount > 0 && selRows.length == 1) {
                return false;
            }
            // first may have children.
            for(int i = 1; i < selRows.length; i++) {
                path = tree.getPathForRow(selRows[i]);
                DefaultMutableTreeNode next = (DefaultMutableTreeNode) path.getLastPathComponent();
                if(first.isNodeChild(next)) {
                    // Found a child of first.
                    if(childCount > selRows.length - 1) {
                        // Not all children of first are selected.
                        return false;
                    }
                }
            }
            return true;
        }
        
        /**
         * Defensive copy used in createTransferable.
         */
        private DefaultMutableTreeNode copy(TreeNode node) {
            return new DefaultMutableTreeNode(node);
        }
        //</editor-fold>
        
        TreeTransferHandler() {
            try {
                String mimeType = DataFlavor.javaJVMLocalObjectMimeType
                                  + ";class=\""
                                  + javax.swing.tree.DefaultMutableTreeNode[].class.getName()
                                  + "\"";
                nodesFlavor = new DataFlavor(mimeType);
                flavors = new DataFlavor[]{nodesFlavor};
            } catch(ClassNotFoundException e) {
                LOG.log(Level.SEVERE, "ClassNotFound: {0}", e.getMessage());
            }
        }
        
        //<editor-fold defaultstate="collapsed" desc="Export">
        @Override
        public int getSourceActions(JComponent c) {
            return TransferHandler.COPY_OR_MOVE;
        }
        
        @Override
        protected Transferable createTransferable(JComponent c) {
            JTree tree = (JTree) c;
            TreePath[] paths = tree.getSelectionPaths();
            if(paths != null) {
                // Make up a node array of copies for transfer and
                // another for/of the nodes that will be removed in
                // exportDone after a successful drop.
                List<DefaultMutableTreeNode> copies = new ArrayList<DefaultMutableTreeNode>();
                List<DefaultMutableTreeNode> toRemove = new ArrayList<DefaultMutableTreeNode>();
                DefaultMutableTreeNode node = (DefaultMutableTreeNode) paths[0].getLastPathComponent();
                DefaultMutableTreeNode copy = copy(node);
                copies.add(copy);
                toRemove.add(node);
                for(int i = 1; i < paths.length; i++) {
                    DefaultMutableTreeNode next = (DefaultMutableTreeNode) paths[i].getLastPathComponent();
                    // Do not allow higher level nodes to be added to list.
                    if(next.getLevel() < node.getLevel()) {
                        break;
                    } else if(next.getLevel() > node.getLevel()) {  // child node
                        copy.add(copy(next));
                        // node already contains child
                    } else {                                        // sibling
                        copies.add(copy(next));
                        toRemove.add(next);
                    }
                }
                DefaultMutableTreeNode[] nodes = copies.toArray(new DefaultMutableTreeNode[copies.size()]);
                nodesToRemove = toRemove.toArray(new DefaultMutableTreeNode[toRemove.size()]);
                return new NodesTransferable(nodes);
            }
            return null;
        }
        
        @Override
        protected void exportDone(JComponent source, Transferable data, int action) {
            if((action & MOVE) == MOVE) {
                JTree tree = (JTree) source;
                DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
                // Remove nodes saved in nodesToRemove in createTransferable.
                for(int i = 0; i < nodesToRemove.length; i++) {
                    model.removeNodeFromParent(nodesToRemove[i]);
                }
            }
        }
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="Import">
        @Override
        public boolean canImport(TransferHandler.TransferSupport support) {
            if(!support.isDataFlavorSupported(nodesFlavor)) {
                return false;
            }
            
            support.setShowDropLocation(true);
            
            // Get drop location info
            JTree tree = (JTree) support.getComponent();
//            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
//            int childIndex = dl.getChildIndex();
            TreePath dest = dl.getPath();
            DefaultMutableTreeNode target = (DefaultMutableTreeNode) dest.getLastPathComponent();
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dest.getLastPathComponent();
            
            // Convert nodes to usable format
            DefaultMutableTreeNode[] clodedNodes;
            try {
                clodedNodes = (DefaultMutableTreeNode[]) support.getTransferable().getTransferData(nodesFlavor);
            } catch(Exception ex) {
                return false;
            }
            DefaultMutableTreeNode[] nodes = new DefaultMutableTreeNode[clodedNodes.length];
            for(int i = 0; i < clodedNodes.length; i++) {
                nodes[i] = (DefaultMutableTreeNode) clodedNodes[i].getUserObject();
            }
            
            // Sanity check
            
            // Do not allow MOVE-action drops if a non-leaf node is
            // selected unless all of its children are also selected.
            if(support.getDropAction() == MOVE && !haveCompleteNode(tree)) {
//                return false;
            }
            
            for(int i = 0; i < nodes.length; i++) {
                // Do not allow a drop on the drag source selections
                if(nodes[i] == parent) {
                    return false;
                }
                // Do not allow a node to be copied or moved onto itself
                if(nodes[i] == target) {
                    return false;
                }
                // Do not allow a node to be copied or moved onto one of its descendants
                if(nodes[i].isNodeDescendant(target)) {
                    return false;
                }
            }
            return true;
        }
        
        @Override
        public boolean importData(TransferHandler.TransferSupport support) {
            if(!support.isDrop()) {
                return false; // Pasting.
            }
            if(!canImport(support)) {
                return false;
            }
            
            // Get drop location info.
            JTree tree = (JTree) support.getComponent();
            DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
            JTree.DropLocation dl = (JTree.DropLocation) support.getDropLocation();
            int childIndex = dl.getChildIndex();
            TreePath dest = dl.getPath();
            
            // Extract transfer data.
            DefaultMutableTreeNode[] nodes = null;
            try {
                nodes = (DefaultMutableTreeNode[]) support.getTransferable().getTransferData(nodesFlavor);
            } catch(UnsupportedFlavorException ufe) {
                LOG.log(Level.WARNING, "UnsupportedFlavor: {0}", ufe.getMessage());
            } catch(java.io.IOException ioe) {
                LOG.log(Level.WARNING, "I/O error: {0}", ioe.getMessage());
            }
            if(nodes == null) {
                return false;
            }
            
            // Do stuff with data.
            DefaultMutableTreeNode parent = (DefaultMutableTreeNode) dest.getLastPathComponent();
            
            // Configure for drop mode.
            int index = childIndex;    // DropMode.INSERT
            if(childIndex == -1) {     // DropMode.ON
                index = parent.getChildCount(); // End of list
            }
            
            // Add data to model.
            for(int i = 0; i < nodes.length; i++) {
                model.insertNodeInto(nodes[i], parent, index++);
            }
            return true;
        }
        //</editor-fold>

        @Override
        public String toString() {
            return getClass().getName();
        }

        private class NodesTransferable implements Transferable {

            private DefaultMutableTreeNode[] nodes;

            NodesTransferable(DefaultMutableTreeNode[] nodes) {
                this.nodes = nodes;
            }

            public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
                if(!isDataFlavorSupported(flavor)) {
                    throw new UnsupportedFlavorException(flavor);
                }
                return nodes;
            }

            public DataFlavor[] getTransferDataFlavors() {
                return flavors;
            }

            public boolean isDataFlavorSupported(DataFlavor flavor) {
                return nodesFlavor.equals(flavor);
            }
        }

    }

}