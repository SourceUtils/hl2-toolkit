package com.timepath.hl2.swing

import com.timepath.hl2.io.font.VBF
import com.timepath.hl2.io.image.VTF
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.io.IOException
import java.util.LinkedList
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.JPanel
import javax.swing.SwingUtilities

/**
 * @author TimePath
 */
public class VBFCanvas
/**
 * Creates new form VBFTest
 */
: JPanel(), MouseListener, MouseMotionListener {
    private var img: Image? = null
    private var last: Point? = null
    var selected: MutableList<VBF.BitmapGlyph> = LinkedList()
    private var vbf: VBF? = null
    private var vtf: VTF? = null

    init {
        addMouseListener(this)
        addMouseMotionListener(this)
    }

    public fun setVBF(vbf: VBF) {
        this.vbf = vbf
        revalidate()
    }

    public fun setVTF(t: VTF) {
        vtf = t
        vbf!!.width = vtf!!.width.toShort()
        vbf!!.height = vtf!!.height.toShort()
        repaint()
    }

    override fun mouseClicked(e: MouseEvent) {
    }

    override fun mousePressed(e: MouseEvent) {
        last = e.getPoint()
        if (!SwingUtilities.isLeftMouseButton(e)) {
            return
        }
        val old = LinkedList(selected)
        val clicked = get(e.getPoint())
        for (g in clicked) {
            if (g in selected) {
                return
            }
        }
        if (e.isControlDown()) {
            selected.addAll(clicked)
        } else {
            selected = clicked
        }
        for (g in old) {
            repaint(g.bounds)
        }
        for (g in selected) {
            repaint(g.bounds)
        }
    }

    fun get(p: Point): MutableList<VBF.BitmapGlyph> {
        val intersected = LinkedList<VBF.BitmapGlyph>()
        vbf?.let {
            for (g in it.glyphs) {
                if (p in g.bounds) {
                    intersected.add(g)
                }
            }
        }
        return intersected
    }

    override fun mouseReleased(e: MouseEvent) {
    }

    override fun mouseEntered(e: MouseEvent) {
    }

    override fun mouseExited(e: MouseEvent) {
    }

    override fun mouseDragged(e: MouseEvent) {
        val p = e.getPoint()
        last?.let {
            e.translatePoint(-it.x, -it.y)
        }
        for (sel in selected) {
            if (SwingUtilities.isRightMouseButton(e)) {
                sel.bounds.width += e.getPoint().x
                sel.bounds.height += e.getPoint().y
            } else {
                sel.bounds.x += e.getPoint().x
                sel.bounds.y += e.getPoint().y
            }
        }
        last = p
        repaint()
    }

    override fun mouseMoved(e: MouseEvent) {
    }

    public fun select(g: VBF.BitmapGlyph?) {
        selected.clear()
        g?.let { g ->
            selected.add(g)
            repaint(g.bounds)
        }
    }

    override fun paintComponent(g: Graphics) {
        val g2 = g as Graphics2D
        g2.setComposite(acNormal)
        g2.setColor(Color.BLACK)
        g2.fillRect(0, 0, getWidth(), getHeight())
        if ((img == null) && (vtf != null)) {
            try {
                img = vtf!!.getImage(0)
            } catch (ex: IOException) {
                LOG.log(Level.SEVERE, null, ex)
            }

        }
        vbf?.let {
            g2.setColor(Color.GRAY)
            g2.fillRect(0, 0, it.width.toInt(), it.height.toInt())
        }
        img?.let {
            g2.drawImage(it, 0, 0, this)
        }
        vbf?.let {
            for (glyph in it.glyphs) {
                if (glyph == null) {
                    continue
                }
                val bounds = glyph.bounds
                if ((bounds == null) || bounds.isEmpty()) {
                    continue
                }
                g2.setComposite(acNormal)
                g2.setColor(Color.GREEN)
                g2.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1)
                if (glyph in selected) {
                    g2.setComposite(acSelected)
                    g2.fillRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1)
                }
                //                 TODO: Negative font folor
                //                Map<TextAttribute, Object> map = new Hashtable<TextAttribute, Object>();
                //                map.put(TextAttribute.SWAP_COLORS, TextAttribute.SWAP_COLORS_ON);
                //                map.put(TextAttribute.FOREGROUND, Color.BLACK);
                //                map.put(TextAttribute.BACKGROUND, Color.TRANSLUCENT);
                //                Font f = this.getFont().deriveFont(map);
                //                g.setFont(f);
                //                g.setXORMode(Color.WHITE);
                g2.setComposite(acText)
                g2.setColor(Color.GREEN)
                g2.drawString(Integer.toString(glyph.index.toInt()), bounds.x + 1, (bounds.y + bounds.height) - 1)
            }
        }
    }

    override fun getPreferredSize() = vbf?.let {
        Dimension(it.width.toInt(), it.height.toInt())
    } ?: Dimension(128, 128)

    companion object {

        private val LOG = Logger.getLogger(javaClass<VBFCanvas>().getName())
        private val serialVersionUID = 1
        private val acNormal = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f)
        private val acSelected = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)
        private val acText = AlphaComposite.getInstance(AlphaComposite.SRC_OVER)
    }
}
