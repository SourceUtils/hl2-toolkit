package com.timepath.swing;

import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

/**
 *
 * @author timepath
 */
@SuppressWarnings("serial")
public class ThemeSelector extends JComboBox {

    public ThemeSelector() {
        Vector comboBoxItems = new Vector();
        final DefaultComboBoxModel model = new DefaultComboBoxModel(comboBoxItems);
        this.setModel(model);

        String lafId = UIManager.getLookAndFeel().getClass().getName();
        for(UIManager.LookAndFeelInfo lafInfo : UIManager.getInstalledLookAndFeels()) {
            String name = lafInfo.getName();
            model.addElement(name);
            if(lafInfo.getClassName().equals(lafId)) {
                model.setSelectedItem(name);
            }
        }

        this.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String laf = (String) ThemeSelector.this.getSelectedItem();
                try {
                    boolean originallyDecorated = UIManager.getLookAndFeel().getSupportsWindowDecorations();
                    for(UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                        if(laf.equals(info.getName())) {
                            LOG.log(Level.INFO, "Setting L&F: {0}", info.getClassName());
                            try {
                                UIManager.setLookAndFeel(info.getClassName());
                            } catch(InstantiationException ex) {
                                Logger.getLogger(ThemeSelector.class.getName()).log(Level.SEVERE, null, ex);
                            } catch(IllegalAccessException ex) {
                                Logger.getLogger(ThemeSelector.class.getName()).log(Level.SEVERE, null, ex);
                            } catch(UnsupportedLookAndFeelException ex) {
                                Logger.getLogger(ThemeSelector.class.getName()).log(Level.SEVERE, null, ex);
                            } catch(ClassNotFoundException ex) {
//                                Logger.getLogger(ThemeSelector.class.getName()).log(Level.SEVERE, null, ex);
                                LOG.warning("Unable to load user L&F");
                            }

                        }
                    }
                    boolean decorate = UIManager.getLookAndFeel().getSupportsWindowDecorations();
                    boolean decorateChanged = decorate != originallyDecorated;
                    boolean frameDecorations = false; // TODO: Frame decoration
//                    JFrame.setDefaultLookAndFeelDecorated(decorate);
//                    JDialog.setDefaultLookAndFeelDecorated(decorate);

                    for(Window w : Window.getWindows()) {
                        SwingUtilities.updateComponentTreeUI(w);
                        if(decorateChanged && frameDecorations) {
                            w.dispose();
                            handle(w, decorate);//w.isVisible());
                            w.setVisible(true);
                        }
                    }
                } catch(Exception ex) {
                    LOG.log(Level.WARNING, "Failed loading L&F: " + laf, ex);
                }
            }

            private void handle(Window window, boolean decorations) {
                int decor = JRootPane.FRAME;
                JRootPane rpc = null;
                if(window instanceof RootPaneContainer) {
                    rpc = ((RootPaneContainer) window).getRootPane();
                }
                if(window instanceof JFrame) {
                    ((JFrame) window).setUndecorated(decorations);
                } else if(window instanceof JDialog) {
                    ((JDialog) window).setUndecorated(decorations);
                } else {
                    LOG.log(Level.WARNING, "Unhandled setUndecorated mapping: {0}", window);
                }
                if(rpc != null) {
                    rpc.setWindowDecorationStyle(decor);
                }
            }
        });

    }

    private static final Logger LOG = Logger.getLogger(ThemeSelector.class.getName());

}
