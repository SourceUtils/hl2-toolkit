package com.timepath.hl2.io.test;

import com.timepath.hl2.io.VBF;
import com.timepath.hl2.io.VBF.BitmapGlyph;
import com.timepath.hl2.io.VTF;
import com.timepath.hl2.io.swing.VBFCanvas;
import com.timepath.plaf.x.filechooser.BaseFileChooser;
import com.timepath.plaf.x.filechooser.NativeFileChooser;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DropMode;
import javax.swing.JTree;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.TreeSelectionEvent;
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

    private static class DisplayableCharacter {

        DisplayableCharacter(int i) {
            this.c = (char) i;
        }

        private final char c;

        @Override
        public String toString() {
            Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
            boolean printable = ((!Character.isISOControl(c)) && c != KeyEvent.CHAR_UNDEFINED && block != null && block != Character.UnicodeBlock.SPECIALS);
            if(!printable) {
                return "0x" + (c < 16 ? "0" : "") + Integer.toHexString(c).toUpperCase();
            }
            return Character.toString(c);
        }
    }

    /**
     * Creates new form VBFTest
     */
    public VBFTest() {
        initComponents();

        jTree2.setModel(jTree1.getModel());
        jTree1.setMinMovable(2);
        jTree1.setMaxLevel(1);
        jTree2.setMinMovable(2);
        jTree2.setMaxLevel(1);
        jTree1.setDropMode(DropMode.ON);
        jTree2.setDropMode(DropMode.ON);

        boolean spinners = false;
        //<editor-fold defaultstate="collapsed" desc="Spinners">
        if(spinners) {
            jSpinner1.addChangeListener(new ChangeListener() {
                private int old = 0;

                @Override
                public void stateChanged(ChangeEvent e) {
                    if(data == null) {
                        jSpinner1.setValue(0);
                        return;
                    }
                    int current = ((Number) jSpinner1.getValue()).intValue();
                    currentGlyph.getBounds().x = current;
                    doRepaint(Math.min(old, current), ((Number) jSpinner2.getValue()).intValue(), Math.max(old, current) + ((Number) jSpinner7.getValue()).intValue(), ((Number) jSpinner8.getValue()).intValue());
                    old = current;
                    int wide = data.getWidth();
                    if(image != null) {
                        wide = image.width;
                    }
                    SpinnerNumberModel s = ((SpinnerNumberModel) jSpinner7.getModel());
                    s.setMaximum(wide - ((Number) jSpinner1.getValue()).intValue());
//                if(s.getNumber() > s.g) {
//                    s.setNumber(s.getMaximum());
//                }
                }
            });
            jSpinner7.addChangeListener(new ChangeListener() {
                private int old = 0;

                @Override
                public void stateChanged(ChangeEvent e) {
                    if(data == null) {
                        jSpinner7.setValue(0);
                        return;
                    }
                    int current = ((Number) jSpinner7.getValue()).intValue();
                    currentGlyph.getBounds().width = current;
                    doRepaint(((Number) jSpinner1.getValue()).intValue(), ((Number) jSpinner2.getValue()).intValue(), Math.max(old, current), ((Number) jSpinner8.getValue()).intValue());
                    old = current;
                    int wide = data.getWidth();
                    if(image != null) {
                        wide = image.width;
                    }
                    ((SpinnerNumberModel) jSpinner1.getModel()).setMaximum(wide - ((Number) jSpinner7.getValue()).intValue());
                }
            });
            jSpinner2.addChangeListener(new ChangeListener() {
                private int old = 0;

                @Override
                public void stateChanged(ChangeEvent e) {
                    if(data == null) {
                        jSpinner2.setValue(0);
                        return;
                    }
                    int current = ((Number) jSpinner2.getValue()).intValue();
                    currentGlyph.getBounds().y = current;
                    doRepaint(((Number) jSpinner1.getValue()).intValue(), Math.min(old, current), ((Number) jSpinner7.getValue()).intValue(), Math.max(old, current) + ((Number) jSpinner8.getValue()).intValue());
                    old = current;
                    int high = data.getHeight();
                    if(image != null) {
                        high = image.height;
                    }
                    ((SpinnerNumberModel) jSpinner8.getModel()).setMaximum(high - ((Number) jSpinner2.getValue()).intValue());
                }
            });
            jSpinner8.addChangeListener(new ChangeListener() {
                private int old = 0;

                @Override
                public void stateChanged(ChangeEvent e) {
                    if(data == null) {
                        jSpinner8.setValue(0);
                        return;
                    }
                    int current = ((Number) jSpinner8.getValue()).intValue();
                    currentGlyph.getBounds().height = current;
                    doRepaint(((Number) jSpinner1.getValue()).intValue(), ((Number) jSpinner2.getValue()).intValue(), ((Number) jSpinner7.getValue()).intValue(), Math.max(old, current));
                    old = current;
                    int high = data.getHeight();
                    if(image != null) {
                        high = image.height;
                    }
                    ((SpinnerNumberModel) jSpinner2.getModel()).setMaximum(high - ((Number) jSpinner8.getValue()).intValue());
                }
            });
        }
        //</editor-fold>
    }

    private void doRepaint(int x, int y, int w, int h) {
        this.canvas.repaint();//x, y, h, h);
    }

    private VBF data;

    private BitmapGlyph currentGlyph;

    private VTF image;

    private void load(String f) throws IOException {
        LOG.log(Level.INFO, "Loading {0}", f);
        VBFCanvas p = this.canvas;

        data = VBF.load(new FileInputStream(f + ".vbf"));
        p.setVBF(data);

        DefaultTreeModel model = (DefaultTreeModel) this.jTree1.getModel();
        for(BitmapGlyph g : data.getGlyphs()) {
            insertGlyph(model, g);
        }

        File vtf = new File(f + ".vtf");
        if(vtf.exists()) {
            image = VTF.load(new FileInputStream(vtf));
            p.setVTF(image);
        }
        canvas.repaint();
    }

    private void insertGlyph(DefaultTreeModel model, BitmapGlyph glyph) {
        DefaultMutableTreeNode child = new DefaultMutableTreeNode(glyph);
        model.insertNodeInto(child, (MutableTreeNode) model.getRoot(), model.getChildCount(model.getRoot()));
        insertCharacters(model, child, glyph.getIndex());
        model.reload();
    }

    private void insertCharacters(DefaultTreeModel model, DefaultMutableTreeNode child, int g) {
        for(int i = 0; i < data.getTable().length; i++) {
            int glyphIndex = data.getTable()[i];
            if(glyphIndex != g) {
                continue;
            }
            DefaultMutableTreeNode sub = new DefaultMutableTreeNode(new DisplayableCharacter(i));
            model.insertNodeInto(sub, child, model.getChildCount(child));
        }
    }

    private void treeInteraction(TreeSelectionEvent evt) {
        TreePath selection = evt.getNewLeadSelectionPath();
        if(selection == null) {
            return;
        }
        JTree other = evt.getSource() == jTree1 ? jTree2 : evt.getSource() == jTree2 ? jTree1 : null;
        if(other != null) {
            other.setSelectionRow(-1);
        }
        Object node = selection.getLastPathComponent();
        if(!(node instanceof DefaultMutableTreeNode)) {
            return;
        }
        Object obj = ((DefaultMutableTreeNode) node).getUserObject();
        if(!(obj instanceof BitmapGlyph)) {
            return;
        }
        currentGlyph = (BitmapGlyph) obj;

        if(currentGlyph.getBounds() == null) {
            currentGlyph.setBounds(new Rectangle());
        }
        Rectangle r = currentGlyph.getBounds();
        jSpinner1.setValue(r.x);
        jSpinner2.setValue(r.y);
        jSpinner7.setValue(r.width);
        jSpinner8.setValue(r.height);
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
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jSpinner1 = new javax.swing.JSpinner();
        jSpinner2 = new javax.swing.JSpinner();
        jPanel5 = new javax.swing.JPanel();
        jSpinner7 = new javax.swing.JSpinner();
        jSpinner8 = new javax.swing.JSpinner();
        jSplitPane2 = new javax.swing.JSplitPane();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTree1 = new com.timepath.swing.ReorderableJTree();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTree2 = new com.timepath.swing.ReorderableJTree();
        jScrollPane1 = new javax.swing.JScrollPane();
        canvas = new com.timepath.hl2.io.swing.VBFCanvas();
        jMenuBar1 = new javax.swing.JMenuBar();
        jMenu1 = new javax.swing.JMenu();
        jMenuItem1 = new javax.swing.JMenuItem();
        jMenuItem2 = new javax.swing.JMenuItem();
        jMenu2 = new javax.swing.JMenu();
        jMenuItem3 = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Bitmap Font Editor");

        jSplitPane1.setDividerSize(2);
        jSplitPane1.setContinuousLayout(true);
        jSplitPane1.setEnabled(false);

        jPanel2.setPreferredSize(new java.awt.Dimension(200, 195));

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Position"));

        jSpinner1.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));

        jSpinner2.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jSpinner1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 61, Short.MAX_VALUE)
                .add(18, 18, 18)
                .add(jSpinner2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 61, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jSpinner1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 28, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jSpinner2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 28, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Dimensions"));

        jSpinner7.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));

        jSpinner8.setModel(new javax.swing.SpinnerNumberModel(Integer.valueOf(0), Integer.valueOf(0), null, Integer.valueOf(1)));

        org.jdesktop.layout.GroupLayout jPanel5Layout = new org.jdesktop.layout.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .add(jSpinner7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 61, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(18, 18, 18)
                .add(jSpinner8, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 61, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jSpinner7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 28, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jSpinner8, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 28, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jPanel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(14, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(13, Short.MAX_VALUE))
        );

        jSplitPane2.setDividerSize(2);
        jSplitPane2.setResizeWeight(0.5);
        jSplitPane2.setEnabled(false);

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

        jSplitPane2.setLeftComponent(jScrollPane3);

        jScrollPane4.setBorder(null);

        treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("root");
        jTree2.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        jTree2.setRootVisible(false);
        jTree2.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                jTree2ValueChanged(evt);
            }
        });
        jScrollPane4.setViewportView(jTree2);

        jSplitPane2.setRightComponent(jScrollPane4);

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jSplitPane2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .add(jSplitPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 200, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
        );

        jSplitPane1.setLeftComponent(jPanel1);

        jScrollPane1.setBorder(null);

        canvas.setLayout(new java.awt.BorderLayout());
        jScrollPane1.setViewportView(canvas);

        jSplitPane1.setRightComponent(jScrollPane1);

        jMenu1.setText("File");

        jMenuItem1.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem1.setMnemonic('O');
        jMenuItem1.setText("Open");
        jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                open(evt);
            }
        });
        jMenu1.add(jMenuItem1);

        jMenuItem2.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        jMenuItem2.setMnemonic('S');
        jMenuItem2.setText("Save");
        jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                save(evt);
            }
        });
        jMenu1.add(jMenuItem2);

        jMenuBar1.add(jMenu1);

        jMenu2.setText("Edit");

        jMenuItem3.setText("Create glyph");
        jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                createGlyph(evt);
            }
        });
        jMenu2.add(jMenuItem3);

        jMenuBar1.add(jMenu2);

        setJMenuBar(jMenuBar1);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 600, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jSplitPane1)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void open(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_open
        try {
            File[] fs = new NativeFileChooser().setParent(this).setTitle("Select vbf").choose();
            if(fs == null) {
                return;
            }
            File f = fs[0];
            load(f.getPath().replace(".vbf", ""));
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_open

    private void save(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_save
        try {
            File[] fs = new NativeFileChooser().setParent(this).setTitle("Select save location").setDialogType(BaseFileChooser.DialogType.SAVE_DIALOG).choose();
            if(fs == null) {
                return;
            }
            File f = fs[0];
            data.save(f);
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_save

    private void jTree1ValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_jTree1ValueChanged
        treeInteraction(evt);
    }//GEN-LAST:event_jTree1ValueChanged

    private void jTree2ValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_jTree2ValueChanged
        treeInteraction(evt);
    }//GEN-LAST:event_jTree2ValueChanged

    private void createGlyph(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_createGlyph
        BitmapGlyph g = new BitmapGlyph();
        if(data == null) {
            data = new VBF();
            canvas.setVBF(data);
        }
        for(int i = 0; i < 256; i++) {
            if(!data.hasGlyph(i)) {
                g.setIndex(i);
                break;
            }
            if(i == data.getGlyphs().size()) {
                g.setIndex(i + 1);
            }
        }
        data.getGlyphs().add(g);
        this.insertGlyph((DefaultTreeModel) jTree1.getModel(), g);
    }//GEN-LAST:event_createGlyph

    /**
     * @param args the command line arguments
     */
    public static void main(String... args) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new VBFTest().setVisible(true);
            }
        });
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private com.timepath.hl2.io.swing.VBFCanvas canvas;
    private javax.swing.JMenu jMenu1;
    private javax.swing.JMenu jMenu2;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JMenuItem jMenuItem2;
    private javax.swing.JMenuItem jMenuItem3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JSpinner jSpinner1;
    private javax.swing.JSpinner jSpinner2;
    private javax.swing.JSpinner jSpinner7;
    private javax.swing.JSpinner jSpinner8;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JSplitPane jSplitPane2;
    private com.timepath.swing.ReorderableJTree jTree1;
    private com.timepath.swing.ReorderableJTree jTree2;
    // End of variables declaration//GEN-END:variables
}
