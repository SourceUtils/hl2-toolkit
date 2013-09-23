package com.timepath.hl2.swing;

import com.timepath.hl2.io.VBF;
import com.timepath.hl2.io.VBF.BitmapGlyph;
import com.timepath.hl2.io.VTF;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author timepath
 */
public class VBFCanvas extends JPanel implements MouseListener, MouseMotionListener {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(VBFCanvas.class.getName());

    /**
     * Creates new form VBFTest
     */
    public VBFCanvas() {
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
    }

    private static final int padding = 32 * 0;

    private static AffineTransform at = AffineTransform.getTranslateInstance(padding, padding);

    /**
     * No derive method on 1.5
     */
    private static AlphaComposite acNormal = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1);

    private static AlphaComposite acSelected = AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                                          0.5f);

    private static AlphaComposite acText = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);

    private ArrayList<BitmapGlyph> selected = new ArrayList<BitmapGlyph>();

    public void select(BitmapGlyph g) {
        selected.clear();
        if(g != null) {
            selected.add(g);
            this.repaint(g.getBounds());
        }
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        g.setComposite(acNormal);
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
//        g.setTransform(at);
        if(img == null && vtf != null) {
            try {
                img = vtf.getImage(vtf.mipCount - 1);
            } catch(IOException ex) {
                Logger.getLogger(VBFCanvas.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if(this.vbf != null) {
            g.setColor(Color.GRAY);
            g.fillRect(0, 0, vbf.getWidth(), vbf.getHeight());
        }
        if(img != null) {
            g.drawImage(img, 0, 0, this);
        }
        if(this.vbf != null) {
            for(BitmapGlyph glyph : vbf.getGlyphs()) {
                if(glyph == null) {
                    continue;
                }
                Rectangle bounds = glyph.getBounds();
                if(bounds == null || bounds.isEmpty()) {
                    continue;
                }
                g.setComposite(acNormal);
                g.setColor(Color.GREEN);
                g.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
                if(selected.contains(glyph)) {
                    g.setComposite(acSelected);
                    g.fillRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
                }
//                 TODO: Negative font folor
//                Map<TextAttribute, Object> map = new Hashtable<TextAttribute, Object>();
//                map.put(TextAttribute.SWAP_COLORS, TextAttribute.SWAP_COLORS_ON);
//                map.put(TextAttribute.FOREGROUND, Color.BLACK);
//                map.put(TextAttribute.BACKGROUND, Color.TRANSLUCENT);
//                Font f = this.getFont().deriveFont(map);
//                g.setFont(f);
//                g.setXORMode(Color.WHITE);
                g.setComposite(acText);
                g.setColor(Color.GREEN);
                g.drawString(Integer.toString(glyph.getIndex()), bounds.x + 1,
                             bounds.y + bounds.height - 1);
            }
        }
    }

    private VBF vbf;

    public void setVBF(VBF b) {
        this.vbf = b;
        this.revalidate();
    }

    private Image img;

    private VTF vtf;

    public void setVTF(VTF t) {
        this.vtf = t;
        this.vbf.setWidth(vtf.width);
        this.vbf.setHeight(vtf.height);
        this.repaint();
    }

    public ArrayList<BitmapGlyph> get(Point p) {
        ArrayList<BitmapGlyph> intersected = new ArrayList<BitmapGlyph>();
        if(vbf == null || vbf.getGlyphs() == null) {
            return intersected;
        }
        for(BitmapGlyph g : vbf.getGlyphs()) {
            if(g.getBounds().contains(p)) {
                intersected.add(g);
            }
        }
        return intersected;
    }

    @Override
    public Dimension getPreferredSize() {
        if(vbf == null) {
            return new Dimension(128, 128);
        }
        return new Dimension(vbf.getWidth(), vbf.getHeight());
    }

    public void mouseClicked(MouseEvent me) {
    }

    private Point last;

    public void mouseDragged(MouseEvent me) {
        Point p = me.getPoint();
        if(last != null) {
            me.translatePoint(-last.x, -last.y);
        }
        last = p;
        for(BitmapGlyph sel : selected) {
            sel.getBounds().x += me.getPoint().x;
            sel.getBounds().y += me.getPoint().y;
        }
        this.repaint();
    }

    public void mousePressed(MouseEvent me) {
        last = me.getPoint();
        ArrayList<BitmapGlyph> old = new ArrayList<BitmapGlyph>(selected);
        if(SwingUtilities.isLeftMouseButton(me)) {
            ArrayList<BitmapGlyph> clicked = get(me.getPoint());
            for(BitmapGlyph g : clicked) {
                if(selected.contains(g)) {
                    return;
                }
            }
            if(me.isControlDown()) {
                selected.addAll(clicked);
            } else {
                selected = clicked;
            }
            for(BitmapGlyph g : old) {
                this.repaint(g.getBounds());
            }
            for(BitmapGlyph g : selected) {
                this.repaint(g.getBounds());
            }
        }
    }

    public void mouseReleased(MouseEvent me) {
    }

    public void mouseEntered(MouseEvent me) {
    }

    public void mouseExited(MouseEvent me) {
    }

    public void mouseMoved(MouseEvent me) {
    }

    public BitmapGlyph getSelected() {
        if(this.selected.isEmpty()) {
            return null;
        }
        return this.selected.get(0);
    }

}
