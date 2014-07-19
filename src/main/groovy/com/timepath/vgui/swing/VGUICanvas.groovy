package com.timepath.vgui.swing

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
        def e = Element.importVdf(node)
        canvas.r.addElement(e)
        def f = new JFrame(contentPane: canvas)
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
        preferredSize = [640, 480] as Dimension
        addComponentListener new ComponentAdapter() {
            @Override
            void componentResized(ComponentEvent e) {
                resize()
            }
        }
    }

    private void resize() {
        offX = (width - r.internal.width) / 2 as int
        offY = (height - r.internal.height) / 2 as int
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
        def rect = [r.internal.width as int, r.internal.height as int] as Rectangle
        for (Element element : r.@elements) {
            rect.add(r.bounds(element))
        }
        return rect
    }

    /**
     * Convenience method for repainting the bare minimum
     *
     * @param bounds
     */
    private void doRepaint(Rectangle bounds) {
        r.elementImage = null
        repaint(offX + bounds.x as int, offY + bounds.y as int, bounds.width - 1 as int, bounds.height - 1 as int)
        //        this.repaint()
    }

    private void doRepaint1(Rectangle bounds) {
        r.elementImage = null
        repaint(offX + bounds.x as int, offY + bounds.y as int, bounds.width as int, bounds.height as int)
    }

    private void hover(Element e) {
        if (r.hoveredElement == e) return  // Don't waste time re-drawing
        if (r.hoveredElement != null) {    // There is something to clean up
            doRepaint(r.bounds(r.hoveredElement))
        }
        r.hoveredElement = e
        if (e != null) doRepaint(r.bounds(e))
    }

    /** As soon as the height drops below 480, stops rendering */
    private BufferedImage drawGrid() {
        def img = new BufferedImage(r.screen.width as int, r.screen.height as int, BufferedImage.TYPE_INT_ARGB)
        def g = img.createGraphics()
        g.composite = GRID_AC
        g.setRenderingHint(RenderingHints.KEY_RENDERING as RenderingHints.Key, RenderingHints.VALUE_RENDER_SPEED)
        g.color = GRID_COLOR
        int w = r.screen.width as int
        int h = r.screen.height as int
        int minGridSpacing = 10
        int i = minGridSpacing
        if (i < 0) return img
        if (i < 2) { // Optimize for small numbers, stop division by zero
            g.fillRect(0, 0, r.screen.width as int, r.screen.height as int)
            return img
        }
        int cross = 0
        int maxX = w - (w % i)
        int maxY = h - (h % i)
        double multX = r.screen.width / (double) r.internal.width
        double multY = r.screen.height / (double) r.internal.height
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
        def UISize = new Dimension(preferredSize.width + (2 * offX) as int, preferredSize.height + (2 * offY) as int)
        super.preferredSize = UISize
        currentbg = null
        gridbg = null
        r.screen = preferredSize
        //        long gcm = gcm(hudRes.width, hudRes.height)
        long resX = r.screen.width as long
        long resY = r.screen.height as long
        double m = resX / (double) resY
        //        System.out.println(resX + "/" + resY + "=" + m)
        //        System.out.println((resX / gcm) + ":" + (resY / gcm) + " = " + Math.round(m * 480) + "x" + 480)
        r.internal = new Dimension((int) Math.round(m * 480), 480)
        repaint()
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        //        Rectangle outliers = getOutliers()
        //        int left = -outliers.x
        //        int right = outliers.width + outliers.x - internal.width
        //        int top = -outliers.y
        //        int down = outliers.height + outliers.y - internal.height
        //        this.resize(this.getWidth() + left + right, this.getHeight() + top + down)
        //        offX = ((this.getWidth() - internal.width) / 2)
        //        offY = ((this.getHeight() - internal.height) / 2)
        //        offX = ((this.getWidth() - internal.width) / 2) + ((-outliers.x) - (outliers.width + outliers.x - internal
        // .width))
        //        offY = ((this.getHeight() - internal.height) / 2) + ((-outliers.y) - (outliers.height + outliers.y - internal
        // .height))
        //        super.paintComponent(graphics)
        Graphics2D g = (Graphics2D) graphics
        g.color = BG_COLOR
        g.fillRect(0, 0, getWidth(), getHeight())
        if (background != null) {
            if (currentbg == null) {
                currentbg = ImageUtils.toCompatibleImage(ImageUtils.resizeImage(background, r.screen.@width, r.screen.@height))
            }
            g.drawImage(currentbg, offX, offY, this)
        } else {
            g.color = Color.WHITE.darker().darker()
            g.fillRect(offX, offY, (int) Math.round(r.screen.width * r.scale), (int) Math.round(r.screen.height * r.scale))
        }
        if (gridbg == null) {
            gridbg = ImageUtils.toCompatibleImage(drawGrid())
        }
        g.drawImage(gridbg, offX, offY, this)
        g.drawImage(r.elementImage, offX, offY, this)
        g.composite = SELECT_AC
        g.color = Color.CYAN.darker()
        g.fillRect(offX + r.selectRect.x + 1 as int, offY + r.selectRect.y + 1 as int, r.selectRect.width - 2 as int, r.selectRect.height - 2 as int)
        g.color = Color.BLUE
        g.drawRect(offX + r.selectRect.x as int, offY + r.selectRect.y as int, r.selectRect.width - 1 as int, r.selectRect.height - 1 as int)
    }

    @Override
    void mouseDragged(MouseEvent e) {
        def p = e.point
        p.translate(-offX, -offY) // Localize
        if (SwingUtilities.isLeftMouseButton(e)) {
            if (dragSelecting) {
                r.select(dragStart, p, e.isControlDown())
            } else if (dragMoving) {
                cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                r.dragX += p.x - dragStart.x as int
                r.dragY += p.y - dragStart.y as int
                r.elementImage = null
                repaint()
                dragStart = p // Hacky
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
            dragStart = [p.x as int, p.y as int] as Point
            r.selectRect.@x = p.@x
            r.selectRect.@y = p.@y
            if (r.hoveredElement == null) { // Clicked nothing
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
        def p = e.point
        p.translate(-offX, -offY) // Localize
        if (SwingUtilities.isLeftMouseButton(e)) {
            setCursor(Cursor.defaultCursor)
            if (dragMoving) placed() // Release element
            dragSelecting = false
            dragMoving = false
            dragStart = null
            def original = new Rectangle(r.selectRect)
            r.selectRect.@width = 0
            r.selectRect.@height = 0
            for (def elem in r.selectedElements) {
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

    private static Element chooseBest(java.util.List<Element> potential) {
        int pSize = potential.size()
        if (pSize == 0) return null
        if (pSize == 1) return potential.get(0)
        def smallest = potential.get(0)
        for (def iterator = potential.listIterator(1); iterator.hasNext();) {
            def e = iterator.next() // Sort by layer, then by size
            if (e.layer > smallest.layer) {
                smallest = e
            } else if (e.layer == smallest.layer) {
                if (e.size < smallest.size) {
                    smallest = e
                }
            }
        }
        return smallest
    }
}
