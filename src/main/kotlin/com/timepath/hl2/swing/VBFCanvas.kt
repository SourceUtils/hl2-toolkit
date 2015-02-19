package com.timepath.hl2.swing

import com.timepath.hl2.io.font.VBF
import com.timepath.hl2.io.image.VTF

import javax.swing.*
import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.io.IOException
import java.util.LinkedList
import java.util.logging.Level
import java.util.logging.Logger

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
    private var selected: MutableList<VBF.BitmapGlyph> = LinkedList()
    private var vbf: VBF? = null
    private var vtf: VTF? = null

    {
        addMouseListener(this)
        addMouseMotionListener(this)
    }

    public fun getSelected(): VBF.BitmapGlyph? {
        if (selected.isEmpty()) {
            return null
        }
        return selected.get(0)
    }

    public fun setVBF(vbf: VBF) {
        this.vbf = vbf
        revalidate()
    }

    public fun setVTF(t: VTF) {
        vtf = t
        vbf!!.setWidth(vtf!!.width)
        vbf!!.setHeight(vtf!!.height)
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
            if (selected.contains(g)) {
                return
            }
        }
        if (e.isControlDown()) {
            selected.addAll(clicked)
        } else {
            selected = clicked
        }
        for (g in old) {
            repaint(g.getBounds())
        }
        for (g in selected) {
            repaint(g.getBounds())
        }
    }

    fun get(p: Point): MutableList<VBF.BitmapGlyph> {
        val intersected = LinkedList<VBF.BitmapGlyph>()
        if ((vbf == null) || (vbf!!.getGlyphs() == null)) {
            return intersected
        }
        for (g in vbf!!.getGlyphs()) {
            if (g.getBounds().contains(p)) {
                intersected.add(g)
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
        if (last != null) {
            e.translatePoint(-last!!.x, -last!!.y)
        }
        for (sel in selected) {
            if (SwingUtilities.isRightMouseButton(e)) {
                sel.getBounds().width += e.getPoint().x
                sel.getBounds().height += e.getPoint().y
            } else {
                sel.getBounds().x += e.getPoint().x
                sel.getBounds().y += e.getPoint().y
            }
        }
        last = p
        repaint()
    }

    override fun mouseMoved(e: MouseEvent) {
    }

    public fun select(g: VBF.BitmapGlyph?) {
        selected.clear()
        if (g != null) {
            selected.add(g)
            repaint(g.getBounds())
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
        if (vbf != null) {
            g2.setColor(Color.GRAY)
            g2.fillRect(0, 0, vbf!!.getWidth().toInt(), vbf!!.getHeight().toInt())
        }
        if (img != null) {
            g2.drawImage(img, 0, 0, this)
        }
        if (vbf != null) {
            for (glyph in vbf!!.getGlyphs()) {
                if (glyph == null) {
                    continue
                }
                val bounds = glyph.getBounds()
                if ((bounds == null) || bounds.isEmpty()) {
                    continue
                }
                g2.setComposite(acNormal)
                g2.setColor(Color.GREEN)
                g2.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1)
                if (selected.contains(glyph)) {
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
                g2.drawString(Integer.toString(glyph.getIndex().toInt()), bounds.x + 1, (bounds.y + bounds.height) - 1)
            }
        }
    }

    override fun getPreferredSize(): Dimension {
        if (vbf == null) {
            return Dimension(128, 128)
        }
        return Dimension(vbf!!.getWidth().toInt(), vbf!!.getHeight().toInt())
    }

    class object {

        private val LOG = Logger.getLogger(javaClass<VBFCanvas>().getName())
        private val serialVersionUID = 1
        private val acNormal = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f)
        private val acSelected = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5.toFloat())
        private val acText = AlphaComposite.getInstance(AlphaComposite.SRC_OVER)
    }
}
