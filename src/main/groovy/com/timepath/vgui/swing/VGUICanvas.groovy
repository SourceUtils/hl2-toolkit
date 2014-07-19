package com.timepath.vgui.swing

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
        def v = new VGUICanvas()
        def f = new JFrame(contentPane: v)
        f.locationRelativeTo = null
        f.visible = true
    }

    @Delegate(includes = ['getElements', 'removeElements', 'getSelected', 'select', 'load'])
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
    private Image background
    /** Left */
    private static int offX = 10
    /** Top */
    private static int offY = 10
    private BufferedImage currentbg
    private BufferedImage gridbg
    private static final Color BG_COLOR = Color.GRAY
    private static final Color GRID_COLOR = Color.WHITE
    private static final float GRID_ALPHA = 0.25f
    private static final AlphaComposite GRID_AC = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, GRID_ALPHA)
    private static final float SELECT_ALPHA = 0.25f
    private static final AlphaComposite SELECT_AC = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, SELECT_ALPHA)
    private static final AlphaComposite SRC_OVER = AlphaComposite.SrcOver
    private boolean isDragSelecting
    private boolean isDragMoving
    private Point dragStart
    private BufferedImage elementImage
    private static final Comparator<VGUIRenderer.ElemenX> LAYER_SORT = { VGUIRenderer.ElemenX o1, VGUIRenderer.ElemenX o2 ->
        return o1.layer - o2.layer
    } as Comparator<VGUIRenderer.ElemenX>

    public VGUICanvas() {
        addMouseListener(this)
        addMouseMotionListener(this)
        setPreferredSize(new Dimension(640, 480))
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                offX = (width - r.internal.width) / 2 as int
                offY = (height - r.internal.height) / 2 as int
                if (elementImage != null) {
                    BufferedImage img = new BufferedImage((int) r.screen.width + (2 * offX),
                            (int) r.screen.height + (2 * offY),
                            BufferedImage.TYPE_INT_ARGB)
                    Graphics2D ge = img.createGraphics()
                    ge.translate((img.width - elementImage.width) / 2 as int,
                            (img.height - elementImage.height) / 2 as int)
                    ge.drawImage(elementImage, 0, 0, VGUICanvas.this)
                    ge.dispose()
                    elementImage = toCompatibleImage(img)
                }
            }
        })
    }

    public void placed() {
    }

    private Rectangle getOutliers() {
        def rect = [r.internal.width as int, r.internal.height as int] as Rectangle
        for (VGUIRenderer.ElemenX element : r.elements) {
            rect.add(element.bounds)
        }
        return rect
    }


    public void setBackgroundImage(Image background) {
        this.background = background
        prepareImage(background, this)
        repaint()
    }

    /**
     * Convenience method for repainting the bare minimum
     *
     * @param bounds
     */
    void doRepaint(Rectangle bounds) {
        elementImage = null
        repaint(offX + bounds.x as int, offY + bounds.y as int, bounds.width - 1 as int, bounds.height - 1 as int)
        //        this.repaint()
    }


    private void hover(VGUIRenderer.ElemenX e) {
        if (r.hoveredElement == e) { // don't waste time re-drawing
            return
        }
        if (r.hoveredElement != null) { // there is something to clean up
            doRepaint(r.hoveredElement.bounds)
        }
        r.hoveredElement = e
        if (e != null) {
            doRepaint(e.bounds)
        }
    }

    static VGUIRenderer.ElemenX chooseBest(java.util.List<VGUIRenderer.ElemenX> potential) {
        int pSize = potential.size()
        if (pSize == 0) return null
        if (pSize == 1) return potential.get(0)
        VGUIRenderer.ElemenX smallest = potential.get(0)
        for (def iterator = potential.listIterator(1); iterator.hasNext();) {
            VGUIRenderer.ElemenX e = iterator.next() // Sort by layer, then by size
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

    /** as soon as the height drops below 480, stops rendering */
    private BufferedImage drawGrid() {
        BufferedImage img = new BufferedImage(r.screen.width as int, r.screen.height as int, BufferedImage.TYPE_INT_ARGB)
        Graphics2D g = img.createGraphics()
        g.composite = GRID_AC
        g.setRenderingHint(RenderingHints.KEY_RENDERING as RenderingHints.Key, RenderingHints.VALUE_RENDER_SPEED)
        g.color = GRID_COLOR
        int w = r.screen.width as int
        int h = r.screen.height as int
        int minGridSpacing = 10
        int i = minGridSpacing
        if (i < 0) return img
        if (i < 2) { // optimize for small numbers, stop division by zero
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
        Dimension UISize = new Dimension(preferredSize.width + (2 * offX) as int, preferredSize.height + (2 * offY) as int)
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
                currentbg = toCompatibleImage(resizeImage(background))
            }
            g.drawImage(currentbg, offX, offY, this)
        } else {
            g.color = Color.WHITE.darker().darker()
            g.fillRect(offX, offY, (int) Math.round(r.screen.width * r.scale), (int) Math.round(r.screen.height * r.scale))
        }
        if (gridbg == null) {
            gridbg = toCompatibleImage(drawGrid())
        }
        g.drawImage(gridbg, offX, offY, this)
        if (elementImage == null) {
            BufferedImage img = new BufferedImage(r.screen.width + (2 * offX) as int,
                    r.screen.height + (2 * offY) as int,
                    BufferedImage.TYPE_INT_ARGB)
            Graphics2D ge = img.createGraphics()
            ge.translate(offX, offY)
            Collections.sort(r.elements, LAYER_SORT)
            for (VGUIRenderer.ElemenX element : r.elements) {
                ge.composite = SRC_OVER
                r.paintElement(element, ge)
            }
            ge.dispose()
            elementImage = toCompatibleImage(img)
        }
        g.drawImage(elementImage, 0, 0, this)
        g.composite = SELECT_AC
        g.color = Color.CYAN.darker()
        g.fillRect(offX + r.selectRect.x + 1 as int, offY + r.selectRect.y + 1 as int, r.selectRect.width - 2 as int, r.selectRect.height - 2 as int)
        g.color = Color.BLUE
        g.drawRect(offX + r.selectRect.x as int, offY + r.selectRect.y as int, r.selectRect.width - 1 as int, r.selectRect.height - 1 as int)
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point p = new Point(e.point)
        p.translate(-offX, -offY) // relative to top left of canvas
        if (SwingUtilities.isLeftMouseButton(e)) {
            if (isDragSelecting) {
                r.select(dragStart, p, e.isControlDown())
            } else if (isDragMoving) {
                cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)
                r._offX += p.x - dragStart.x as int
                r._offY += p.y - dragStart.y as int
                elementImage = null
                repaint()
                dragStart = p // hacky
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Point p = new Point(e.point)
        p.translate(-offX, -offY)
        hover(chooseBest(r.pick(p, r.elements)))
    }

    @Override
    public void mousePressed(MouseEvent e) {
        requestFocusInWindow()
        Point p = new Point(e.point)
        p.translate(-offX, -offY)
        if (SwingUtilities.isLeftMouseButton(e)) {
            dragStart = new Point(p.x as int, p.y as int)
            r.selectRect.@x = p.@x
            r.selectRect.@y = p.@y
            if (r.hoveredElement == null) { // Clicked nothing
                if (!e.isControlDown()) {
                    r.deselectAll()
                }
                isDragSelecting = true
                isDragMoving = false
            } else { // Hovering over something
                isDragSelecting = false
                isDragMoving = true
                if (e.isControlDown()) { // Always select
                    if (r.isSelected(r.hoveredElement)) {
                        r.deselect(r.hoveredElement)
                    } else {
                        r.select(r.hoveredElement)
                    }
                } else {
                    if (!r.isSelected(r.hoveredElement)) { // If the thing I'm hovering isn't selected already
                        r.deselectAll()
                        r.select(r.hoveredElement)
                    }
                }
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Point p = new Point(e.point)
        p.translate(-offX, -offY)
        if (SwingUtilities.isLeftMouseButton(e)) {
            setCursor(Cursor.defaultCursor)
            if (isDragMoving) {
                placed()
            }
            isDragSelecting = false
            isDragMoving = false
            dragStart = null
            Rectangle original = new Rectangle(r.selectRect)
            r.selectRect.@width = 0
            r.selectRect.@height = 0
            for (VGUIRenderer.ElemenX elem : r.selectedElements) {
                if (r.selectedElements.contains(elem.parent) &&
                        !elem.parent.name.replaceAll("\"", "").endsWith(".res")) { // XXX: hacky
                    continue
                }
                r.translate(elem, r._offX, r._offY)
            }
            r._offX = 0
            r._offY = 0
            doRepaint(new Rectangle(original.x as int, original.y as int, original.width + 1 as int, original.height + 1 as int))
        }
    }

    @Override
    void mouseClicked(final MouseEvent e) {}

    @Override
    void mouseEntered(final MouseEvent e) {}

    @Override
    void mouseExited(final MouseEvent e) {}

//    @Category(Image)
//    class ImageExtensions {
    BufferedImage resizeImage(Image image) { // TODO: aspect ratio tuning
        int w = r.screen.@width
        int h = r.screen.@height
        int type = BufferedImage.TYPE_INT_ARGB
        BufferedImage resizedImage = new BufferedImage(w, h, type)
        Graphics2D g = resizedImage.createGraphics()
        g.composite = SRC_OVER
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION as RenderingHints.Key, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.setRenderingHint(RenderingHints.KEY_RENDERING as RenderingHints.Key, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING as RenderingHints.Key, RenderingHints.VALUE_ANTIALIAS_ON)
        int proposedWidth = Math.round((h / image.getHeight(null) as float) * image.getWidth(null))
        int excess = Math.abs(proposedWidth - w) / 2 as int
        g.drawImage(image, -excess, 0, w + (2 * excess), h, null) // Should scale most images correctly
        g.dispose()
        return resizedImage
    }

    BufferedImage toCompatibleImage(BufferedImage image) {
        // Obtain the current system graphical settings
        def configuration = GraphicsEnvironment.localGraphicsEnvironment.defaultScreenDevice.defaultConfiguration

        // If image is already compatible and optimized for current system settings, simply return it
        if (image.colorModel.equals(configuration.colorModel)) {
            return image
        }
        // Image is not optimized, so create a new image that is
        def copy = configuration.createCompatibleImage(image.width, image.height, image.transparency)
        // Get the graphics context of the new image to draw the old image on
        def g2d = copy.graphics as Graphics2D
        // Actually draw the image and dispose of context no longer needed
        g2d.drawImage(image, 0, 0, null)
        g2d.dispose()
        // Return the new optimized image
        return copy
    }
//    }

}
