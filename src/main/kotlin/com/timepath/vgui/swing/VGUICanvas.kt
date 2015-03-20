package com.timepath.vgui.swing

import com.timepath.steam.SteamUtils
import com.timepath.steam.io.VDFNode
import com.timepath.steam.io.VDFNode.VDFProperty
import com.timepath.vgui.Element
import com.timepath.vgui.ImageUtils
import com.timepath.vgui.VGUIRenderer
import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.ArrayList
import java.util.Arrays
import java.util.LinkedList
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.SwingUtilities
import javax.swing.WindowConstants
import kotlin.platform.platformStatic

/**
 * @author TimePath
 */
SuppressWarnings("serial")
public class VGUICanvas : JPanel(), MouseListener, MouseMotionListener {

    public val elements: LinkedList<Element> get() = r!!.elements

    init {
        addMouseListener(this)
        addMouseMotionListener(this)
        setPreferredSize(Dimension(640, 480))
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) {
                resize()
            }

        })
    }

    private fun resize() {
        offX = ((getWidth() - r!!.internal.width) / 2).toInt()
        offY = ((getHeight() - r!!.internal.height) / 2).toInt()
        repaint()
    }

    /**
     * Fired when an element has been dropped
     */
    protected fun placed() {
    }

    public fun setBackgroundImage(background: Image) {
        this.background = background
        prepareImage(background, this)
        repaint()
    }

    private fun getOutliers(): Rectangle {
        val r = r!!
        val rect = Rectangle(r.internal.width, r.internal.height)
        for (elem in r.elements) rect.add(r.bounds(elem))
        return rect
    }

    /**
     * Convenience method for repainting the bare minimum
     */
    private fun doRepaint(bounds: Rectangle) {
        r!!.elementImage = null
        repaint(offX + bounds.x, offY + bounds.y, bounds.width.toInt() - 1, bounds.height.toInt() - 1)
    }

    private fun doRepaint1(bounds: Rectangle) {
        r!!.elementImage = null
        repaint(offX + bounds.x, offY + bounds.y, bounds.width, bounds.height)
    }

    private fun hover(e: Element) {
        if (e == r!!.hoveredElement) return
        // Nothing to do
        // Clean up if needed
        if (r!!.hoveredElement != null)
            doRepaint(r!!.bounds(r!!.hoveredElement!!))
        // Draw the new element if needed
        r!!.hoveredElement = e
        if (r != null)
            doRepaint(r!!.bounds(e))
    }

    /**
     * As soon as the height drops below 480, stops rendering
     */
    private fun drawGrid(): BufferedImage {
        val img = BufferedImage(r!!.screen.width, r!!.screen.height, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setComposite(GRID_AC)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED)
        g.setColor(GRID_COLOR)
        val w = r!!.screen.width
        val h = r!!.screen.height
        val minGridSpacing = 10
        val i = minGridSpacing
        if (i < 0) return img
        if (i < 2) {
            // Optimize for small numbers, stop division by zero
            g.fillRect(0, 0, r!!.screen.width, r!!.screen.height)
            return img
        }

        val cross = 0
        val maxX = w - (w % i)
        val maxY = h - (h % i)
        val multX = r!!.screen.getWidth() / r!!.internal.getWidth()
        val multY = r!!.screen.getHeight() / r!!.internal.getHeight()
        for (y in -1..(maxY / i)) {
            for (x in -1..(maxX / i)) {
                val dx = Math.round((maxX.toDouble() * x.toDouble() * i.toDouble() * multX).toDouble() / maxX.toDouble()).toInt()
                val dy = Math.round((maxY.toDouble() * y.toDouble() * i.toDouble() * multY).toDouble() / maxY.toDouble()).toInt()
                g.drawLine(dx + cross, dy + cross, dx.toInt() - (1 + cross), dy.toInt() - (1 + cross))
                g.drawLine(dx.toInt() - (1 + cross), dy + cross, dx + cross, dy.toInt() - (1 + cross))
            }

        }

        g.dispose()
        return img
    }

    override fun setPreferredSize(preferredSize: Dimension) {
        val UISize = Dimension(preferredSize.width + (2 * offX), preferredSize.height + (2 * offY))
        super<JPanel>.setPreferredSize(UISize)
        currentbg = null
        gridbg = null
        r!!.screen = preferredSize
        //        long gcm = gcm(hudRes.width, hudRes.height)
        val resX = r!!.screen.getWidth()
        val resY = r!!.screen.getHeight()
        val m = resX / resY
        //        System.out.println(resX + "/" + resY + "=" + m)
        //        System.out.println((resX / gcm) + ":" + (resY / gcm) + " = " + Math.round(m * 480) + "x" + 480)
        r!!.internal = Dimension(Math.round(m.toDouble() * 480).toInt(), 480)
        repaint()
    }

    override fun paintComponent(graphics: Graphics) {
        val g = graphics as Graphics2D
        g.setColor(BG_COLOR)
        g.fillRect(0, 0, getWidth(), getHeight())
        if (background != null) {
            if (currentbg != null)
                currentbg = ImageUtils.toCompatibleImage(ImageUtils.resizeImage(background!!, r!!.screen.width, r!!.screen.height))
            g.drawImage(currentbg, offX, offY, this)
        } else {
            g.setColor(Color.WHITE.darker().darker())
            g.fillRect(offX, offY, Math.round(r!!.screen.getWidth().toDouble() * r!!.scale).toInt(), Math.round(r!!.screen.getHeight().toDouble() * r!!.scale).toInt())
        }

        if (gridbg == null)
            gridbg = ImageUtils.toCompatibleImage(drawGrid())
        g.drawImage(gridbg, offX, offY, this)
        g.drawImage(r!!.elementImage, offX, offY, this)
        if (dragSelecting) {
            g.setComposite(SELECT_AC)
            g.setColor(Color.CYAN.darker())
            g.fillRect(offX + r!!.selectRect.x + 1, offY + r!!.selectRect.y + 1, r!!.selectRect.width.toInt() - 2, r!!.selectRect.height.toInt() - 2)
            g.setColor(Color.BLUE)
            g.drawRect(offX + r!!.selectRect.x, offY + r!!.selectRect.y, r!!.selectRect.width.toInt() - 1, r!!.selectRect.height.toInt() - 1)
        }

    }

    override fun mouseDragged(e: MouseEvent) {
        val p = e.getPoint()
        p.translate(-offX, -offY)// Localize
        if (SwingUtilities.isLeftMouseButton(e)) {
            if (dragSelecting) {
                r!!.select(dragStart!!, p, e.isControlDown())
            } else if (dragMoving) {
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR))
                r!!.dragX = r!!.dragX.toInt() + p.x - dragStart!!.x
                r!!.dragY = r!!.dragY.toInt() + p.y - dragStart!!.y
                r!!.elementImage = null
                repaint()
                dragStart = p
            }

        }

    }

    override fun mouseMoved(e: MouseEvent) {
        val p = e.getPoint()
        p.translate(-offX, -offY)// Localize
        hover(chooseBest(r!!.pick(p))!!)
    }

    override fun mousePressed(e: MouseEvent) {
        val p = e.getPoint()
        p.translate(-offX, -offY)// Localize
        if (SwingUtilities.isLeftMouseButton(e)) {
            dragStart = Point(p.x, p.y)
            r!!.selectRect.setSize(p.x, p.y)
            if (r!!.hoveredElement == null) {
                // Clicked nothing
                if (!e.isControlDown()) r!!.deselectAll()
                dragSelecting = true
                dragMoving = false
            } else {
                // Hovering over something
                dragSelecting = false
                dragMoving = true
                if (e.isControlDown()) {
                    // Toggle select
                    if (r!!.isSelected(r!!.hoveredElement!!)) {
                        r!!.deselect(r!!.hoveredElement)
                    } else {
                        r!!.select(r!!.hoveredElement)
                    }

                } else {
                    // If the thing I'm hovering isn't selected already, drop all selections
                    if (!r!!.isSelected(r!!.hoveredElement!!)) {
                        r!!.deselectAll()
                        r!!.select(r!!.hoveredElement)
                    }

                }

            }

        }

    }

    override fun mouseReleased(e: MouseEvent) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            setCursor(Cursor.getDefaultCursor())
            if (dragMoving) placed()// Release element
            dragSelecting = false
            dragMoving = false
            dragStart = null
            val original = Rectangle(r!!.selectRect)
            r!!.selectRect.setSize(0, 0)
            for (elem in r!!.getSelectedElements()) {
                // ???
                if (r!!.getSelectedElements().contains(elem.parent) && !elem.parent!!.name!!.replaceAll("\"", "").endsWith(".res")) {
                    // XXX: hacky
                    continue
                }

                r!!.translate(elem, r!!.dragX.toDouble(), r!!.dragY.toDouble())
            }

            r!!.dragX = 0
            r!!.dragY = 0
            doRepaint1(original)
        }

    }

    override fun mouseClicked(e: MouseEvent) {
    }

    override fun mouseEntered(e: MouseEvent) {
    }

    override fun mouseExited(e: MouseEvent) {
    }

    private var background: Image? = null
    private var currentbg: BufferedImage? = null
    private var gridbg: BufferedImage? = null
    private val BG_COLOR = Color.GRAY
    private val GRID_COLOR = Color.WHITE
    /**
     * Left
     */
    private var offX = 10
    /**
     * Top
     */
    private var offY = 10
    /**
     * Initial drag point
     */
    private var dragStart: Point? = null
    private var dragSelecting: Boolean = false
    private var dragMoving: Boolean = false
    public var r: VGUIRenderer? = object : VGUIRenderer() {
        override fun doRepaint(bounds: Rectangle) {
            this@VGUICanvas.doRepaint(bounds)
        }

        override fun repaint() {
            this@VGUICanvas.repaint()
        }

        override fun getFontMetrics(font: Font): FontMetrics {
            return this@VGUICanvas.getFontMetrics(font)
        }

    }

    companion object {

        throws(javaClass<IOException>())
        public platformStatic fun main(args: Array<String>) {
            val canvas = VGUICanvas()
            val root = VDFNode("Root")
            var node = VDFNode("Test")
            root.addNode(node)
            fun p(k: String, v: Any): VDFNode.VDFProperty {
                return VDFNode.VDFProperty(k, v)
            }
            node.addAllProperties(ArrayList(Arrays.asList<VDFProperty>(p("enabled", 1), p("visible", 1), p("xpos", 5), p("ypos", 5), p("zpos", 0), p("wide", 100), p("tall", 100), p("labeltext", "test"), p("textalignment", "center"), p("controlname", "label"))))
            node = VDFNode(FileInputStream(File(SteamUtils.getSteam(), "resource/FileOpenDialog.res")), StandardCharsets.UTF_8)
            for (parent in node.getNodes()) {
                for (n in parent.getNodes()) {
                    val e = Element.importVdf(n)
                    canvas.r!!.addElement(e)
                }
            }
            val f = JFrame()
            f.setContentPane(canvas)
            f.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
            f.pack()
            f.setLocationRelativeTo(null)
            f.setVisible(true)
        }

        private fun chooseBest(potential: List<Element>): Element? {
            var smallest: Element? = null
            for (elem in potential) {
                if (smallest == null) {
                    smallest = elem
                } else if (elem.layer > smallest!!.layer) {
                    // Sort by layer, then by size
                    smallest = elem
                } else if (elem.layer == smallest!!.layer && elem.size < smallest!!.size) {
                    smallest = elem
                }

            }

            return smallest
        }

        private val GRID_AC = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f)
        private val SELECT_AC = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f)
    }
}

fun main(args: Array<String>) = VGUICanvas.main(args)
