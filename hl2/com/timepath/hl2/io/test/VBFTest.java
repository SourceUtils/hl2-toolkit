package com.timepath.hl2.io.test;

import com.timepath.hl2.io.VBF;
import com.timepath.hl2.io.VBF.BitmapGlyph;
import com.timepath.hl2.io.VTF;
import com.timepath.hl2.io.swing.VBFCanvas;
import com.timepath.plaf.x.filechooser.NativeFileChooser;
import java.awt.Rectangle;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author timepath
 */
public class VBFTest extends javax.swing.JFrame {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(VBFTest.class.getName());

    /**
     * Creates new form VBFTest
     */
    public VBFTest() {
        initComponents();
        //<editor-fold defaultstate="collapsed" desc="Spinners">
        jSpinner1.addChangeListener(new ChangeListener() {
            int old = 0;
            @Override
            public void stateChanged(ChangeEvent e) {
                if(b == null) {
                    jSpinner1.setValue(0);
                    return;
                }
                int current = ((Number)jSpinner1.getValue()).intValue();
                g.getBounds().x = current;
                vBFCanvas1.repaint(Math.min(old, current), ((Number)jSpinner2.getValue()).intValue(), Math.max(old, current) + ((Number)jSpinner7.getValue()).intValue(), ((Number)jSpinner8.getValue()).intValue());
                vBFCanvas1.repaint();
                old = current;
                ((SpinnerNumberModel)jSpinner7.getModel()).setMaximum(b.getWidth() - ((Number)jSpinner1.getValue()).intValue());
            }
        });
        jSpinner7.addChangeListener(new ChangeListener() {
            int old = 0;
            @Override
            public void stateChanged(ChangeEvent e) {
                if(b == null) {
                    jSpinner7.setValue(0);
                    return;
                }
                int current = ((Number)jSpinner7.getValue()).intValue();
                g.getBounds().width = current;
                vBFCanvas1.repaint(((Number)jSpinner1.getValue()).intValue(), ((Number)jSpinner2.getValue()).intValue(), Math.max(old, current), ((Number)jSpinner8.getValue()).intValue());
                vBFCanvas1.repaint();
                old = current;
                ((SpinnerNumberModel)jSpinner1.getModel()).setMaximum(b.getWidth() - ((Number)jSpinner7.getValue()).intValue());
            }
        });
        jSpinner2.addChangeListener(new ChangeListener() {
            int old = 0;
            @Override
            public void stateChanged(ChangeEvent e) {
                if(b == null) {
                    jSpinner2.setValue(0);
                    return;
                }
                int current = ((Number)jSpinner2.getValue()).intValue();
                g.getBounds().y = current;
                vBFCanvas1.repaint(((Number)jSpinner1.getValue()).intValue(), Math.min(old, current), ((Number)jSpinner7.getValue()).intValue(), Math.max(old, current) + ((Number)jSpinner8.getValue()).intValue());
                vBFCanvas1.repaint();
                old = current;
                ((SpinnerNumberModel)jSpinner8.getModel()).setMaximum(b.getHeight() - ((Number)jSpinner2.getValue()).intValue());
            }
        });
        jSpinner8.addChangeListener(new ChangeListener() {
            int old = 0;
            @Override
            public void stateChanged(ChangeEvent e) {
                if(b == null) {
                    jSpinner8.setValue(0);
                    return;
                }
                int current = ((Number)jSpinner8.getValue()).intValue();
                g.getBounds().height = current;
                vBFCanvas1.repaint(((Number)jSpinner1.getValue()).intValue(), ((Number)jSpinner2.getValue()).intValue(), ((Number)jSpinner7.getValue()).intValue(), Math.max(old, current));
                vBFCanvas1.repaint();
                old = current;
                ((SpinnerNumberModel)jSpinner2.getModel()).setMaximum(b.getHeight() - ((Number)jSpinner8.getValue()).intValue());
            }
        });
        //</editor-fold>
    }

    private VBF b;

    private BitmapGlyph g;

