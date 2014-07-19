package com.timepath.vgui
import com.timepath.vgui.Element.Alignment
import com.timepath.vgui.Element.VAlignment
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.logging.Log

import java.awt.*
import java.util.List
/**
 * @author TimePath
 * @see <a href="http://i.imgur.com/KxiV3.jpg">OSX</a>
 * @see <a href="http://i.imgur.com/VqABM.jpg">WIN</a>
 */
@CompileStatic
@TypeChecked
@Log('LOG')
public abstract class VGUIRenderer {

    private static final AlphaComposite SRC_OVER = AlphaComposite.SrcOver
    /** List of elements */
    final List<ElemenX> elements = new LinkedList<>()
    /** List of currently selected elements */
    final List<ElemenX> selectedElements = new LinkedList<>()
    double scale = 1
    Dimension screen
    Dimension internal
    Rectangle selectRect = new Rectangle()
    int _offX
    int _offY
    ElemenX hoveredElement

    void removeElements(List<ElemenX> remove) {
        elements.removeAll(remove)
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

    void paintElement(ElemenX e, Graphics2D g) {
        def r = this
            if ((e.getWidth() > 0) && (e.getHeight() > 0)) { // invisible? don't waste time
                int elementX = (int) Math.round(e.getX() * (r.screen.width / r.internal.width) * r.scale)
                int elementY = (int) Math.round(e.getY() * (r.screen.height / r.internal.height) * r.scale)
                int elementW = (int) Math.round(e.getWidth() * (r.screen.width / r.internal.width) * r.scale)
                int elementH = (int) Math.round(e.getHeight() * (r.screen.height / r.internal.height) * r.scale)
                if (r.selectedElements.contains(e)) {
                    elementX += r._offX
                    elementY += r._offY
                }
                Shape bounds = new Rectangle(elementX, elementY, elementW, elementH)
                Shape clip = g.getClip()
                g.setClip(bounds)
                if (e.getFgColor() != null) {
                    g.setColor(e.getFgColor())
                    float elementBgAlpha = 0.25f
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, elementBgAlpha))
                    g.fillRect(elementX, elementY, elementW - 1, elementH - 1)
                }
                if (e.getImage() != null) {
                    if (e.getFgColor() != null) {
                        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, e.getFgColor().getAlpha()))
                    }
                    g.drawImage(e.getImage(), elementX, elementY, elementW, elementH, null)
                }
                if (r.selectedElements.contains(e)) {
                    g.setColor(Color.CYAN)
                } else {
                    g.setColor(Color.GREEN)
                }
                g.drawRect(elementX, elementY, elementW - 1, elementH - 1)
                if (r.hoveredElement == e) {
                    g.setColor(new Color(255 - g.getColor().getRed(), 255 - g.getColor().getGreen(), 255 - g.getColor().getBlue()))
                    g.setComposite(SRC_OVER)
                    //                g.drawRect(elementX + offX, elementY + offY, e.getWidth() - 1, e.getHeight() - 1) // border
                    g.drawRect(elementX + 1, elementY + 1, elementW - 3, elementH - 3) // inner
                    //                g.drawRect(elementX + offX - 1, elementY + offY - 1, e.getWidth() + 1,
                    // e.getHeight() + 1) // outer
                }
                if ((e.getLabelText() != null) && !e.getLabelText().isEmpty()) {
                    if (e.getFgColor() != null) {
                        g.setColor(e.getFgColor())
                    } else {
                        g.setColor(Color.WHITE)
                    }
                    g.setComposite(SRC_OVER)
                    int screenRes = Toolkit.getDefaultToolkit().getScreenResolution()
                    int fontSize = (int) Math.round((14.0d * screenRes) / 72.0d) // Java2D = 72 DPI
                    if (e.font == null) {
                        e.font = g.font // a default
                    }
                    FontMetrics fm = getFontMetrics(e.getFont())
                    int width = fm.stringWidth(e.getLabelText())
                    g.font = e.font
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING as RenderingHints.Key, RenderingHints.VALUE_ANTIALIAS_ON)
                    if (e.textAlignment == Alignment.Left) {
                        g.drawString(e.labelText, elementX, elementY + fontSize)
                    } else if (e.textAlignment == Alignment.Right) {
                        g.drawString(e.labelText, (elementX + elementW) - width, elementY + fontSize)
                    } else if (e.textAlignment == Alignment.Center) {
                        g.drawString(e.labelText, elementX + ((elementW - width) / 2) as int, elementY + fontSize as int)
                    }
                }
                g.setClip(clip)
            }
            //        for(int i = 0 i < e.children.size() i++) {
            //            paintElement(e.children.get(i), g)
            //        }

    }

    // Checks if poing p is inside the bounds of any element
    List<ElemenX> pick(Point p, List<ElemenX> elements) {
        List<ElemenX> potential = new LinkedList<>()
        for (ElemenX e in elements) {
                if (e.bounds.contains(p)) {
                    potential.add(e)
                }
        }
        return potential
    }

    abstract void doRepaint(Rectangle bounds)

    abstract void repaint()

    abstract FontMetrics getFontMetrics(Font font)

    void select(Point p1, Point p2, boolean ctrl) {
        if ((p1 != null) && (p2 != null)) {
            Rectangle originalSelectRect = new Rectangle(selectRect)
            selectRect = fitRect(p1, p2, selectRect)
            for (ElemenX e : elements) {
                    if (selectRect.intersects(e.bounds)) {
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

    private static Rectangle fitRect(Point p1, Point p2, Rectangle r) {
        Rectangle result = new Rectangle(r)
        result.@x = Math.min(p1.@x, p2.@x)
        result.@y = Math.min(p1.@y, p2.@y)
        result.@width = Math.abs(p2.@x - p1.@x)
        result.@height = Math.abs(p2.@y - p1.@y)
        return result
    }

    public void select(ElemenX e) {
            if (e != null) {
                if (selectedElements.contains(e)) {
                    return
                }
                selectedElements.add(e)
                //            if(e.children != null) {
                //                for(int i = 0 i < e.children.size() i++) {
                //                    select(e.children.get(i))
                //                }
                //            }
                doRepaint(e.bounds)
            }
    }

    void deselect(ElemenX e) {
            if (e != null) {
                if (!selectedElements.contains(e)) {
                    return
                }
                selectedElements.remove(e)
                //            if(e.children != null) {
                //                for(int i = 0 i < e.children.size() i++) {
                //                    deselect(e.children.get(i))
                //                }
                //            }
                doRepaint(e.bounds)
            }
    }

    void translate(ElemenX e, double dx, double dy) { // TODO: scaling (scale 5 = 5 pixels to move 1 x/y co-ord)
        //        Rectangle originalBounds = new Rectangle(e.getBounds())
        if (e.XAlignment == Alignment.Right) {
            dx *= -1
        }
        double scaleX = screen.width / (double) internal.width
        dx = Math.round(dx / scaleX)
        e.localX += dx
        if (e.YAlignment == VAlignment.Bottom) {
            dy *= -1
        }
        double scaleY = screen.height / (double) internal.height
        dy = Math.round(dy / scaleY)
        e.localY += dy
        //        this.doRepaint(originalBounds)
        //        this.doRepaint(e.getBounds())
        repaint() // helps
    }

    boolean isSelected(ElemenX e) {
        return selectedElements.contains(e)
    }

    void deselectAll() {
            Iterable<ElemenX> temp = new ArrayList<>(selectedElements)
            selectedElements.clear()
            for (ElemenX temp1 : temp) {
                doRepaint(temp1.bounds)
            }
    }

    /**
     * Special exceptions are handled here
     *
     * @param element
     */
    public void load(Element element) {
        if (element.file == null) {
            return
        }
        if (Element.areas.containsKey(element.file)) {
            Element p = Element.areas.get(element.file)
            p.addNode(element)
            addElement(p)
        } else if ("HudPlayerHealth".equalsIgnoreCase(element.file)) { // better, but still not perfect
            // move by "CHealthAccountPanel" delta_item_x" and "delta_item_start_y"
            Element p = Element.areas.get("CHealthAccountPanel")
            p.addNode(element)
            addElement(p)
        } else if ("HudAmmoWeapons".equalsIgnoreCase(element.file)) {
            Element p = Element.areas.get("HudWeaponAmmo")
            p.addNode(element)
            addElement(p)
        }
        addElement(element) // weird but it has to be done
    }

    void addElement(Element e) {
        def x = new ElemenX(e)
        if (!elements.contains(x)) {
            x.load()
            //            x.setCanvas(this)
            elements.add(x)
            doRepaint(x.bounds)
        }
    }

    public List<ElemenX> getSelected() {
        return Collections.unmodifiableList(selectedElements)
    }

    class ElemenX {
        @Delegate Element e

        ElemenX(Element p) { e = p }

        ElemenX getParent() { new ElemenX(e.parent) }

        int getX() {
                if ((this.parent == null) || this.parent.name.replaceAll("\"", "").endsWith(".res")) {
                    if (this.XAlignment == Alignment.Center) {
                        return this.localX + (internal.width / 2)
                    } else {
                        return (this.XAlignment == Alignment.Right) ? (internal.width - this.localX) : this.localX
                    }
                } else {
                    int x
                    if (this.XAlignment == Alignment.Center) {
                        x = (this.parent.width / 2 + this.localX) as int
                    } else {
                        x = (this.XAlignment == Alignment.Right ? (this.parent.width - this.localX) : this.localX)
                    }
                    return x + this.parent.x
                }
        }

        int getY() {
                if ((this.parent == null) || this.parent.name.replaceAll("\"", "").endsWith(".res")) {
                    if (this.YAlignment == Alignment.Center) {
                        return this.localY + (internal.height / 2)
                    } else {
                        return (this.YAlignment == Alignment.Right) ? (internal.height - this.localY) : this.localY
                    }
                }
                int y
                if (this.YAlignment == Alignment.Center) {
                    y = (this.parent.height / 2 + this.localY) as int
                } else {
                    y = ((this.YAlignment == Alignment.Right) ? (this.parent.height - this.localY) : this.localY) as int
                }
                return y + this.parent.y
        }

        int getWidth() {
                return (this.wideMode == Element.DimensionMode.Mode2) ? ((parent != null)
                        ? (parent.width - this.wide)
                        : (internal.width - this.wide)) : this.wide
        }

        int getHeight() {
                return (this.tallMode == Element.DimensionMode.Mode2) ? ((parent != null)
                        ? (parent.height - this.tall)
                        : (internal.height - this.tall)) : this.tall
        }

        Rectangle getBounds() {
                int minX = (int) Math.round(this.localX * (screen.width / internal.width) * scale)
                int minY = (int) Math.round(this.localY * (screen.height / internal.height) * scale)
                int maxX = (int) Math.round(this.wide * (screen.width / internal.width) * scale)
                int maxY = (int) Math.round(this.tall * (screen.height / internal.height) * scale)
                return new Rectangle(minX, minY, maxX + 1, maxY + 1)
        }
    }

}
