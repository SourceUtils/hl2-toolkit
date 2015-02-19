package com.timepath.vgui;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * @author TimePath
 * @see <a href="http://i.imgur.com/KxiV3.jpg">OSX</a>
 * @see <a href="http://i.imgur.com/VqABM.jpg">WIN</a>
 */
public abstract class VGUIRenderer {
    public BufferedImage getElementImage() {
        if (elementImage != null) return elementImage;
        BufferedImage img = new BufferedImage((int) screen.getWidth(), (int) screen.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        Collections.sort(elements, LAYER_SORT);
        for (Element element : elements) {
            g.setComposite(SRC_OVER);
            paintElement(element, g);
        }

        g.dispose();
        return elementImage = ImageUtils.toCompatibleImage(img);
    }

    public static void registerLocator(ResourceLocator locator) {
        locators.add(locator);
    }

    public static InputStream locate(String path) {
        for (ResourceLocator locator : locators) {
            InputStream result = locator.locate(path);
            if (result != null)
                return result;
        }

        return null;
    }

    public static Image locateImage(final String name) {
        for (ResourceLocator locator : locators) {
            Image result = locator.locateImage(name);
            if (result != null)
                return result;
        }

        return null;
    }

    private static void fitRect(Rectangle r, Point p1, Point p2) {
        r.x = Math.min(p1.x, p2.x);
        r.y = Math.min(p1.y, p2.y);
        r.width = Math.abs(p2.x - p1.x);
        r.height = Math.abs(p2.y - p1.y);
    }

    /**
     * Finds the greatest common multiple
     */
    private static long gcm(long a, long b) {
        return (b == 0) ? a : gcm(b, a % b);
    }

    public void paintElement(Element e, Graphics2D g) {
        VGUIRenderer r = this;
        if (width(e) != 0 && height(e) != 0) {// invisible? don't waste time
            int elementX = (int) Math.round((double) x(e) * (r.screen.getWidth() / r.internal.getWidth()) * r.scale);
            int elementY = (int) Math.round((double) y(e) * (r.screen.getHeight() / r.internal.getHeight()) * r.scale);
            int elementW = (int) Math.round((double) width(e) * (r.screen.getWidth() / r.internal.getWidth()) * r.scale);
            int elementH = (int) Math.round((double) height(e) * (r.screen.getHeight() / r.internal.getHeight()) * r.scale);
            if (r.selectedElements.contains(e)) {
                elementX += r.dragX;
                elementY += r.dragY;
            }

            Rectangle bounds = new Rectangle(elementX, elementY, elementW, elementH);
            Shape clip = g.getClip();
            g.setClip(bounds);
            if (e.getFgColor() != null) {
                g.setColor(e.getFgColor());
                float elementBgAlpha = 0.25f;
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, elementBgAlpha));
                g.fillRect(elementX, elementY, elementW - 1, elementH - 1);
            }

            if (e.getImage() != null) {
                if (e.getFgColor() != null) {
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, e.getFgColor().getAlpha()));
                }

