package com.timepath.vgui

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.logging.Log

import java.awt.*
import java.awt.image.BufferedImage
import java.util.List

/**
 * @author TimePath
 * @see <a href="http://i.imgur.com/KxiV3.jpg">OSX</a>
 * @see <a href="http://i.imgur.com/VqABM.jpg">WIN</a>
 */
@CompileStatic
@TypeChecked
@Log('LOG')
abstract class VGUIRenderer {

    private static final AlphaComposite SRC_OVER = AlphaComposite.SrcOver
    private static final Comparator<Element> LAYER_SORT = { Element a, Element b -> a.layer <=> b.layer } as Comparator
    BufferedImage elementImage

    BufferedImage getElementImage() {
        if (elementImage) return elementImage
        def img = new BufferedImage(screen.width as int, screen.height as int, BufferedImage.TYPE_INT_ARGB)
        def g = img.createGraphics()
        Collections.sort(elements, LAYER_SORT)
        for (element in elements) {
            g.composite = SRC_OVER
            paintElement(element, g)
        }
        g.dispose()
        elementImage = ImageUtils.toCompatibleImage(img)
    }

    /** List of elements */
    final LinkedList<Element> elements = new LinkedList<>()
    /** List of currently selected elements */
    List<Element> selectedElements = new LinkedList<>()
    /** Render scale */
    double scale = 1
    /** Screen space */
    Dimension screen
    /** Virtual space */
    Dimension internal
    /** Selection */
    Rectangle selectRect = new Rectangle()
    /** Drag X displacement */
    int dragX
    /** Drag Y displacement */
    int dragY
    /** Currently hoverer */
    Element hoveredElement

    private static void fitRect(Rectangle r, Point p1, Point p2) {
        r.@x = Math.min(p1.@x, p2.@x)
        r.@y = Math.min(p1.@y, p2.@y)
        r.@width = Math.abs(p2.@x - p1.@x)
        r.@height = Math.abs(p2.@y - p1.@y)
    }

    /**
     * Finds the greatest common multiple
     *
     * @param a
     * @param b
     *
     * @return
     */
    private static long gcm(long a, long b) {
        return (b == 0) ? a : gcm(b, a % b)
    }

