package com.timepath.steam.io.test;

import com.timepath.plaf.x.filechooser.NativeFileChooser;
import com.timepath.steam.io.GCF;
import com.timepath.steam.io.GCF.DirectoryEntry;
import com.timepath.swing.DirectoryTreeCellRenderer;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 *
 * @author timepath
 */
@SuppressWarnings("serial")
public class GCFTest extends javax.swing.JFrame {
    private GCF g;
    private final DefaultTreeModel tree;
    private final DefaultTableModel table;

    /**
     * Creates new form GCFTest
     */
    public GCFTest() {
        initComponents();
        jTree1.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        jTree1.setCellRenderer(new DirectoryTreeCellRenderer());
        tree = (DefaultTreeModel) jTree1.getModel();
        jTable1.setDefaultEditor(Object.class, new CellSelectionListener());
        table = (DefaultTableModel) jTable1.getModel();
    }
    
    private class CellSelectionListener extends DefaultCellEditor {
        
        CellSelectionListener() {
            super(new JTextField());
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            Object val = table.getValueAt(row, 0);
            if(val instanceof DirectoryEntry) {
                directoryChanged((DirectoryEntry) val);
            }
            return null;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPopupMenu1 = new javax.swing.JPopupMenu();
        jPopupMenuItem1 = new javax.swing.JMenuItem();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTree1 = new javax.swing.JTree();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jTextField1 = new javax.swing.JTextField();
        jButton1 = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTable1 = new javax.swing.JTable();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();

        jPopupMenuItem1.setText("Extract");
        jPopupMenuItem1.setEnabled(false);
        jPopupMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jPopupMenuItem1ActionPerformed(evt);
            }
        });
        jPopupMenu1.add(jPopupMenuItem1);

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("GCF Viewer");

        jSplitPane1.setDividerLocation(200);
        jSplitPane1.setContinuousLayout(true);
        jSplitPane1.setOneTouchExpandable(true);

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        jTree1.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        jTree1.setRootVisible(false);
        jTree1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTree1MouseClicked(evt);
            }
        });
        jTree1.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                directoryChanged(evt);
            }
        });
        jScrollPane2.setViewportView(jTree1);

        jSplitPane1.setLeftComponent(jScrollPane2);

        jPanel1.setLayout(new java.awt.BorderLayout());

        jPanel2.setLayout(new java.awt.BorderLayout());

        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });
        jPanel2.add(jTextField1, java.awt.BorderLayout.CENTER);

        jButton1.setText("Search");
        jButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton1ActionPerformed(evt);
            }
        });
        jPanel2.add(jButton1, java.awt.BorderLayout.EAST);

        jPanel1.add(jPanel2, java.awt.BorderLayout.NORTH);

        jTable1.setAutoCreateRowSorter(true);
        jTable1.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Name", "Size", "Attributes", "Path"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.Integer.class, java.lang.Object.class, java.lang.Object.class
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }
        });
        jTable1.setFillsViewportHeight(true);
        jTable1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jTable1MouseClicked(evt);
            }
        });
        jScrollPane3.setViewportView(jTable1);
        jTable1.getColumnModel().getSelectionModel().setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        jPanel1.add(jScrollPane3, java.awt.BorderLayout.CENTER);

        jSplitPane1.setRightComponent(jPanel1);

        getContentPane().add(jSplitPane1, java.awt.BorderLayout.CENTER);

        jMenu1.setText("File");

        jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem1.setText("Open");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                open(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void open(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_open
        File f = new NativeFileChooser(this, "Open GCF", null).choose(false, false);
        if(f == null) {
            LOG.info("File is null");
            return;
        }
        g = GCF.load(f);
        if(g == null) {
            LOG.log(Level.WARNING, "Unable to load {0}", f);
            return;
        }
        ((DefaultMutableTreeNode) tree.getRoot()).removeAllChildren();
        DefaultMutableTreeNode gcf = new DefaultMutableTreeNode(g);
        DefaultMutableTreeNode direct = new DefaultMutableTreeNode(g.directoryEntries[0]);
        tree.insertNodeInto(direct, gcf, 0);
        g.analyze(direct, false);
        tree.insertNodeInto(gcf, (MutableTreeNode) tree.getRoot(), tree.getChildCount(tree.getRoot()));
        tree.reload();
    }//GEN-LAST:event_open
    
    private void directoryChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_directoryChanged
        TreePath selection = evt.getNewLeadSelectionPath();
        if(selection == null) {
            table.setRowCount(0);
            return;
        }
        Object node = selection.getLastPathComponent();
        if(!(node instanceof DefaultMutableTreeNode)) {
            return;
        }
        Object obj = ((DefaultMutableTreeNode) node).getUserObject();
        if(!(obj instanceof DirectoryEntry)) {
            return;
        }
        directoryChanged((DirectoryEntry) obj);
    }//GEN-LAST:event_directoryChanged

    private ArrayList<DirectoryEntry> toExtract = new ArrayList<DirectoryEntry>();
    
    private void extractablesUpdated() {
        jPopupMenuItem1.setEnabled(!toExtract.isEmpty());
    }
    
    private void jPopupMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jPopupMenuItem1ActionPerformed
        File out = NativeFileChooser.choose(this, "Select extraction directory", null, true, true);
        if(out == null) {
            return;
        }
        for(DirectoryEntry e : toExtract) {
            try {
                g.extract(e.index, out);
            } catch (IOException ex) {
                Logger.getLogger(GCFTest.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        LOG.info("Done");
    }//GEN-LAST:event_jPopupMenuItem1ActionPerformed

    private void jTree1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTree1MouseClicked
        if(SwingUtilities.isRightMouseButton(evt)) {
            TreePath clicked = jTree1.getPathForLocation(evt.getX(), evt.getY());
            if(clicked == null) {
                return;
            }
            Object obj = clicked.getLastPathComponent();
            if(obj instanceof DefaultMutableTreeNode) {
                obj = ((DefaultMutableTreeNode) obj).getUserObject();
            }
            if(jTree1.getSelectionPaths() == null || !Arrays.asList(jTree1.getSelectionPaths()).contains(clicked)) {
                jTree1.setSelectionPath(clicked);
            }
            toExtract.clear();
            TreePath[] paths = jTree1.getSelectionPaths();
            for(TreePath p : paths) {
                if(!(p.getLastPathComponent() instanceof DefaultMutableTreeNode)) {
                    return;
                }
                Object userObject = ((DefaultMutableTreeNode)p.getLastPathComponent()).getUserObject();
                if(userObject instanceof DirectoryEntry) {
                    toExtract.add((DirectoryEntry) userObject);
                }
            }
            extractablesUpdated();
            jPopupMenu1.show(jTree1, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_jTree1MouseClicked

    private void jTable1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jTable1MouseClicked
        if(SwingUtilities.isRightMouseButton(evt)) {
            int row = jTable1.rowAtPoint(evt.getPoint());
            if(row == -1) {
                return;
            }
            Object obj = table.getValueAt(row, 0);
            int[] selectedRows = jTable1.getSelectedRows();
            Arrays.sort(selectedRows);
            if(Arrays.binarySearch(selectedRows, row) < 0) {
                jTable1.setRowSelectionInterval(row, row);
            }
            toExtract.clear();
            int[] selected = jTable1.getSelectedRows();
            for(int r : selected) {
                Object userObject = table.getValueAt(r, 0);
                if(userObject instanceof DirectoryEntry) {
                    toExtract.add((DirectoryEntry) userObject);
                }
            }
            extractablesUpdated();
            jPopupMenu1.show(jTable1, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_jTable1MouseClicked

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        search();
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton1ActionPerformed
        search();
    }//GEN-LAST:event_jButton1ActionPerformed
    
    private void search() {
        jTree1.setSelectionPath(null);
        ArrayList<DirectoryEntry> children = g.find(jTextField1.getText());
        table.setRowCount(0);
        for(int i = 0; i < children.size(); i++) {
            DirectoryEntry c = children.get(i);
            if(!c.isDirectory()) {
                table.addRow(new Object[]{c, c.itemSize, c.attributes, c.getPath()});
            }
        }
    }
    
    private void directoryChanged(DirectoryEntry dir) {
        if(!dir.isDirectory()) {
            return;
        }
        DirectoryEntry[] children = g.getImmediateChildren(dir);
        table.setRowCount(0);
        for(int i = 0; i < children.length; i++) {
            DirectoryEntry c = children[i];
            if(!c.isDirectory()) {
                table.addRow(new Object[]{c, c.itemSize, c.attributes, c.getPath()});
            }
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String... args) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(GCFTest.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(GCFTest.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(GCFTest.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(GCFTest.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new GCFTest().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButton1;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPopupMenu jPopupMenu1;
    private javax.swing.JMenuItem jPopupMenuItem1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTable jTable1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTree jTree1;
    // End of variables declaration//GEN-END:variables
    private static final Logger LOG = Logger.getLogger(GCFTest.class.getName());
}