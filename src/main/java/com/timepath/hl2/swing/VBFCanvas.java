package com.timepath.hl2.swing;

import com.timepath.hl2.io.font.VBF;
import com.timepath.hl2.io.image.VTF;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class VBFCanvas extends JPanel implements MouseListener, MouseMotionListener {

    private static final Logger         LOG              = Logger.getLogger(VBFCanvas.class.getName());
    private static final long           serialVersionUID = 1L;
    private static final AlphaComposite acNormal         = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1);
    private static final AlphaComposite acSelected       = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);
    private static final AlphaComposite acText           = AlphaComposite.getInstance(AlphaComposite.SRC_OVER);
    private Image img;
    private Point last;
    private List<VBF.BitmapGlyph> selected = new LinkedList<>();
    private VBF vbf;
    private VTF vtf;

    /**
     * Creates new form VBFTest
     */
    public VBFCanvas() {
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public VBF.BitmapGlyph getSelected() {
        if(selected.isEmpty()) {
            return null;
        }
        return selected.get(0);
    }

    public void setVBF(VBF vbf) {
        this.vbf = vbf;
        revalidate();
    }

    public void setVTF(VTF t) {
        vtf = t;
        vbf.setWidth(vtf.getWidth());
        vbf.setHeight(vtf.getHeight());
        repaint();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mousePressed(MouseEvent e) {
        last = e.getPoint();
        if(!SwingUtilities.isLeftMouseButton(e)) {
            return;
        }
        Iterable<VBF.BitmapGlyph> old = new LinkedList<>(selected);
        List<VBF.BitmapGlyph> clicked = get(e.getPoint());
        for(VBF.BitmapGlyph g : clicked) {
            if(selected.contains(g)) {
                return;
            }
        }
        if(e.isControlDown()) {
            selected.addAll(clicked);
        } else {
            selected = clicked;
        }
        for(VBF.BitmapGlyph g : old) {
            repaint(g.getBounds());
        }
        for(VBF.BitmapGlyph g : selected) {
            repaint(g.getBounds());
        }
    }

    List<VBF.BitmapGlyph> get(Point p) {
        List<VBF.BitmapGlyph> intersected = new LinkedList<>();
        if(( vbf == null ) || ( vbf.getGlyphs() == null )) {
            return intersected;
        }
        for(VBF.BitmapGlyph g : vbf.getGlyphs()) {
            if(g.getBounds().contains(p)) {
                intersected.add(g);
            }
        }
        return intersected;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point p = e.getPoint();
        if(last != null) {
            e.translatePoint(-last.x, -last.y);
        }
        for(VBF.BitmapGlyph sel : selected) {
            if(SwingUtilities.isRightMouseButton(e)) {
                sel.getBounds().width += e.getPoint().x;
                sel.getBounds().height += e.getPoint().y;
            } else {
                sel.getBounds().x += e.getPoint().x;
                sel.getBounds().y += e.getPoint().y;
            }
        }
        last = p;
        repaint();
    }

    @Override
    public void mouseMoved(MouseEvent e) {
    }

    public void select(VBF.BitmapGlyph g) {
        selected.clear();
        if(g != null) {
            selected.add(g);
            repaint(g.getBounds());
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setComposite(acNormal);
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, getWidth(), getHeight());
        if(( img == null ) && ( vtf != null )) {
            try {
                img = vtf.getImage(0);
            } catch(IOException ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
        if(vbf != null) {
            g2.setColor(Color.GRAY);
            g2.fillRect(0, 0, vbf.getWidth(), vbf.getHeight());
        }
        if(img != null) {
            g2.drawImage(img, 0, 0, this);
        }
        if(vbf != null) {
            for(VBF.BitmapGlyph glyph : vbf.getGlyphs()) {
                if(glyph == null) {
                    continue;
                }
                Rectangle bounds = glyph.getBounds();
                if(( bounds == null ) || bounds.isEmpty()) {
                    continue;
                }
                g2.setComposite(acNormal);
                g2.setColor(Color.GREEN);
                g2.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
                if(selected.contains(glyph)) {
                    g2.setComposite(acSelected);
                    g2.fillRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
                }
                //                 TODO: Negative font folor
                //                Map<TextAttribute, Object> map = new Hashtable<TextAttribute, Object>();
                //                map.put(TextAttribute.SWAP_COLORS, TextAttribute.SWAP_COLORS_ON);
                //                map.put(TextAttribute.FOREGROUND, Color.BLACK);
                //                map.put(TextAttribute.BACKGROUND, Color.TRANSLUCENT);
                //                Font f = this.getFont().deriveFont(map);
                //                g.setFont(f);
                //                g.setXORMode(Color.WHITE);
                g2.setComposite(acText);
                g2.setColor(Color.GREEN);
                g2.drawString(Integer.toString(glyph.getIndex()), bounds.x + 1, ( bounds.y + bounds.height ) - 1);
            }
        }
    }

    @Override
    public Dimension getPreferredSize() {
        if(vbf == null) {
            return new Dimension(128, 128);
        }
        return new Dimension(vbf.getWidth(), vbf.getHeight());
    }
}