    void paintElement(Element e, Graphics2D g) {
        def r = this
        if (width(e) && height(e)) { // invisible? don't waste time
            int elementX = (int) Math.round(x(e) * (r.screen.width / r.internal.width) * r.scale)
            int elementY = (int) Math.round(y(e) * (r.screen.height / r.internal.height) * r.scale)
            int elementW = (int) Math.round(width(e) * (r.screen.width / r.internal.width) * r.scale)
            int elementH = (int) Math.round(height(e) * (r.screen.height / r.internal.height) * r.scale)
            if (r.selectedElements.contains(e)) {
                elementX += r.dragX
                elementY += r.dragY
            }
            def bounds = new Rectangle(elementX, elementY, elementW, elementH)
            def clip = g.getClip()
            g.clip = bounds
            if (e.fgColor != null) {
                g.color = e.fgColor
                float elementBgAlpha = 0.25f
                g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, elementBgAlpha)
                g.fillRect(elementX, elementY, elementW - 1, elementH - 1)
            }
            if (e.image != null) {
                if (e.fgColor != null) {
                    g.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, e.fgColor.alpha)
                }
                g.drawImage(e.image, elementX, elementY, elementW, elementH, null)
            }
            if (r.selectedElements.contains(e)) {
                g.color = Color.CYAN
            } else {
                g.color = Color.GREEN
            }
            g.drawRect(elementX, elementY, elementW - 1, elementH - 1)
            if (r.hoveredElement == e) {
                g.color = new Color(255 - g.color.red, 255 - g.color.green, 255 - g.color.blue)
                g.composite = SRC_OVER
                //                g.drawRect(elementX + offX, elementY + offY, e.getWidth() - 1, e.getHeight() - 1) // border
                g.drawRect(elementX + 1, elementY + 1, elementW - 3, elementH - 3) // inner
                //                g.drawRect(elementX + offX - 1, elementY + offY - 1, e.getWidth() + 1,
                // e.getHeight() + 1) // outer
            }
            if ((e.labelText != null) && !e.labelText.empty) {
                if (e.fgColor != null) {
                    g.color = e.fgColor
                } else {
                    g.color = Color.WHITE
                }
                g.composite = SRC_OVER
                int screenRes = Toolkit.defaultToolkit.screenResolution
                int fontSize = (int) Math.round((14.0d * screenRes) / 72.0d) // Java2D = 72 DPI
                if (e.font == null) {
                    e.font = g.font // a default
                }
                def fm = getFontMetrics(e.font)
                int width = fm.stringWidth(e.labelText)
                g.font = e.font
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING as RenderingHints.Key, RenderingHints.VALUE_ANTIALIAS_ON)
                if (e.textAlignment == Element.Alignment.Left) {
                    g.drawString(e.labelText, elementX, elementY + fontSize)
                } else if (e.textAlignment == Element.Alignment.Right) {
                    g.drawString(e.labelText, (elementX + elementW) - width, elementY + fontSize)
                } else if (e.textAlignment == Element.Alignment.Center) {
                    g.drawString(e.labelText, elementX + ((elementW - width) / 2) as int, elementY + fontSize as int)
                }
            }
            g.clip = clip
        }
        //        for(int i = 0 i < e.children.size() i++) {
        //            paintElement(e.children.get(i), g)
        //        }

    }

    abstract void doRepaint(Rectangle bounds)

    abstract void repaint()

    abstract FontMetrics getFontMetrics(Font font)

    /** Checks if point p is inside the bounds of any element */
    List<Element> pick(Point p) {
        List<Element> potential = new LinkedList<>()
        for (Element e in elements) {
            if (bounds(e).contains(p)) {
                potential.add(e)
            }
        }
        return potential
    }

    void select(Point p1, Point p2, boolean ctrl) {
        if (p1 && p2) {
            Rectangle originalSelectRect = new Rectangle(selectRect)
            fitRect(selectRect, p1, p2)
            for (Element e in elements) {
                if (selectRect.intersects(bounds(e))) {
                    select(e) // TODO: not perfect, I want the selection inverted as it goes over
                } else if (!ctrl) {
                    deselect(e)
                }
            }
            // This repaints the overlap a second time. A minor inefficiency...
            doRepaint(new Rectangle(originalSelectRect.x as int,
                    originalSelectRect.y as int,
                    originalSelectRect.width + 1 as int,
                    originalSelectRect.height + 1 as int))
            doRepaint(new Rectangle(selectRect.x as int, selectRect.y as int, selectRect.width + 1 as int, selectRect.height + 1 as int))
        }
    }

    /**
     * Special exceptions are handled here
     *
     * @param element
     */
    void load(Element element) {
        if (!element.file) return
        if (Element.areas.containsKey(element.file)) {
            Element p = Element.areas.get(element.file)
            p.addNode(element)
            addElement(p)
        } else if ("HudPlayerHealth".equalsIgnoreCase(element.file)) { // Better, but still not perfect
            // Move by "CHealthAccountPanel" delta_item_x" and "delta_item_start_y"
            Element p = Element.areas.get("CHealthAccountPanel")
            p.addNode(element)
            addElement(p)
        } else if ("HudAmmoWeapons".equalsIgnoreCase(element.file)) {
            Element p = Element.areas.get("HudWeaponAmmo")
            p.addNode(element)
            addElement(p)
        }
        addElement(element) // Weird but it has to be done
    }

    void addElement(Element e) {
        if (elements.contains(e)) return
        //            e.setCanvas(this)
        elements.add(e)
        doRepaint(bounds(e))
    }

    void removeElements(List<Element> remove) {
        elements.removeAll(remove)
    }

    void select(Element e) {
        if (!e) return;
        if (selectedElements.contains(e)) return
        selectedElements.add(e)
        //            if(e.children != null) {
        //                for(int i = 0 i < e.children.size() i++) {
        //                    select(e.children.get(i))
        //                }
        //            }
        doRepaint(bounds(e))
    }

    boolean isSelected(Element e) {
        return selectedElements.contains(e)
    }

    List<Element> getSelected() {
        return Collections.unmodifiableList(selectedElements)
    }

    void deselect(Element e) {
        if (!e) return;
        if (!selectedElements.contains(e)) return
        selectedElements.remove(e)
        //            if(e.children != null) {
        //                for(int i = 0 i < e.children.size() i++) {
        //                    deselect(e.children.get(i))
        //                }
        //            }
        doRepaint(bounds(e))
    }

    void deselectAll() {
        List<Element> old = selectedElements
        selectedElements = new LinkedList<>()
        for (e in old) doRepaint(bounds(e))
    }

    void translate(Element e, double dx, double dy) { // TODO: scaling (scale 5 = 5 pixels to move 1 x/y co-ord)
        //        Rectangle originalBounds = new Rectangle(e.getBounds())
        if (e.XAlignment == Element.Alignment.Right) {
            dx *= -1
        }
        double scaleX = screen.width / (double) internal.width
        dx = Math.round(dx / scaleX)
        e.localX += dx
        if (e.YAlignment == Element.VAlignment.Bottom) {
            dy *= -1
        }
        double scaleY = screen.height / (double) internal.height
        dy = Math.round(dy / scaleY)
        e.localY += dy
        //        this.doRepaint(originalBounds)
        //        this.doRepaint(e.getBounds())
        repaint() // helps
    }

    int x(Element e) {
        if (e) {
            def parent = e.parent
            int lx = e.localXi
            int _x = x(parent)
            switch (e.XAlignment) {
                case Element.Alignment.Left: return _x + (lx) as int
                case Element.Alignment.Center: return _x + (width(parent) / 2 + lx) as int
                case Element.Alignment.Right: return _x + (width(parent) - lx) as int
            }
        }
        return 0
    }

    int y(Element e) {
        if (e) {
            def parent = e.parent
            int ly = e.localYi
            int _y = y(parent)
            switch (e.YAlignment) {
                case Element.VAlignment.Top: return _y + (ly) as int
                case Element.VAlignment.Center: return _y + (height(parent) / 2 + ly) as int
                case Element.VAlignment.Bottom: return _y + (height(parent) - ly) as int
            }
        }
        return 0
    }

    int width(Element e) {
        if (!e) return internal.width
        return (e.wideMode == Element.DimensionMode.Mode1) ? (e.wide) : (width(e.parent) - e.wide)
    }

    int height(Element e) {
        if (!e) return internal.height
        return (e.tallMode == Element.DimensionMode.Mode1) ? (e.tall) : (height(e.parent) - e.tall)
    }

    Rectangle bounds(Element e) {
        def scaleX = (screen.width / internal.width) * scale
        def scaleY = (screen.height / internal.height) * scale
        return [
                Math.round(scaleX * e.localX) as int,
                Math.round(scaleY * e.localY) as int,
                Math.round(scaleX * e.wide) + 1 as int,
                Math.round(scaleY * e.tall) + 1 as int
        ] as Rectangle
    }

}
