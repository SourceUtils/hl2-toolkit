package com.timepath.hl2.swing;

import com.timepath.hl2.io.VTF;
import com.timepath.hl2.io.font.VBF;
import com.timepath.hl2.io.font.VBF.BitmapGlyph;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author TimePath
 */
public class VBFCanvas extends JPanel implements MouseListener, MouseMotionListener {

    private static final Logger LOG = Logger.getLogger(VBFCanvas.class.getName());

    private static final AlphaComposite acNormal = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1);

    private static final AlphaComposite acSelected = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);

    private static final AlphaComposite acText = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);

    private static final long serialVersionUID = 1L;

    private Image img;

    private Point last;

    private List<BitmapGlyph> selected = new LinkedList<BitmapGlyph>();

    private VBF vbf;

    private VTF vtf;

    /**
     * Creates new form VBFTest
     */
    public VBFCanvas() {
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
    }

    public List<BitmapGlyph> get(Point p) {
        List<BitmapGlyph> intersected = new LinkedList<BitmapGlyph>();
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

    public BitmapGlyph getSelected() {
        if(this.selected.isEmpty()) {
            return null;
        }
        return this.selected.get(0);
    }

    public void setVBF(VBF b) {
        this.vbf = b;
        this.revalidate();
    }

    public void setVTF(VTF t) {
        this.vtf = t;
        this.vbf.setWidth(vtf.getWidth());
        this.vbf.setHeight(vtf.getHeight());
        this.repaint();
    }

    public void mouseClicked(MouseEvent me) {
    }

    public void mouseDragged(MouseEvent me) {
        Point p = me.getPoint();
        if(last != null) {
            me.translatePoint(-last.x, -last.y);
        }
        for(BitmapGlyph sel : selected) {
            if(SwingUtilities.isRightMouseButton(me)) {
                sel.getBounds().width += me.getPoint().x;
                sel.getBounds().height += me.getPoint().y;
            } else {
                sel.getBounds().x += me.getPoint().x;
                sel.getBounds().y += me.getPoint().y;
            }
        }
        last = p;
        this.repaint();
    }

    public void mouseEntered(MouseEvent me) {
    }

    public void mouseExited(MouseEvent me) {
    }

    public void mouseMoved(MouseEvent me) {
    }

    public void mousePressed(MouseEvent me) {
        last = me.getPoint();
        if(!SwingUtilities.isLeftMouseButton(me)) {
            return;
        }
        List<BitmapGlyph> old = new LinkedList<BitmapGlyph>(selected);
        List<BitmapGlyph> clicked = get(me.getPoint());
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

    public void mouseReleased(MouseEvent me) {
    }

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
        if(img == null && vtf != null) {
            try {
                img = vtf.getImage(vtf.getMipCount() - 1);
            } catch(IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
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
                g.drawString(Integer.toString(glyph.getIndex()), bounds.x + 1, bounds.y + bounds.height - 1);
            }
        }
    }

}
