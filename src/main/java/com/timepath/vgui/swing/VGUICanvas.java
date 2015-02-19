package com.timepath.vgui.swing;

import com.timepath.steam.SteamUtils;
import com.timepath.steam.io.VDFNode;
import com.timepath.vgui.Element;
import com.timepath.vgui.ImageUtils;
import com.timepath.vgui.VGUIRenderer;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * @author TimePath
 */
@SuppressWarnings("serial")
public class VGUICanvas extends JPanel implements MouseListener, MouseMotionListener {

    private static VDFNode.VDFProperty p(String k, Object v) {
        return new VDFNode.VDFProperty(k, v);
    }

    public static void main(String[] args) throws IOException {
        VGUICanvas canvas = new VGUICanvas();
        VDFNode root = new VDFNode("Root");
        VDFNode node = new VDFNode("Test");
        root.addNode(node);
        node.addAllProperties(new ArrayList<>(Arrays.asList(
                p("enabled", 1),
                p("visible", 1),
                p("xpos", 5),
                p("ypos", 5),
                p("zpos", 0),
                p("wide", 100),
                p("tall", 100),
                p("labeltext", "test"),
                p("textalignment", "center"),
                p("controlname", "label")
        )));
        node = new VDFNode(new FileInputStream(new File(SteamUtils.getSteam(), "resource/FileOpenDialog.res")), StandardCharsets.UTF_8);
        for (VDFNode parent : node.getNodes()) {
            for (VDFNode n : parent.getNodes()) {
                Element e = Element.importVdf(n);
                canvas.r.addElement(e);
            }
        }
        JFrame f = new JFrame();
        f.setContentPane(canvas);
        f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }

    public LinkedList<Element> getElements() {
        return r.getElements();
    }

