package com.timepath.vgui.swing

import com.timepath.steam.SteamUtils
import com.timepath.steam.io.VDFNode
import com.timepath.vgui.Element
import com.timepath.vgui.ImageUtils
import com.timepath.vgui.VGUIRenderer
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.logging.Log

import javax.swing.*
import java.awt.*
import java.awt.event.*
import java.awt.image.BufferedImage
import java.nio.charset.StandardCharsets
import java.util.List

/**
 * @author TimePath
 */
@CompileStatic
@TypeChecked
@Log('LOG')
@SuppressWarnings("serial")
class VGUICanvas extends JPanel implements MouseListener, MouseMotionListener {

    public static void main(String[] args) {
        def canvas = new VGUICanvas()
        VDFNode root = new VDFNode("Root")
        def p = { String k, v -> new VDFNode.VDFProperty(k, v) }
        VDFNode node = new VDFNode("Test")
        root.addNode(node)
        node.addAllProperties([
                p('enabled', 1),
                p('visible', 1),
                p('xpos', 5),
                p('ypos', 5),
                p('zpos', 0),
                p('wide', 100),
                p('tall', 100),
                p('labeltext', 'test'),
                p('textalignment', 'center'),
                p('controlname', 'label'),
        ])
        node = new VDFNode(new FileInputStream(new File(SteamUtils.steam, "resource/FileOpenDialog.res")), StandardCharsets.UTF_8)
        for (VDFNode n in (node.nodes*.nodes.flatten() as List<VDFNode>)) {
            def e = Element.importVdf(n)
            canvas.r.addElement(e)
        }
        def f = new JFrame(contentPane: canvas, defaultCloseOperation: WindowConstants.DISPOSE_ON_CLOSE)
        f.pack()
        f.locationRelativeTo = null
        f.visible = true
    }

