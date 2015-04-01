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

    public val r: VGUIRenderer = object : VGUIRenderer() {
        override fun doRepaint(bounds: Rectangle) = this@VGUICanvas.doRepaint(bounds)
        override fun repaint() = this@VGUICanvas.repaint()
        override fun getFontMetrics(font: Font) = this@VGUICanvas.getFontMetrics(font)
    }

    public val elements: LinkedList<Element> get() = r.elements

    override fun mouseClicked(e: MouseEvent) = Unit
    override fun mouseEntered(e: MouseEvent) = Unit
    override fun mouseExited(e: MouseEvent) = Unit

    /** Fired when an element has been dropped */
    protected fun placed(): Unit = Unit

    init {
        addMouseListener(this)
        addMouseMotionListener(this)
        setPreferredSize(Dimension(640, 480))
        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent) = resize()
        })
    }

    private fun resize() {
        offX = ((getWidth() - r.internal.width) / 2).toInt()
        offY = ((getHeight() - r.internal.height) / 2).toInt()
        repaint()
    }

    public fun setBackgroundImage(background: Image) {
        this.background = background
        prepareImage(background, this)
        repaint()
    }

    private fun getOutliers(): Rectangle {
        val rect = Rectangle(r.internal.width, r.internal.height)
        r.elements.forEach { rect.add(r.bounds(it)) }
        return rect
    }

    /** Convenience method for repainting the bare minimum */
    internal fun doRepaint(bounds: Rectangle, add: Int = 0) {
        r.elementImage = null
        repaint(offX + bounds.x, offY + bounds.y, bounds.width - 1 + add, bounds.height - 1 + add)
    }

    private fun hover(elem: Element?) {
        val current = r.hoveredElement
        if (elem == current) return
        r.hoveredElement = elem
        current?.let { doRepaint(r.bounds(it)) }
        elem?.let { doRepaint(r.bounds(it)) }
    }

    /** As soon as the height drops below 480, stops rendering */
    private fun drawGrid(): BufferedImage {
        val img = BufferedImage(r.screen.width, r.screen.height, BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        g.setComposite(GRID_AC)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED)
        g.setColor(GRID_COLOR)
        val w = r.screen.width
        val h = r.screen.height
        val minGridSpacing = 10
        val i = minGridSpacing
        if (i < 0) return img
        if (i < 2) {
            // Optimize for small numbers, stop division by zero
            g.fillRect(0, 0, r.screen.width, r.screen.height)
            return img
        }

        val cross = 0
        val maxX = w - (w % i)
        val maxY = h - (h % i)
        val multX = r.screen.getWidth() / r.internal.getWidth()
        val multY = r.screen.getHeight() / r.internal.getHeight()
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
        r.screen = preferredSize
        //        long gcm = gcm(hudRes.width, hudRes.height)
        val resX = r.screen.getWidth()
        val resY = r.screen.getHeight()
        val m = resX / resY
        //        System.out.println(resX + "/" + resY + "=" + m)
        //        System.out.println((resX / gcm) + ":" + (resY / gcm) + " = " + Math.round(m * 480) + "x" + 480)
        r.internal = Dimension(Math.round(m.toDouble() * 480).toInt(), 480)
        repaint()
    }

    override fun paintComponent(g: Graphics) = (g as Graphics2D).let { g ->
        g.setColor(BG_COLOR)
        g.fillRect(0, 0, getWidth(), getHeight())
        if (background != null) {
            if (currentbg != null)
                currentbg = ImageUtils.toCompatibleImage(ImageUtils.resizeImage(background!!, r.screen.width, r.screen.height))
            g.drawImage(currentbg, offX, offY, this)
        } else {
            g.setColor(Color.WHITE.darker().darker())
            g.fillRect(offX, offY, Math.round(r.screen.getWidth().toDouble() * r.scale).toInt(), Math.round(r.screen.getHeight().toDouble() * r.scale).toInt())
        }

        if (gridbg == null)
            gridbg = ImageUtils.toCompatibleImage(drawGrid())
        g.drawImage(gridbg, offX, offY, this)
        g.drawImage(r.elementImage, offX, offY, this)
        if (dragSelecting) {
            g.setComposite(SELECT_AC)
            g.setColor(Color.CYAN.darker())
            g.fillRect(offX + r.selectRect.x + 1, offY + r.selectRect.y + 1, r.selectRect.width.toInt() - 2, r.selectRect.height.toInt() - 2)
            g.setColor(Color.BLUE)
            g.drawRect(offX + r.selectRect.x, offY + r.selectRect.y, r.selectRect.width.toInt() - 1, r.selectRect.height.toInt() - 1)
        }
    }

    override fun mouseDragged(e: MouseEvent) {
        val p = e.getPoint().let {
            it.translate(-offX, -offY) // Localize
            it
        }
        if (SwingUtilities.isLeftMouseButton(e)) {
            if (dragSelecting) {
                r.select(dragStart!!, p, e.isControlDown())
            } else if (dragMoving) {
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR))
                r.dragX = r.dragX.toInt() + p.x - dragStart!!.x
                r.dragY = r.dragY.toInt() + p.y - dragStart!!.y
                r.elementImage = null
                repaint()
                dragStart = p
            }
        }
    }

    override fun mouseMoved(e: MouseEvent) {
        val elem = r.pick(e.getPoint().let {
            it.translate(-offX, -offY) // Localize
            it
        }).let { chooseBest(it) }
        hover(elem)
    }

    override fun mousePressed(e: MouseEvent) {
        val p = e.getPoint().let {
            it.translate(-offX, -offY) // Localize
            it
        }
        if (SwingUtilities.isLeftMouseButton(e)) {
            dragStart = Point(p.x, p.y)
            r.selectRect.setSize(p.x, p.y)
            if (r.hoveredElement == null) {
                // Clicked nothing
                if (!e.isControlDown()) r.deselectAll()
                dragSelecting = true
                dragMoving = false
            } else {
                // Hovering over something
                dragSelecting = false
                dragMoving = true
                if (e.isControlDown()) {
                    // Toggle select
                    if (r.isSelected(r.hoveredElement!!)) {
                        r.deselect(r.hoveredElement)
                    } else {
                        r.select(r.hoveredElement)
                    }

                } else {
                    // If the thing I'm hovering isn't selected already, drop all selections
                    if (!r.isSelected(r.hoveredElement!!)) {
                        r.deselectAll()
                        r.select(r.hoveredElement)
                    }
                }
            }
        }
    }

    override fun mouseReleased(e: MouseEvent) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            setCursor(Cursor.getDefaultCursor())
            if (dragMoving) placed() // Release element
            dragSelecting = false
            dragMoving = false
            dragStart = null
            val original = Rectangle(r.selectRect)
            r.selectRect.setSize(0, 0)
            for (elem in r.getSelectedElements()) {
                // ???
                if (elem.parent in r.getSelectedElements() && !elem.parent!!.name!!.replaceAll("\"", "").endsWith(".res")) {
                    // XXX: hacky
                    continue
                }
                r.translate(elem, r.dragX.toDouble(), r.dragY.toDouble())
            }
            r.dragX = 0
            r.dragY = 0
            doRepaint(original, 1)
        }
    }


    private var background: Image? = null
    private var currentbg: BufferedImage? = null
    private var gridbg: BufferedImage? = null
    private val BG_COLOR = Color.GRAY
    private val GRID_COLOR = Color.WHITE
    /** Left */
    private var offX = 10
    /** Top */
    private var offY = 10
    /** Initial drag point */
    private var dragStart: Point? = null
    private var dragSelecting: Boolean = false
    private var dragMoving: Boolean = false

    companion object {

        public platformStatic fun main(args: Array<String>) {
            val canvas = VGUICanvas()
            val root = VDFNode("Root")
            VDFNode("Test").let {
                fun p(k: String, v: Any) = VDFNode.VDFProperty(k, v)
                it.addAllProperties(listOf(
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
                ))
                it
            }.let {
                root.addNode(it)
            }
            FileInputStream(File(SteamUtils.getSteam(), "resource/FileOpenDialog.res")).use {
                VDFNode(it, Charsets.UTF_8).let {
                    it.getNodes().forEach {
                        it.getNodes().forEach {
                            Element.importVdf(it).let {
                                canvas.r.addElement(it)
                            }
                        }
                    }
                }
            }
            val f = JFrame().let {
                it.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE)
                it.setContentPane(canvas)
                it.pack()
                it.setLocationRelativeTo(null)
                it
            }
            f.setVisible(true)
        }

        private fun chooseBest(potential: List<Element>): Element? {
            if (potential.isEmpty()) return null
            return potential.fold(potential.first(), { smallest: Element, it: Element ->
                when {
                    it.layer > smallest.layer -> it
                    it.layer == smallest.layer && it.size < smallest.size -> it
                    else -> smallest
                }
            })
        }

        private val GRID_AC = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f)
        private val SELECT_AC = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f)
    }
}