    public VGUICanvas() {
        addMouseListener(this);
        addMouseMotionListener(this);
        setPreferredSize(new Dimension(640, 480));
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resize();
            }

        });
    }

    private void resize() {
        offX = (int) ((getWidth() - r.getInternal().width) / 2);
        offY = (int) ((getHeight() - r.getInternal().height) / 2);
        repaint();
    }

    /**
     * Fired when an element has been dropped
     */
    protected void placed() {
    }

    public void setBackgroundImage(Image background) {
        this.background = background;
        prepareImage(background, this);
        repaint();
    }

    private Rectangle getOutliers() {
        Rectangle rect = new Rectangle(r.getInternal().width, r.getInternal().height);
        for (Element elem : r.getElements()) rect.add(r.bounds(elem));
        return ((Rectangle) (rect));
    }

    /**
     * Convenience method for repainting the bare minimum
     */
    private void doRepaint(Rectangle bounds) {
        r.setElementImage(null);
        repaint(offX + bounds.x, offY + bounds.y, (int) bounds.width - 1, (int) bounds.height - 1);
    }

    private void doRepaint1(Rectangle bounds) {
        r.setElementImage(null);
        repaint(offX + bounds.x, offY + bounds.y, bounds.width, bounds.height);
    }

    private void hover(Element e) {
        if (e.equals(r.getHoveredElement())) return;
// Nothing to do
        // Clean up if needed
        if (r.getHoveredElement() != null)
            doRepaint(r.bounds(r.getHoveredElement()));
        // Draw the new element if needed
        r.setHoveredElement(e);
        if (r != null)
            doRepaint(r.bounds(e));
    }

    /**
     * As soon as the height drops below 480, stops rendering
     */
    private BufferedImage drawGrid() {
        BufferedImage img = new BufferedImage(r.getScreen().width, r.getScreen().height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setComposite(GRID_AC);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g.setColor(GRID_COLOR);
        int w = r.getScreen().width;
        int h = r.getScreen().height;
        int minGridSpacing = 10;
        int i = minGridSpacing;
        if (i < 0) return ((BufferedImage) (img));
        if (i < 2) {// Optimize for small numbers, stop division by zero
            g.fillRect(0, 0, r.getScreen().width, r.getScreen().height);
            return ((BufferedImage) (img));
        }

        int cross = 0;
        int maxX = w - (w % i);
        int maxY = h - (h % i);
        double multX = r.getScreen().getWidth() / r.getInternal().getWidth();
        double multY = r.getScreen().getHeight() / r.getInternal().getHeight();
        for (int y = -1; y <= (maxY / i); y++) {
            for (int x = -1; x <= (maxX / i); x++) {
                int dx = (int) Math.round((double) (maxX * x * i * multX) / maxX);
                int dy = (int) Math.round((double) (maxY * y * i * multY) / maxY);
                g.drawLine(dx + cross, dy + cross, (int) dx - (1 + cross), (int) dy - (1 + cross));
                g.drawLine((int) dx - (1 + cross), dy + cross, dx + cross, (int) dy - (1 + cross));
            }

        }

        g.dispose();
        return ((BufferedImage) (img));
    }

    @Override
    public void setPreferredSize(Dimension preferredSize) {
        Dimension UISize = new Dimension(preferredSize.width + (2 * offX), preferredSize.height + (2 * offY));
        super.setPreferredSize(UISize);
        currentbg = null;
        gridbg = null;
        r.setScreen(preferredSize);
        //        long gcm = gcm(hudRes.width, hudRes.height)
        double resX = r.getScreen().getWidth();
        double resY = r.getScreen().getHeight();
        double m = resX / resY;
        //        System.out.println(resX + "/" + resY + "=" + m)
        //        System.out.println((resX / gcm) + ":" + (resY / gcm) + " = " + Math.round(m * 480) + "x" + 480)
        r.setInternal(new Dimension((int) Math.round((double) m * 480), 480));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        g.setColor(BG_COLOR);
        g.fillRect(0, 0, getWidth(), getHeight());
        if (background != null) {
            if (currentbg != null)
                currentbg = ImageUtils.toCompatibleImage(ImageUtils.resizeImage(background, r.getScreen().width, r.getScreen().height));
            g.drawImage(currentbg, offX, offY, this);
        } else {
            g.setColor(Color.WHITE.darker().darker());
            g.fillRect(offX, offY, (int) Math.round((double) r.getScreen().getWidth() * r.getScale()), (int) Math.round((double) r.getScreen().getHeight() * r.getScale()));
        }

        if (gridbg == null)
            gridbg = ImageUtils.toCompatibleImage(drawGrid());
        g.drawImage(gridbg, offX, offY, this);
        g.drawImage(r.getElementImage(), offX, offY, this);
        if (dragSelecting) {
            g.setComposite(SELECT_AC);
            g.setColor(Color.CYAN.darker());
            g.fillRect(offX + r.getSelectRect().x + 1, offY + r.getSelectRect().y + 1, (int) r.getSelectRect().width - 2, (int) r.getSelectRect().height - 2);
            g.setColor(Color.BLUE);
            g.drawRect(offX + r.getSelectRect().x, offY + r.getSelectRect().y, (int) r.getSelectRect().width - 1, (int) r.getSelectRect().height - 1);
        }

    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point p = e.getPoint();
        p.translate(-offX, -offY);// Localize
        if (SwingUtilities.isLeftMouseButton(e)) {
            if (dragSelecting) {
                r.select(dragStart, p, e.isControlDown());
            } else if (dragMoving) {
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                r.setDragX((int) r.getDragX() + p.x - dragStart.x);
                r.setDragY((int) r.getDragY() + p.y - dragStart.y);
                r.setElementImage(null);
                repaint();
                dragStart = p;
            }

        }

    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Point p = e.getPoint();
        p.translate(-offX, -offY);// Localize
        hover(chooseBest(r.pick(p)));
    }

    @Override
    public void mousePressed(MouseEvent e) {
        Point p = e.getPoint();
        p.translate(-offX, -offY);// Localize
        if (SwingUtilities.isLeftMouseButton(e)) {
            dragStart = new Point(p.x, p.y);
            r.getSelectRect().setSize(p.x, p.y);
            if (r.getHoveredElement() == null) {// Clicked nothing
                if (!e.isControlDown()) r.deselectAll();
                dragSelecting = true;
                dragMoving = false;
            } else {// Hovering over something
                dragSelecting = false;
                dragMoving = true;
                if (e.isControlDown()) {
                    // Toggle select
                    if (r.isSelected(r.getHoveredElement())) {
                        r.deselect(r.getHoveredElement());
                    } else {
                        r.select(r.getHoveredElement());
                    }

                } else {
                    // If the thing I'm hovering isn't selected already, drop all selections
                    if (!r.isSelected(r.getHoveredElement())) {
                        r.deselectAll();
                        r.select(r.getHoveredElement());
                    }

                }

            }

        }

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            setCursor(Cursor.getDefaultCursor());
            if (dragMoving) placed();// Release element
            dragSelecting = false;
            dragMoving = false;
            dragStart = null;
            Rectangle original = new Rectangle(r.getSelectRect());
            r.getSelectRect().setSize(0, 0);
            for (Element elem : r.getSelectedElements()) {
                // ???
                if (r.getSelectedElements().contains(elem.getParent()) && !elem.getParent().getName().replaceAll("\"", "").endsWith(".res")) {// XXX: hacky
                    continue;
                }

                r.translate(elem, r.getDragX(), r.getDragY());
            }

            r.setDragX(0);
            r.setDragY(0);
            doRepaint1(original);
        }

    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    private static Element chooseBest(List<Element> potential) {
        Element smallest = null;
        for (Element elem : potential) {
            if (smallest == null) {
                smallest = elem;
            } else if (elem.getLayer() > smallest.getLayer()) {// Sort by layer, then by size
                smallest = elem;
            } else if (elem.getLayer() == smallest.getLayer() && elem.getSize() < smallest.getSize()) {
                smallest = elem;
            }

        }

        return smallest;
    }

    public VGUIRenderer getR() {
        return r;
    }

    public void setR(VGUIRenderer r) {
        this.r = r;
    }

    private static final AlphaComposite GRID_AC = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f);
    private static final AlphaComposite SELECT_AC = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f);
    private Image background;
    private BufferedImage currentbg;
    private BufferedImage gridbg;
    private Color BG_COLOR = Color.GRAY;
    private Color GRID_COLOR = Color.WHITE;
    /**
     * Left
     */
    private int offX = 10;
    /**
     * Top
     */
    private int offY = 10;
    /**
     * Initial drag point
     */
    private Point dragStart;
    private boolean dragSelecting;
    private boolean dragMoving;
    private VGUIRenderer r = new VGUIRenderer() {
        @Override
        public void doRepaint(final Rectangle bounds) {
            VGUICanvas.this.doRepaint(bounds);
        }

        @Override
        public void repaint() {
            VGUICanvas.this.repaint();
        }

        @Override
        public FontMetrics getFontMetrics(final Font font) {
            return VGUICanvas.this.getFontMetrics(font);
        }

    };
}