    private static final AlphaComposite GRID_AC = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f)
    private static final AlphaComposite SELECT_AC = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.25f)

    private Image background
    private BufferedImage currentbg
    private BufferedImage gridbg
    private Color BG_COLOR = Color.GRAY
    private Color GRID_COLOR = Color.WHITE
    /** Left */
    private int offX = 10
    /** Top */
    private int offY = 10
    /** Initial drag point */
    private Point dragStart
    private boolean dragSelecting
    private boolean dragMoving

    @Delegate(includes = ['removeElements', 'getSelected', 'select', 'load'])
    VGUIRenderer r = new VGUIRenderer() {
        @Override
        void doRepaint(final Rectangle bounds) {
            VGUICanvas.this.doRepaint(bounds)
        }

        @Override
        void repaint() {
            VGUICanvas.this.repaint()
        }

        @Override
        FontMetrics getFontMetrics(final Font font) {
            return VGUICanvas.this.getFontMetrics(font)
        }
    }

    LinkedList<Element> getElements() { r.elements }

    public VGUICanvas() {
        addMouseListener(this)
        addMouseMotionListener(this)
        preferredSize = new Dimension(640, 480)
        addComponentListener new ComponentAdapter() {
            @Override
            void componentResized(ComponentEvent e) {
                resize()
            }
        }
    }

    private void resize() {
        offX = (int) ((width - r.internal.@width) / 2)
        offY = (int) ((height - r.internal.@height) / 2)
        repaint()
    }

    /** Fired when an element has been dropped */
    protected void placed() {
    }

    void setBackgroundImage(Image background) {
        this.background = background
        prepareImage(background, this)
        repaint()
    }

    private Rectangle getOutliers() {
        def rect = new Rectangle(r.internal.@width, r.internal.@height)
        for (elem in r.@elements) rect.add(r.bounds(elem))
        return rect
    }

    /**
     * Convenience method for repainting the bare minimum
     *
     * @param bounds
     */
    private void doRepaint(Rectangle bounds) {
        r.elementImage = null
        repaint(offX + bounds.@x, offY + bounds.@y, bounds.@width - 1, bounds.@height - 1)
    }

    private void doRepaint1(Rectangle bounds) {
        r.elementImage = null
        repaint(offX + bounds.@x, offY + bounds.@y, bounds.@width, bounds.@height)
    }

    private void hover(Element e) {
        if (r.hoveredElement == e) return // Nothing to do
        // Clean up if needed
        if (r.hoveredElement) doRepaint(r.bounds(r.hoveredElement))
        // Draw the new element if needed
        if ((r.hoveredElement = e)) doRepaint(r.bounds(e))
    }

    /** As soon as the height drops below 480, stops rendering */
    private BufferedImage drawGrid() {
        def img = new BufferedImage(r.screen.@width, r.screen.@height, BufferedImage.TYPE_INT_ARGB)
        def g = img.createGraphics()
        g.composite = GRID_AC
        g.setRenderingHint(RenderingHints.KEY_RENDERING as RenderingHints.Key, RenderingHints.VALUE_RENDER_SPEED)
        g.color = GRID_COLOR
        int w = r.screen.@width
        int h = r.screen.@height
        int minGridSpacing = 10
        int i = minGridSpacing
        if (i < 0) return img
        if (i < 2) { // Optimize for small numbers, stop division by zero
            g.fillRect(0, 0, r.screen.@width, r.screen.@height)
            return img
        }
        int cross = 0
        int maxX = w - (w % i)
        int maxY = h - (h % i)
        double multX = r.screen.width / r.internal.width
        double multY = r.screen.height / r.internal.height
        for (int y = -1; y <= (maxY / i); y++) {
            for (int x = -1; x <= (maxX / i); x++) {
                int dx = (int) Math.round((maxX * x * i * multX) / maxX)
                int dy = (int) Math.round((maxY * y * i * multY) / maxY)
                g.drawLine(dx + cross, dy + cross, dx - (1 + cross), dy - (1 + cross))
                g.drawLine(dx - (1 + cross), dy + cross, dx + cross, dy - (1 + cross))
            }
        }
        g.dispose()
        return img
    }

    @Override
    public void setPreferredSize(Dimension preferredSize) {
        def UISize = new Dimension(preferredSize.@width + (2 * offX), preferredSize.@height + (2 * offY))
        super.preferredSize = UISize
        currentbg = null
        gridbg = null
        r.screen = preferredSize
        //        long gcm = gcm(hudRes.width, hudRes.height)
        double resX = r.screen.width
        double resY = r.screen.height
        double m = resX / resY
        //        System.out.println(resX + "/" + resY + "=" + m)
        //        System.out.println((resX / gcm) + ":" + (resY / gcm) + " = " + Math.round(m * 480) + "x" + 480)
        r.internal = new Dimension((int) Math.round(m * 480), 480)
        repaint()
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = graphics as Graphics2D
        g.color = BG_COLOR
        g.fillRect(0, 0, width, height)
        if (background) {
            if (!currentbg) currentbg = ImageUtils.toCompatibleImage(ImageUtils.resizeImage(background, r.screen.@width, r.screen.@height))
            g.drawImage(currentbg, offX, offY, this)
        } else {
            g.color = Color.WHITE.darker().darker()
            g.fillRect(offX, offY, (int) Math.round(r.screen.width * r.scale), (int) Math.round(r.screen.height * r.scale))
        }
        if (!gridbg) gridbg = ImageUtils.toCompatibleImage(drawGrid())
        g.drawImage(gridbg, offX, offY, this)
        g.drawImage(r.elementImage, offX, offY, this)
        if (dragSelecting) {
            g.composite = SELECT_AC
            g.color = Color.CYAN.darker()
            g.fillRect(offX + r.selectRect.@x + 1, offY + r.selectRect.@y + 1, r.selectRect.@width - 2, r.selectRect.@height - 2)
            g.color = Color.BLUE
            g.drawRect(offX + r.selectRect.@x, offY + r.selectRect.@y, r.selectRect.@width - 1, r.selectRect.@height - 1)
        }
    }

    @Override
    void mouseDragged(MouseEvent e) {
        def p = e.point
        p.translate(-offX, -offY) // Localize
        if (SwingUtilities.isLeftMouseButton(e)) {
            if (dragSelecting) {
                r.select(dragStart, p, e.controlDown)
            } else if (dragMoving) {
                cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                r.dragX += p.@x - dragStart.@x
                r.dragY += p.@y - dragStart.@y
                r.elementImage = null
                repaint()
                dragStart = p
            }
        }
    }

    @Override
    void mouseMoved(MouseEvent e) {
        def p = e.point
        p.translate(-offX, -offY) // Localize
        hover chooseBest(r.pick(p))
    }

    @Override
    void mousePressed(MouseEvent e) {
        def p = e.point
        p.translate(-offX, -offY) // Localize
        if (SwingUtilities.isLeftMouseButton(e)) {
            dragStart = new Point(p.@x, p.@y)
            r.selectRect.setSize(p.@x, p.@y)
            if (!r.hoveredElement) { // Clicked nothing
                if (!e.controlDown) r.deselectAll()
                dragSelecting = true
                dragMoving = false
            } else { // Hovering over something
                dragSelecting = false
                dragMoving = true
                if (e.controlDown) {
                    // Toggle select
                    if (r.isSelected(r.hoveredElement)) {
                        r.deselect(r.hoveredElement)
                    } else {
                        r.select(r.hoveredElement)
                    }
                } else {
                    // If the thing I'm hovering isn't selected already, drop all selections
                    if (!r.isSelected(r.hoveredElement)) {
                        r.deselectAll()
                        r.select(r.hoveredElement)
                    }
                }
            }
        }
    }

    @Override
    void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            cursor = Cursor.defaultCursor
            if (dragMoving) placed() // Release element
            dragSelecting = false
            dragMoving = false
            dragStart = null
            def original = new Rectangle(r.selectRect)
            r.selectRect.setSize(0, 0)
            for (elem in r.selectedElements) {
                // ???
                if (r.selectedElements.contains(elem.parent) &&
                        !elem.parent.name.replaceAll("\"", "").endsWith(".res")) { // XXX: hacky
                    continue
                }
                r.translate(elem, r.dragX, r.dragY)
            }
            r.dragX = 0
            r.dragY = 0
            doRepaint1(original)
        }
    }

    @Override
    void mouseClicked(MouseEvent e) {}

    @Override
    void mouseEntered(MouseEvent e) {}

    @Override
    void mouseExited(MouseEvent e) {}

    private static Element chooseBest(List<Element> potential) {
        Element smallest = null
        for (elem in potential) {
            if (!smallest) {
                smallest = elem
            } else if (elem.layer > smallest.layer) { // Sort by layer, then by size
                smallest = elem
            } else if (elem.layer == smallest.layer && elem.size < smallest.size) {
                smallest = elem
            }
        }
        return smallest
    }
}