                g.drawImage(e.getImage(), elementX, elementY, elementW, elementH, null);
            }

            if (r.selectedElements.contains(e)) {
                g.setColor(Color.CYAN);
            } else {
                g.setColor(Color.GREEN);
            }

            g.drawRect(elementX, elementY, elementW - 1, elementH - 1);
            if (e.equals(r.hoveredElement)) {
                g.setColor(new Color(255 - g.getColor().getRed(), 255 - g.getColor().getGreen(), 255 - g.getColor().getBlue()));
                g.setComposite(SRC_OVER);
                //                g.drawRect(elementX + offX, elementY + offY, e.getWidth() - 1, e.getHeight() - 1) // border
                g.drawRect(elementX + 1, elementY + 1, elementW - 3, elementH - 3);// inner
                //                g.drawRect(elementX + offX - 1, elementY + offY - 1, e.getWidth() + 1,
                // e.getHeight() + 1) // outer
            }

            if ((e.getLabelText() != null) && !e.getLabelText().isEmpty()) {
                if (e.getFgColor() != null) {
                    g.setColor(e.getFgColor());
                } else {
                    g.setColor(Color.WHITE);
                }

                g.setComposite(SRC_OVER);
                int screenRes = Toolkit.getDefaultToolkit().getScreenResolution();
                int fontSize = (int) Math.round(14.0d * screenRes / 72.0d);// Java2D = 72 DPI
                if (e.getFont() == null) {
                    e.setFont(g.getFont());// a default
                }

                FontMetrics fm = getFontMetrics(e.getFont());
                int width = fm.stringWidth(e.getLabelText());
                g.setFont(e.getFont());
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (e.getTextAlignment().equals(Element.Alignment.Left)) {
                    g.drawString(e.getLabelText(), elementX, elementY + fontSize);
                } else if (e.getTextAlignment().equals(Element.Alignment.Right)) {
                    g.drawString(e.getLabelText(), elementX + elementW - width, elementY + fontSize);
                } else if (e.getTextAlignment().equals(Element.Alignment.Center)) {
                    g.drawString(e.getLabelText(), elementX + ((elementW - width) / 2), elementY + fontSize);
                }

            }

            g.setClip(clip);
        }

        //        for(int i = 0 i < e.children.size() i++) {
        //            paintElement(e.children.get(i), g)
        //        }

    }

    public abstract void doRepaint(Rectangle bounds);

    public abstract void repaint();

    public abstract FontMetrics getFontMetrics(Font font);

    /**
     * Checks if point p is inside the bounds of any element
     */
    public List<Element> pick(Point p) {
        List<Element> potential = new LinkedList<Element>();
        for (Element e : elements) {
            if (bounds(e).contains(p)) {
                potential.add(e);
            }

        }

        return potential;
    }

    public void select(@NotNull Point p1, @NotNull Point p2, boolean ctrl) {
        Rectangle originalSelectRect = new Rectangle(selectRect);
        fitRect(selectRect, p1, p2);
        for (Element e : elements) {
            if (selectRect.intersects(bounds(e))) {
                select(e);// TODO: not perfect, I want the selection inverted as it goes over
            } else if (!ctrl) {
                deselect(e);
            }

        }

        // This repaints the overlap a second time. A minor inefficiency...
        doRepaint(new Rectangle((int) originalSelectRect.getX(), (int) (originalSelectRect.getY()), (int) (originalSelectRect.getWidth() + 1), (int) (originalSelectRect.getHeight() + 1)));
        doRepaint(new Rectangle((int) selectRect.getX(), (int) (selectRect.getY()), (int) (selectRect.getWidth() + 1), (int) (selectRect.getHeight() + 1)));
    }

    /**
     * Special exceptions are handled here
     */
    public void load(Element element) {
        if (element.getFile() == null) return;

        if (Element.areas.containsKey(element.getFile())) {
            Element p = Element.areas.get(element.getFile());
            p.addNode(element);
            addElement(p);
        } else if ("HudPlayerHealth".equalsIgnoreCase(element.getFile())) {// Better, but still not perfect
            // Move by "CHealthAccountPanel" delta_item_x" and "delta_item_start_y"
            Element p = Element.areas.get("CHealthAccountPanel");
            p.addNode(element);
            addElement(p);
        } else if ("HudAmmoWeapons".equalsIgnoreCase(element.getFile())) {
            Element p = Element.areas.get("HudWeaponAmmo");
            p.addNode(element);
            addElement(p);
        }

        addElement(element);// Weird but it has to be done
    }

    public void addElement(Element e) {
        if (elements.contains(e)) return;

        //            e.setCanvas(this)
        elements.add(e);
        doRepaint(bounds(e));
    }

    public void removeElements(List<Element> remove) {
        elements.removeAll(remove);
    }

    public void select(Element e) {
        if (e == null) return;

        if (selectedElements.contains(e)) return;

        selectedElements.add(e);
        //            if(e.children != null) {
        //                for(int i = 0 i < e.children.size() i++) {
        //                    select(e.children.get(i))
        //                }
        //            }
        doRepaint(bounds(e));
    }

    public boolean isSelected(Element e) {
        return selectedElements.contains(e);
    }

    public List<Element> getSelected() {
        return Collections.unmodifiableList(selectedElements);
    }

    public void deselect(Element e) {
        if (e == null) return;

        if (!selectedElements.contains(e)) return;

        selectedElements.remove(e);
        //            if(e.children != null) {
        //                for(int i = 0 i < e.children.size() i++) {
        //                    deselect(e.children.get(i))
        //                }
        //            }
        doRepaint(bounds(e));
    }

    public void deselectAll() {
        List<Element> old = selectedElements;
        selectedElements = new LinkedList<Element>();
        for (Element e : old) doRepaint(bounds(e));
    }

    public void translate(Element e, double dx, double dy) {// TODO: scaling (scale 5 = 5 pixels to move 1 x/y co-ord)
        //        Rectangle originalBounds = new Rectangle(e.getBounds())
        if (e.getXAlignment().equals(Element.Alignment.Right)) {
            dx *= -1;
        }

        double scaleX = screen.getWidth() / internal.getWidth();
        dx = Math.round(dx / scaleX);
        e.setLocalX(e.getLocalX() + dx);
        if (e.getYAlignment().equals(Element.VAlignment.Bottom)) {
            dy *= -1;
        }

        double scaleY = screen.getHeight() / internal.getHeight();
        dy = Math.round(dy / scaleY);
        e.setLocalY(e.getLocalY() + dy);
        //        this.doRepaint(originalBounds)
        //        this.doRepaint(e.getBounds())
        repaint();// helps
    }

    public int x(Element e) {
        if (e != null) {
            Element parent = e.getParent();
            int lx = e.getLocalXi();
            int _x = x(parent);
            switch (e.getXAlignment()) {
                case Left:
                    return (_x + (lx));
                case Center:
                    return (_x + (width(parent) / 2 + lx));
                case Right:
                    return (_x + (width(parent) - lx));
            }
        }

        return 0;
    }

    public int y(Element e) {
        if (e != null) {
            Element parent = e.getParent();
            int ly = e.getLocalYi();
            int _y = y(parent);
            switch (e.getYAlignment()) {
                case Top:
                    return (_y + (ly));
                case Center:
                    return (_y + (height(parent) / 2 + ly));
                case Bottom:
                    return (_y + (height(parent) - ly));
            }
        }

        return 0;
    }

    public int width(Element e) {
        if (e == null)
            return (int) internal.getWidth();
        return (e.getWideMode().equals(Element.DimensionMode.Mode1)) ? (e.getWide()) : (width(e.getParent()) - e.getWide());
    }

    public int height(Element e) {
        if (e == null)
            return (int) internal.getHeight();
        return (e.getTallMode().equals(Element.DimensionMode.Mode1)) ? (e.getTall()) : (height(e.getParent()) - e.getTall());
    }

    public Rectangle bounds(Element e) {
        Double scaleX = (screen.getWidth() / internal.getWidth()) * scale;
        Double scaleY = (screen.getHeight() / internal.getHeight()) * scale;
        return new Rectangle(
                (int) Math.round(scaleX * e.getLocalX()),
                (int) Math.round(scaleY * e.getLocalY()),
                (int) Math.round(scaleX * e.getWide()) + 1,
                (int) Math.round(scaleY * e.getTall()) + 1
        );
    }

    public void setElementImage(BufferedImage elementImage) {
        this.elementImage = elementImage;
    }

    public final LinkedList<Element> getElements() {
        return elements;
    }

    public List<Element> getSelectedElements() {
        return selectedElements;
    }

    public void setSelectedElements(List<Element> selectedElements) {
        this.selectedElements = selectedElements;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public Dimension getScreen() {
        return screen;
    }

    public void setScreen(Dimension screen) {
        this.screen = screen;
    }

    public Dimension getInternal() {
        return internal;
    }

    public void setInternal(Dimension internal) {
        this.internal = internal;
    }

    public Rectangle getSelectRect() {
        return selectRect;
    }

    public void setSelectRect(Rectangle selectRect) {
        this.selectRect = selectRect;
    }

    public int getDragX() {
        return dragX;
    }

    public void setDragX(int dragX) {
        this.dragX = dragX;
    }

    public int getDragY() {
        return dragY;
    }

    public void setDragY(int dragY) {
        this.dragY = dragY;
    }

    public Element getHoveredElement() {
        return hoveredElement;
    }

    public void setHoveredElement(Element hoveredElement) {
        this.hoveredElement = hoveredElement;
    }

    private static final AlphaComposite SRC_OVER = AlphaComposite.SrcOver;
    private static final Comparator<Element> LAYER_SORT = new Comparator<Element>() {
        @Override
        public int compare(@NotNull Element a, @NotNull Element b) {
            return a.getLayer() - b.getLayer();
        }
    };
    private BufferedImage elementImage;
    /**
     * List of elements
     */
    private final LinkedList<Element> elements = new LinkedList<Element>();
    /**
     * List of currently selected elements
     */
    private List<Element> selectedElements = new LinkedList<Element>();
    /**
     * Render scale
     */
    private double scale = 1;
    /**
     * Screen space
     */
    private Dimension screen;
    /**
     * Virtual space
     */
    private Dimension internal;
    /**
     * Selection
     */
    private Rectangle selectRect = new Rectangle();
    /**
     * Drag X displacement
     */
    private int dragX;
    /**
     * Drag Y displacement
     */
    private int dragY;
    /**
     * Currently hoverer
     */
    private Element hoveredElement;
    private static List<ResourceLocator> locators = new ArrayList<ResourceLocator>();

    public static abstract class ResourceLocator {
        /**
         * Locate a resource
         *
         * @return an InputStream to the resource, or null
         */
        public abstract InputStream locate(String path);

        public abstract Image locateImage(String path);
    }
}