    private void load(String f) throws IOException {
        if(f == null) {
            return;
        }
        VBFCanvas p = this.vBFCanvas1;

        b = VBF.load(new FileInputStream(f + ".vbf"));
        p.setVBF(b);

        DefaultTreeModel model = (DefaultTreeModel) this.jTree1.getModel();
        for(int g = 0; g < b.getGlyphs().length; g++) {
            DefaultMutableTreeNode child = new DefaultMutableTreeNode(b.getGlyphs()[g]);
            for(int i = 0; i < b.getTable().length; i++) {
                int glyphIndex = b.getTable()[i];
                if(glyphIndex != g) {
                    continue;
                }
                DefaultMutableTreeNode sub = new DefaultMutableTreeNode((char) i);
                model.insertNodeInto(sub, child, model.getChildCount(child));
            }
            model.insertNodeInto(child, (MutableTreeNode) model.getRoot(), model.getChildCount(model.getRoot()));
        }
        model.reload();

        VTF t = VTF.load(new File(f + ".vtf"));
        p.setVTF(t);
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTree1 = new javax.swing.JTree();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jSpinner1 = new javax.swing.JSpinner();
        jSpinner2 = new javax.swing.JSpinner();
        jPanel5 = new javax.swing.JPanel();
        jSpinner7 = new javax.swing.JSpinner();
        jSpinner8 = new javax.swing.JSpinner();
        jScrollPane1 = new javax.swing.JScrollPane();
        vBFCanvas1 = new com.timepath.hl2.io.swing.VBFCanvas();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Bitmap Font Editor");

        jSplitPane1.setDividerSize(2);
        jSplitPane1.setContinuousLayout(true);
        jSplitPane1.setEnabled(false);

        jScrollPane3.setBorder(null);

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        jTree1.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        jTree1.setRootVisible(false);
        jTree1.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                jTree1ValueChanged(evt);
            }
        });
        jScrollPane3.setViewportView(jTree1);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Position"));

        jSpinner1.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));

        jSpinner2.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSpinner1, javax.swing.GroupLayout.DEFAULT_SIZE, 61, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(jSpinner2, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSpinner1, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jSpinner2, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Dimensions"));

        jSpinner7.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));

        jSpinner8.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jSpinner7, javax.swing.GroupLayout.PREFERRED_SIZE, 61, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jSpinner8, javax.swing.GroupLayout.DEFAULT_SIZE, 61, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jSpinner7, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jSpinner8, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(10, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addComponent(jScrollPane3)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, 177, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jSplitPane1.setLeftComponent(jPanel1);

        jScrollPane1.setBorder(null);

        vBFCanvas1.setLayout(new java.awt.BorderLayout());
        jScrollPane1.setViewportView(vBFCanvas1);

        jSplitPane1.setRightComponent(jScrollPane1);

        jMenu1.setText("File");

        jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem1.setMnemonic('O');
        jMenuItem1.setText("Open");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem1ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuItem2.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem2.setMnemonic('S');
        jMenuItem2.setText("Save");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMenuItem2ActionPerformed(evt);
            }
        });
        jMenu1.add(jMenuItem2);

        jMenuBar1.add(jMenu1);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.Alignment.TRAILING)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
        File f = NativeFileChooser.choose(this, "Select vbf", null, false, false);
        if(f == null) {
            return;
        }
        try {
            load(f.getPath().replace(".vbf", ""));
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItem1ActionPerformed

    private void jTree1ValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_jTree1ValueChanged
        TreePath selection = evt.getNewLeadSelectionPath();
        if(selection == null) {
            return;
        }
        Object node = selection.getLastPathComponent();
        if(!(node instanceof DefaultMutableTreeNode)) {
            return;
        }
        Object obj = ((DefaultMutableTreeNode) node).getUserObject();
        if(!(obj instanceof BitmapGlyph)) {
            return;
        }
        g = (BitmapGlyph) obj;
        
        Rectangle r = g.getBounds();
        jSpinner1.setValue(r.x);
        jSpinner2.setValue(r.y);
        jSpinner7.setValue(r.width);
        jSpinner8.setValue(r.height);
    }//GEN-LAST:event_jTree1ValueChanged

    private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
        File f = NativeFileChooser.choose(this, "Select save location", null, false, true);
        if(f == null) {
            return;
        }
        try {
            b.save(f);
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_jMenuItem2ActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String... args) {
        /*
         * Set the Nimbus look and feel
         */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the default look and
         * feel.
         * For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for(javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch(ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(VBFTest.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch(InstantiationException ex) {
            java.util.logging.Logger.getLogger(VBFTest.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch(IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(VBFTest.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch(javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(VBFTest.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /*
         * Create and display the form
         */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new VBFTest().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JSpinner jSpinner1;
    private javax.swing.JSpinner jSpinner2;
    private javax.swing.JSpinner jSpinner5;
    private javax.swing.JSpinner jSpinner6;
    private javax.swing.JSpinner jSpinner7;
    private javax.swing.JSpinner jSpinner8;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTree jTree1;
    private com.timepath.hl2.io.swing.VBFCanvas vBFCanvas1;
    // End of variables declaration//GEN-END:variables
}
