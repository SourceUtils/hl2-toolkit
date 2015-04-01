package com.timepath.vgui

import java.awt.*
import java.awt.image.BufferedImage
import java.io.InputStream
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.LinkedList
import kotlin.properties.Delegates

/**
 * @author TimePath
 * @see <a href="http://i.imgur.com/KxiV3.jpg">OSX</a>
 * @see <a href="http://i.imgur.com/VqABM.jpg">WIN</a>
 */
public abstract class VGUIRenderer {

    public fun paintElement(e: Element, g: Graphics2D) {
        val r = this
        if (width(e) != 0 && height(e) != 0) {
            // invisible? don't waste time
            var elementX = Math.round(x(e).toDouble() * (r.screen.getWidth() / r.internal.getWidth()) * r.scale).toInt()
            var elementY = Math.round(y(e).toDouble() * (r.screen.getHeight() / r.internal.getHeight()) * r.scale).toInt()
            val elementW = Math.round(width(e).toDouble() * (r.screen.getWidth() / r.internal.getWidth()) * r.scale).toInt()
            val elementH = Math.round(height(e).toDouble() * (r.screen.getHeight() / r.internal.getHeight()) * r.scale).toInt()
            if (e in r.selectedElements) {
                elementX += r.dragX
                elementY += r.dragY
            }

            val bounds = Rectangle(elementX, elementY, elementW, elementH)
            val clip = g.getClip()
            g.setClip(bounds)
            if (e.fgColor != null) {
                g.setColor(e.fgColor)
                val elementBgAlpha = 0.25f
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, elementBgAlpha))
                g.fillRect(elementX, elementY, elementW - 1, elementH - 1)
            }

