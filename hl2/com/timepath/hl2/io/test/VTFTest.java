package com.timepath.hl2.io.test;

import com.timepath.hl2.io.VTF;
import com.timepath.hl2.io.VTF.Format;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileFilter;

/**
 *
 * @author timepath
 */
public class VTFTest {

    private static final Logger LOG = Logger.getLogger(VTFTest.class.getName());

    public static void test() {
        final JFrame f = new JFrame("Vtf Loader");
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JScrollPane jsp = new JScrollPane();
        jsp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        jsp.getVerticalScrollBar().setUnitIncrement(64);
        f.add(jsp);
        JPanel pane = new JPanel();
        pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
        jsp.setViewportView(pane);

        @SuppressWarnings("serial")
        class ImagePreviewPanel extends JPanel implements PropertyChangeListener {

            private int width, height;

            private Image image;

            private static final int ACCSIZE = 128;

            private Color bg;

            ImagePreviewPanel() {
                setPreferredSize(new Dimension(ACCSIZE, -1));
                bg = getBackground();
            }

            public void propertyChange(PropertyChangeEvent e) {
                String propertyName = e.getPropertyName();

                // Make sure we are responding to the right event.
                if(propertyName.equals(JFileChooser.SELECTED_FILE_CHANGED_PROPERTY)) {
                    File selection = (File) e.getNewValue();
                    String name;

                    if(selection == null) {
                        return;
                    } else {
                        name = selection.getAbsolutePath();
                    }

                    /*
                     * Make reasonably sure we have an image format that AWT can
                     * handle so we don't try to draw something silly.
                     */
                    try {
                        VTF v = VTF.load(new FileInputStream(selection));
                        if(v == null) {
                            return;
                        }
                        Image i = v.getImage(0);
                        if(i == null) {
                            return;
                        }
                        f.setIconImage(VTF.load(new FileInputStream(selection)).getThumbImage());
                        image = (i != null ? i : new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB));
                        scaleImage();
                        repaint();
                    } catch(IOException ex) {
                        LOG.log(Level.SEVERE, null, ex);
                    }
                }
            }

            private void scaleImage() {
                width = image.getWidth(this);
                height = image.getHeight(this);
                double ratio = 1.0;

                /*
                 * Determine how to scale the image. Since the accessory can expand
                 * vertically make sure we don't go larger than ACCSIZE when scaling
                 * vertically.
                 */
                if(width >= height) {
                    ratio = (double) (ACCSIZE - 5) / width;
                    width = ACCSIZE - 5;
                    height = (int) (height * ratio);
                } else {
                    if(getHeight() > ACCSIZE) {
                        ratio = (double) (ACCSIZE - 5) / height;
                        height = ACCSIZE - 5;
                        width = (int) (width * ratio);
                    } else {
                        ratio = (double) getHeight() / height;
                        height = getHeight();
                        width = (int) (width * ratio);
                    }
                }

                image = image.getScaledInstance(width, height, Image.SCALE_DEFAULT);
            }

            @Override
            public void paintComponent(Graphics g) {
                g.setColor(bg);

                /*
                 * If we don't do this, we will end up with garbage from previous
                 * images if they have larger sizes than the one we are currently
                 * drawing. Also, it seems that the file list can paint outside
                 * of its rectangle, and will cause odd behavior if we don't clear
                 * or fill the rectangle for the accessory before drawing. This might
                 * be a bug in JFileChooser.
                 */
                g.fillRect(0, 0, ACCSIZE, getHeight());
                g.drawImage(image, getWidth() / 2 - width / 2 + 5, getHeight() / 2 - height / 2, this);
            }
        }

        class VtfFileFilter extends FileFilter {

            VtfFileFilter(Format format) {
                this.vtfFormat = format;
            }

            Format vtfFormat;

            @Override
            public boolean accept(File file) {
                if(file.isDirectory()) {
                    return true;
                }
                VTF v = null;
                try {
                    v = VTF.load(new FileInputStream(file));
                } catch(IOException ex) {
                    Logger.getLogger(VTFTest.class.getName()).log(Level.SEVERE, null, ex);
                }
                if(v == null) {
                    return false;
                }
                if(vtfFormat == VTF.Format.IMAGE_FORMAT_NONE) {
                    return true;
                }
                return (v.format == vtfFormat);
            }

            @Override
            public String getDescription() {
                return "VTF (" + (vtfFormat != Format.IMAGE_FORMAT_NONE ? vtfFormat.name() : "All") + ")";
            }
        }

        JFileChooser chooser = new JFileChooser();
        FileFilter generic = new VtfFileFilter(Format.IMAGE_FORMAT_NONE);
        chooser.addChoosableFileFilter(generic);
        chooser.addChoosableFileFilter(new VtfFileFilter(Format.IMAGE_FORMAT_DXT1));
        chooser.addChoosableFileFilter(new VtfFileFilter(Format.IMAGE_FORMAT_DXT5));
        chooser.setFileFilter(generic);
        ImagePreviewPanel preview = new ImagePreviewPanel();
        chooser.setAccessory(preview);
        chooser.addPropertyChangeListener(preview);
        chooser.setControlButtonsAreShown(false);
        pane.add(chooser);
//        boolean init = false;
//        File root = new File("res/vtf/hud/");
//        File[] subs = root.listFiles();
//        if(subs != null) {
//            for(int i = 0; i < subs.length; i++) {
//                if(subs[i].getName().endsWith(".vtf")) {
//                    try {
//                        VtfFile v = VtfFile.load(subs[i]);
//                        Image image = null;
//                        if(v != null) {
//                            image = v.getImage(0);
//                        }
//                        if(image != null) {
//                            JPanel p = new JPanel(new BorderLayout());
//                            p.setBackground(Color.MAGENTA.darker().darker());
//                            p.setSize(image.getWidth(null), image.getHeight(null));
//                            JLabel l = new JLabel();
//                            l.setToolTipText(subs[i].getName());
//                            l.setIcon(new ImageIcon(image));
//                            p.add(l, BorderLayout.CENTER);
//                            p.setBorder(BorderFactory.createLineBorder(Color.BLACK));
//                            pane.add(p);
//                            jsp.invalidate();
//                            jsp.validate();
//                            jsp.repaint();
//
//                            if(!init) {
        f.setVisible(true);
        f.pack();
//                                init = true;
//                            }
//                        }
//                    } catch (IOException ex) {
//                        logger.log(Level.SEVERE, null, ex);
//                    }
//                }
//            }
//        }
    }

    public static void main(String... args) {
        new Thread(new Runnable() {
            public void run() {
                test();
            }
        }).start();
    }
}