            if (e.image != null) {
                if (e.fgColor != null) {
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, e.fgColor!!.getAlpha().toFloat()))
                }

                g.drawImage(e.image, elementX, elementY, elementW, elementH, null)
            }

            if (e in r.selectedElements) {
                g.setColor(Color.CYAN)
            } else {
                g.setColor(Color.GREEN)
            }

            g.drawRect(elementX, elementY, elementW - 1, elementH - 1)
            if (e == r.hoveredElement) {
                g.setColor(Color(255 - g.getColor().getRed(), 255 - g.getColor().getGreen(), 255 - g.getColor().getBlue()))
                g.setComposite(SRC_OVER)
                //                g.drawRect(elementX + offX, elementY + offY, e.getWidth() - 1, e.getHeight() - 1) // border
                g.drawRect(elementX + 1, elementY + 1, elementW - 3, elementH - 3)// inner
                //                g.drawRect(elementX + offX - 1, elementY + offY - 1, e.getWidth() + 1,
                // e.getHeight() + 1) // outer
            }

            val labelText = e.labelText
            if ((labelText != null) && !labelText.isEmpty()) {
                if (e.fgColor != null) {
                    g.setColor(e.fgColor)
                } else {
                    g.setColor(Color.WHITE)
                }

                g.setComposite(SRC_OVER)
                val screenRes = Toolkit.getDefaultToolkit().getScreenResolution()
                val fontSize = Math.round(14.0 * screenRes.toDouble() / 72.0).toInt()// Java2D = 72 DPI
                if (e.font == null) {
                    e.font = g.getFont()// a default
                }

                val fm = getFontMetrics(e.font!!)
                val width = fm.stringWidth(labelText)
                g.setFont(e.font)
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
                if (e.textAlignment == Element.Alignment.Left) {
                    g.drawString(labelText, elementX, elementY + fontSize)
                } else if (e.textAlignment == Element.Alignment.Right) {
                    g.drawString(labelText, elementX + elementW - width, elementY + fontSize)
                } else if (e.textAlignment == Element.Alignment.Center) {
                    g.drawString(labelText, elementX + ((elementW - width) / 2), elementY + fontSize)
                }

            }

            g.setClip(clip)
        }

        //        for(int i = 0 i < e.children.size() i++) {
        //            paintElement(e.children.get(i), g)
        //        }

    }

    public abstract fun doRepaint(bounds: Rectangle)

    public abstract fun repaint()

    public abstract fun getFontMetrics(font: Font): FontMetrics

    /**
     * Checks if point p is inside the bounds of any element
     */
    public fun pick(p: Point): List<Element> {
        val potential = LinkedList<Element>()
        for (e in elements) {
            if (p in bounds(e)) {
                potential.add(e)
            }

        }

        return potential
    }

    public fun select(p1: Point, p2: Point, ctrl: Boolean) {
        val originalSelectRect = Rectangle(selectRect)
        fitRect(selectRect, p1, p2)
        for (e in elements) {
            if (selectRect.intersects(bounds(e))) {
                select(e)// TODO: not perfect, I want the selection inverted as it goes over
            } else if (!ctrl) {
                deselect(e)
            }

        }

        // This repaints the overlap a second time. A minor inefficiency...
        doRepaint(Rectangle(originalSelectRect.getX().toInt(), (originalSelectRect.getY()).toInt(), (originalSelectRect.getWidth() + 1).toInt(), (originalSelectRect.getHeight() + 1).toInt()))
        doRepaint(Rectangle(selectRect.getX().toInt(), (selectRect.getY()).toInt(), (selectRect.getWidth() + 1).toInt(), (selectRect.getHeight() + 1).toInt()))
    }

    /**
     * Special exceptions are handled here
     */
    public fun load(element: Element) {
        if (element.file == null) return

        if (Element.areas.containsKey(element.file)) {
            val p = Element.areas[element.file]!!
            p.addNode(element)
            addElement(p)
        } else if ("HudPlayerHealth".equalsIgnoreCase(element.file!!)) {
            // Better, but still not perfect
            // Move by "CHealthAccountPanel" delta_item_x" and "delta_item_start_y"
            val p = Element.areas["CHealthAccountPanel"]!!
            p.addNode(element)
            addElement(p)
        } else if ("HudAmmoWeapons".equalsIgnoreCase(element.file!!)) {
            val p = Element.areas["HudWeaponAmmo"]!!
            p.addNode(element)
            addElement(p)
        }

        addElement(element)// Weird but it has to be done
    }

    public fun addElement(e: Element) {
        if (e in elements) return

        //            e.setCanvas(this)
        elements.add(e)
        doRepaint(bounds(e))
    }

    public fun removeElements(remove: List<Element>) {
        elements.removeAll(remove)
    }

    public fun select(e: Element?) {
        if (e == null) return

        if (e in selectedElements) return

        selectedElements.add(e)
        //            if(e.children != null) {
        //                for(int i = 0 i < e.children.size() i++) {
        //                    select(e.children.get(i))
        //                }
        //            }
        doRepaint(bounds(e))
    }

    public fun isSelected(e: Element): Boolean {
        return e in selectedElements
    }

    public val selected: List<Element>
        get() = Collections.unmodifiableList<Element>(selectedElements)

    public fun deselect(e: Element?) {
        if (e == null) return

        if (!(e in selectedElements)) return

        selectedElements.remove(e)
        //            if(e.children != null) {
        //                for(int i = 0 i < e.children.size() i++) {
        //                    deselect(e.children.get(i))
        //                }
        //            }
        doRepaint(bounds(e))
    }

    public fun deselectAll() {
        val old = selectedElements
        selectedElements = LinkedList<Element>()
        for (e in old) doRepaint(bounds(e))
    }

    public fun translate(e: Element, dx: Double, dy: Double) {
        // TODO: scaling (scale 5 = 5 pixels to move 1 x/y co-ord)
        [suppress("NAME_SHADOWING")]
        var dx = dx
        //        Rectangle originalBounds = new Rectangle(e.getBounds())
        if (e.XAlignment == Element.Alignment.Right) {
            dx *= (-1).toDouble()
        }
        val scaleX = screen.getWidth() / internal.getWidth()
        dx = Math.round(dx / scaleX).toDouble()
        e.localX = e.localX + dx

        [suppress("NAME_SHADOWING")]
        var dy = dy
        if (e.YAlignment == Element.VAlignment.Bottom) {
            dy *= (-1).toDouble()
        }
        val scaleY = screen.getHeight() / internal.getHeight()
        dy = Math.round(dy / scaleY).toDouble()
        e.localY = e.localY + dy
        //        this.doRepaint(originalBounds)
        //        this.doRepaint(e.getBounds())
        repaint()// helps
    }

    public fun x(e: Element?): Int {
        if (e != null) {
            val parent = e.parent
            val lx = e.localXi
            val _x = x(parent)
            when (e.XAlignment) {
                Element.Alignment.Left -> return (_x + (lx))
                Element.Alignment.Center -> return (_x + (width(parent) / 2 + lx))
                Element.Alignment.Right -> return (_x + (width(parent) - lx))
            }
        }

        return 0
    }

    public fun y(e: Element?): Int {
        if (e != null) {
            val parent = e.parent
            val ly = e.localYi
            val _y = y(parent)
            when (e.YAlignment) {
                Element.VAlignment.Top -> return (_y + (ly))
                Element.VAlignment.Center -> return (_y + (height(parent) / 2 + ly))
                Element.VAlignment.Bottom -> return (_y + (height(parent) - ly))
            }
        }

        return 0
    }

    public fun width(e: Element?): Int {
        if (e == null)
            return internal.getWidth().toInt()
        return if ((e.wideMode == Element.DimensionMode.Mode1)) (e.wide) else (width(e.parent) - e.wide)
    }

    public fun height(e: Element?): Int {
        if (e == null)
            return internal.getHeight().toInt()
        return if ((e.tallMode == Element.DimensionMode.Mode1)) (e.tall) else (height(e.parent) - e.tall)
    }

    public fun bounds(e: Element): Rectangle {
        val scaleX = (screen.getWidth() / internal.getWidth()) * scale
        val scaleY = (screen.getHeight() / internal.getHeight()) * scale
        return Rectangle(Math.round(scaleX * e.localX).toInt(), Math.round(scaleY * e.localY).toInt(), Math.round(scaleX * e.wide.toDouble()).toInt() + 1, Math.round(scaleY * e.tall.toDouble()).toInt() + 1)
    }

    public fun getSelectedElements(): List<Element> {
        return selectedElements
    }

    public fun setSelectedElements(selectedElements: MutableList<Element>) {
        this.selectedElements = selectedElements
    }

    var elementImage: BufferedImage? = null
        get() {
            val img = BufferedImage(screen.getWidth().toInt(), screen.getHeight().toInt(), BufferedImage.TYPE_INT_ARGB)
            val g = img.createGraphics()
            Collections.sort<Element>(elements, LAYER_SORT)
            for (element in elements) {
                g.setComposite(SRC_OVER)
                paintElement(element, g)
            }

            g.dispose()
            $elementImage = ImageUtils.toCompatibleImage(img)
            return $elementImage!!
        }
    /**
     * List of elements
     */
    public val elements: LinkedList<Element> = LinkedList()
    /**
     * List of currently selected elements
     */
    private var selectedElements: MutableList<Element> = LinkedList()
    /**
     * Render scale
     */
    public var scale: Double = 1.0
    /**
     * Screen space
     */
    public var screen: Dimension by Delegates.notNull()
    /**
     * Virtual space
     */
    public var internal: Dimension by Delegates.notNull()
    /**
     * Selection
     */
    public var selectRect: Rectangle = Rectangle()
    /**
     * Drag X displacement
     */
    public var dragX: Int = 0
    /**
     * Drag Y displacement
     */
    public var dragY: Int = 0
    /**
     * Currently hoverer
     */
    public var hoveredElement: Element? = null

    public abstract class ResourceLocator {
        /**
         * Locate a resource
         *
         * @return an InputStream to the resource, or null
         */
        public abstract fun locate(path: String): InputStream?

        public abstract fun locateImage(path: String): Image?
    }

    companion object {

        public fun registerLocator(locator: ResourceLocator) {
            locators.add(locator)
        }

        public fun locate(path: String): InputStream? {
            for (locator in locators) {
                val result = locator.locate(path)
                if (result != null)
                    return result
            }

            return null
        }

        public fun locateImage(name: String): Image? {
            for (locator in locators) {
                val result = locator.locateImage(name)
                if (result != null)
                    return result
            }

            return null
        }

        private fun fitRect(r: Rectangle, p1: Point, p2: Point) {
            r.x = Math.min(p1.x, p2.x)
            r.y = Math.min(p1.y, p2.y)
            r.width = Math.abs(p2.x - p1.x)
            r.height = Math.abs(p2.y - p1.y)
        }

        /**
         * Finds the greatest common multiple
         */
        private fun gcm(a: Long, b: Long): Long {
            return if ((b == 0L)) a else gcm(b, a % b)
        }

        private val SRC_OVER = AlphaComposite.SrcOver
        private val LAYER_SORT = object : Comparator<Element> {
            override fun compare(a: Element, b: Element): Int {
                return a.layer - b.layer
            }
        }
        private val locators = ArrayList<ResourceLocator>()
    }
}
