package com.timepath.hl2.swing;

import com.timepath.hl2.io.util.Alignment;
import com.timepath.hl2.io.util.Element;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.logging.Logger;

/**
 * OSX - http://i.imgur.com/KxiV3.jpg
 * WIN - http://i.imgur.com/VqABM.jpg
 *
 * @author TimePath
 */
@SuppressWarnings("serial")
public class VGUICanvas extends JPanel implements MouseListener, MouseMotionListener {

    private static final Logger              LOG              = Logger.getLogger(VGUICanvas.class.getName());
    private static       float               gridAlpha        = 0.25f;
    private static       AlphaComposite      acGrid           = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, gridAlpha);
    private static       float               elementBgAlpha   = 0.25f;
    private static       float               selectAlpha      = 0.25f;
    private static       AlphaComposite      acSelect         = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, selectAlpha);
    /**
     * No derive method on 1.5
     */
    private static       AlphaComposite      acSimple         = AlphaComposite.SrcOver;
    private static       Comparator<Element> layerSort        = new Comparator<Element>() {
        @Override
        public int compare(Element o1, Element o2) {
            return o1.getLayer() - o2.getLayer();
        }
    };
    private static       int                 offX             = 10; // left
    private static       int                 offY             = 10; // top
    private final        Color               BG_COLOR         = Color.GRAY;
    private final        Color               GRID_COLOR       = Color.WHITE;
    private final        int                 minGridSpacing   = 10;
    /** List of elements */
    private final        List<Element>       elements         = new LinkedList<>();
    /** List of currently selected elements */
    private final        List<Element>       selectedElements = new LinkedList<>();
    private              double              scale            = 1;
    private Dimension screen;
    private Dimension internal;
    private Rectangle selectRect = new Rectangle();
    private BufferedImage currentbg;
    private BufferedImage gridbg;
    private BufferedImage elementImage;
    private Image         background;
    private int           _offX;
    private int           _offY;
    private boolean       isDragSelecting;
    private boolean       isDragMoving;
    private Point         dragStart;
    private Element       hoveredElement;

    public VGUICanvas() {
        initInput();
        setPreferredSize(new Dimension(640, 480));
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                offX = ( getWidth() - internal.width ) / 2;
                offY = ( getHeight() - internal.height ) / 2;
                if(elementImage != null) {
                    BufferedImage img = new BufferedImage(screen.width + ( 2 * offX ),
                                                          screen.height + ( 2 * offY ),
                                                          BufferedImage.TYPE_INT_ARGB);
                    Graphics2D ge = img.createGraphics();
                    ge.translate(( img.getWidth() - elementImage.getWidth() ) / 2,
                                 ( img.getHeight() - elementImage.getHeight() ) / 2);
                    ge.drawImage(elementImage, 0, 0, VGUICanvas.this);
                    ge.dispose();
                    elementImage = toCompatibleImage(img);
                }
            }
        });
    }

    private static BufferedImage toCompatibleImage(BufferedImage image) {
        // obtain the current system graphical settings
        GraphicsConfiguration gfx_config = GraphicsEnvironment.getLocalGraphicsEnvironment().
                getDefaultScreenDevice().getDefaultConfiguration();

        /*
         * if image is already compatible and optimized for current system
         * settings, simply return it
         */
        if(image.getColorModel().equals(gfx_config.getColorModel())) {
            return image;
        }
        // image is not optimized, so create a new image that is
        BufferedImage new_image = gfx_config.createCompatibleImage(image.getWidth(), image.getHeight(), image.getTransparency());
        // get the graphics context of the new image to draw the old image on
        Graphics2D g2d = (Graphics2D) new_image.getGraphics();
        // actually draw the image and dispose of context no longer needed
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        // return the new optimized image
        return new_image;
    }

    private void initInput() {
        addMouseListener(this);
        addMouseMotionListener(this);
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
        return ( b == 0 ) ? a : gcm(b, a % b);
    }

    public void setBackgroundImage(Image background) {
        this.background = background;
        prepareImage(background, this);
        repaint();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        Point p = new Point(e.getPoint());
        p.translate(-offX, -offY); // relative to top left of canvas
        if(SwingUtilities.isLeftMouseButton(e)) {
            if(isDragSelecting) {
                select(dragStart, p, e.isControlDown());
            } else if(isDragMoving) {
                setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
                _offX += p.x - dragStart.x;
                _offY += p.y - dragStart.y;
                elementImage = null;
                repaint();
                dragStart = p; // hacky
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        Point p = new Point(e.getPoint());
        p.translate(-offX, -offY);
        hover(chooseBest(pick(p, elements)));
    }

    // Checks if poing p is inside the bounds of any element
    static List<Element> pick(Point p, Iterable<Element> elements) {
        List<Element> potential = new LinkedList<>();
        for(Element e : elements) {
            if(e.getBounds().contains(p)) {
                potential.add(e);
            }
        }
        return potential;
    }

    static Element chooseBest(List<Element> potential) {
        int pSize = potential.size();
        if(pSize == 0) {
            return null;
        }
        if(pSize == 1) {
            return potential.get(0);
        }
        Element smallest = potential.get(0);
        for(int i = 1; i < potential.size(); i++) {
            Element e = potential.get(i); // sort by layer, then by size
            if(e.getLayer() > smallest.getLayer()) {
                smallest = e;
            } else if(e.getLayer() == smallest.getLayer()) {
                if(e.getSize() < smallest.getSize()) {
                    smallest = e;
                }
            }
        }
        return smallest;
    }

    private void hover(Element e) {
        if(hoveredElement == e) { // don't waste time re-drawing
            return;
        }
        if(hoveredElement != null) { // there is something to clean up
            doRepaint(hoveredElement.getBounds());
        }
        hoveredElement = e;
        if(e != null) {
            doRepaint(e.getBounds());
        }
    }

    void select(Point p1, Point p2, boolean ctrl) {
        if(( p1 != null ) && ( p2 != null )) {
            Rectangle originalSelectRect = new Rectangle(selectRect);
            selectRect = fitRect(p1, p2, selectRect);
            for(Element e : elements) {
                if(selectRect.intersects(e.getBounds())) {
                    select(e); // TODO: not perfect, I want the selection inverted as it goes over
                } else {
                    if(!ctrl) {
                        deselect(e);
                    }
                }
            }
            // This repaints the overlap a second time. A minor inefficiency...
            doRepaint(new Rectangle(originalSelectRect.x,
                                    originalSelectRect.y,
                                    originalSelectRect.width + 1,
                                    originalSelectRect.height + 1));
            doRepaint(new Rectangle(selectRect.x, selectRect.y, selectRect.width + 1, selectRect.height + 1));
        }
    }

    private static Rectangle fitRect(Point p1, Point p2, Rectangle r) {
        Rectangle result = new Rectangle(r);
        result.x = Math.min(p1.x, p2.x);
        result.y = Math.min(p1.y, p2.y);
        result.width = Math.abs(p2.x - p1.x);
        result.height = Math.abs(p2.y - p1.y);
        return result;
    }

    public void select(Element e) {
        if(e != null) {
            if(selectedElements.contains(e)) {
                return;
            }
            selectedElements.add(e);
            //            if(e.children != null) {
            //                for(int i = 0; i < e.children.size(); i++) {
            //                    select(e.children.get(i));
            //                }
            //            }
            doRepaint(e.getBounds());
        }
    }

    /**
     * Convenience method for repainting the bare minimum
     *
     * @param bounds
     */
    void doRepaint(Rectangle bounds) {
        elementImage = null;
        repaint(offX + bounds.x, offY + bounds.y, bounds.width - 1, bounds.height - 1);
        //        this.repaint();
    }

    void deselect(Element e) {
        if(e != null) {
            if(!selectedElements.contains(e)) {
                return;
            }
            selectedElements.remove(e);
            //            if(e.children != null) {
            //                for(int i = 0; i < e.children.size(); i++) {
            //                    deselect(e.children.get(i));
            //                }
            //            }
            doRepaint(e.getBounds());
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    } // May be needed for double clicks later on

    @Override
    public void mousePressed(MouseEvent e) {
        requestFocusInWindow();
        Point p = new Point(e.getPoint());
        p.translate(-offX, -offY);
        if(SwingUtilities.isLeftMouseButton(e)) {
            dragStart = new Point(p.x, p.y);
            selectRect.x = p.x;
            selectRect.y = p.y;
            if(hoveredElement == null) { // clicked nothing
                if(!e.isControlDown()) {
                    deselectAll();
                }
                isDragSelecting = true;
                isDragMoving = false;
            } else { // hovering over something
                isDragSelecting = false;
                isDragMoving = true;
                if(e.isControlDown()) { // always select
                    if(isSelected(hoveredElement)) {
                        deselect(hoveredElement);
                    } else {
                        select(hoveredElement);
                    }
                } else {
                    if(!isSelected(hoveredElement)) { // If the thing I'm hovering isn't selected already
                        deselectAll();
                        select(hoveredElement);
                    }
                }
            }
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Point p = new Point(e.getPoint());
        p.translate(-offX, -offY);
        if(SwingUtilities.isLeftMouseButton(e)) {
            setCursor(Cursor.getDefaultCursor());
            if(isDragMoving) {
                placed();
            }
            isDragSelecting = false;
            isDragMoving = false;
            dragStart = null;
            Rectangle original = new Rectangle(selectRect);
            selectRect.width = 0;
            selectRect.height = 0;
            for(Element elem : selectedElements) {
                if(selectedElements.contains(elem.getParent()) &&
                   !elem.getParent().getName().replaceAll("\"", "").endsWith(".res")) { // XXX: hacky
                    continue;
                }
                translate(elem, _offX, _offY);
            }
            _offX = 0;
            _offY = 0;
            doRepaint(new Rectangle(original.x, original.y, original.width + 1, original.height + 1));
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    } // Needed for showing mouse coordinates later

    @Override
    public void mouseExited(MouseEvent e) {
    } // Needed for hiding mouse coordinates later

    public void placed() {
    }

    void translate(Element e, double dx, double dy) { // TODO: scaling (scale 5 = 5 pixels to move 1 x/y co-ord)
        //        Rectangle originalBounds = new Rectangle(e.getBounds());
        if(e.getXAlignment() == Alignment.Right) {
            dx *= -1;
        }
        double scaleX = screen.width / (double) internal.width;
        dx = Math.round(dx / scaleX);
        e.setLocalX(e.getLocalX() + dx);
        if(e.getYAlignment() == Alignment.Right) {
            dy *= -1;
        }
        double scaleY = screen.height / (double) internal.height;
        dy = Math.round(dy / scaleY);
        e.setLocalY(e.getLocalY() + dy);
        //        this.doRepaint(originalBounds);
        //        this.doRepaint(e.getBounds());
        repaint(); // helps
    }

    boolean isSelected(Element e) {
        return selectedElements.contains(e);
    }

    void deselectAll() {
        Iterable<Element> temp = new ArrayList<>(selectedElements);
        selectedElements.clear();
        for(Element temp1 : temp) {
            doRepaint(temp1.getBounds());
        }
    }

    Element getHovered() {
        return hoveredElement;
    }

    public List<Element> getElements() {
        return Collections.unmodifiableList(elements);
    }

    public void removeElements(Iterable<Element> list) {
        for(Element e : list) {
            removeElement(e);
        }
    }

    void removeElement(Element e) {
        if(elements.contains(e)) {
            elements.remove(e);
            doRepaint(e.getBounds());
        }
    }

    public void clearElements() {
        for(Element element : elements) {
            removeElement(element);
        }
    }

    /**
     * Special exceptions are handled here
     *
     * @param element
     */
    public void load(Element element) {
        if(element.getFile() == null) {
            return;
        }
        if(Element.areas.containsKey(element.getFile())) {
            Element p = Element.areas.get(element.getFile());
            p.add(element);
            addElement(p);
        } else if("HudPlayerHealth".equalsIgnoreCase(element.getFile())) { // better, but still not perfect
            // move by "CHealthAccountPanel" delta_item_x" and "delta_item_start_y"
            Element p = Element.areas.get("CHealthAccountPanel");
            p.add(element);
            addElement(p);
        } else if("HudAmmoWeapons".equalsIgnoreCase(element.getFile())) {
            Element p = Element.areas.get("HudWeaponAmmo");
            p.add(element);
            addElement(p);
        }
        addElement(element); // weird but it has to be done
    }

    void addElement(Element e) {
        if(!elements.contains(e)) {
            e.validateLoad();
            //            e.setCanvas(this);
            elements.add(e);
            doRepaint(e.getBounds());
        }
    }

    public Iterable<Element> getSelected() {
        return Collections.unmodifiableList(selectedElements);
    }

    public void removeAllElements() {
        Iterable<Element> temp = new ArrayList<>(elements);
        elements.clear();
        for(Element e : temp) {
            doRepaint(e.getBounds());
        }
    }

    private Rectangle getOutliers() {
        Rectangle r = new Rectangle(internal.width, internal.height);
        for(Element element : elements) {
            r.add(element.getBounds());
        }
        return r;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        //        Rectangle outliers = getOutliers();
        //        int left = -outliers.x;
        //        int right = outliers.width + outliers.x - internal.width;
        //        int top = -outliers.y;
        //        int down = outliers.height + outliers.y - internal.height;
        //        this.resize(this.getWidth() + left + right, this.getHeight() + top + down);
        //        offX = ((this.getWidth() - internal.width) / 2);
        //        offY = ((this.getHeight() - internal.height) / 2);
        //        offX = ((this.getWidth() - internal.width) / 2) + ((-outliers.x) - (outliers.width + outliers.x - internal
        // .width));
        //        offY = ((this.getHeight() - internal.height) / 2) + ((-outliers.y) - (outliers.height + outliers.y - internal
        // .height));
        //        super.paintComponent(graphics);
        Graphics2D g = (Graphics2D) graphics;
        g.setColor(BG_COLOR);
        g.fillRect(0, 0, getWidth(), getHeight());
        if(background != null) {
            if(currentbg == null) {
                currentbg = toCompatibleImage(resizeImage(background));
            }
            g.drawImage(currentbg, offX, offY, this);
        } else {
            g.setColor(Color.WHITE.darker().darker());
            g.fillRect(offX, offY, (int) Math.round(screen.width * scale), (int) Math.round(screen.height * scale));
        }
        if(gridbg == null) {
            gridbg = toCompatibleImage(drawGrid());
        }
        g.drawImage(gridbg, offX, offY, this);
        if(elementImage == null) {
            BufferedImage img = new BufferedImage(screen.width + ( 2 * offX ),
                                                  screen.height + ( 2 * offY ),
                                                  BufferedImage.TYPE_INT_ARGB);
            Graphics2D ge = img.createGraphics();
            ge.translate(offX, offY);
            Collections.sort(elements, layerSort);
            for(Element element : elements) {
                ge.setComposite(acSimple);
                paintElement(element, ge);
            }
            ge.dispose();
            elementImage = toCompatibleImage(img);
        }
        g.drawImage(elementImage, 0, 0, this);
        g.setComposite(acSelect);
        g.setColor(Color.CYAN.darker());
        g.fillRect(offX + selectRect.x + 1, offY + selectRect.y + 1, selectRect.width - 2, selectRect.height - 2);
        g.setColor(Color.BLUE);
        g.drawRect(offX + selectRect.x, offY + selectRect.y, selectRect.width - 1, selectRect.height - 1);
    }

    @Override
    public void setPreferredSize(Dimension preferredSize) {
        Dimension UISize = new Dimension(preferredSize.width + ( 2 * offX ), preferredSize.height + ( 2 * offY ));
        super.setPreferredSize(UISize);
        currentbg = null;
        gridbg = null;
        screen = preferredSize;
        //        long gcm = gcm(hudRes.width, hudRes.height);
        long resX = screen.width;
        long resY = screen.height;
        double m = resX / (double) resY;
        //        System.out.println(resX + "/" + resY + "=" + m);
        //        System.out.println((resX / gcm) + ":" + (resY / gcm) + " = " + Math.round(m * 480) + "x" + 480);
        internal = new Dimension((int) Math.round(m * 480), 480);
        repaint();
    }

    private BufferedImage resizeImage(Image image) { // TODO: aspect ratio tuning
        int w = screen.width;
        int h = screen.height;
        int type = BufferedImage.TYPE_INT_ARGB;
        BufferedImage resizedImage = new BufferedImage(w, h, type);
        Graphics2D g = resizedImage.createGraphics();
        g.setComposite(acSimple);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int proposedWidth = Math.round(( h / (float) image.getHeight(null) ) * image.getWidth(null));
        int excess = Math.abs(proposedWidth - w) / 2;
        g.drawImage(image, -excess, 0, w + ( 2 * excess ), h, this); // should scale most images correctly
        g.dispose();
        return resizedImage;
    }

    // as soon as the height drops below 480, stops rendering
    private BufferedImage drawGrid() {
        BufferedImage img = new BufferedImage(screen.width, screen.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setComposite(acGrid);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        g.setColor(GRID_COLOR);
        int w = screen.width;
        int h = screen.height;
        int i = minGridSpacing;
        if(i < 0) {
            return img;
        }
        if(i < 2) { // optimize for small numbers, stop division by zero
            g.fillRect(0, 0, screen.width, screen.height);
            return img;
        }
        int cross = 0;
        int maxX = w - ( w % i );
        int maxY = h - ( h % i );
        double multX = screen.width / (double) internal.width;
        double multY = screen.height / (double) internal.height;
        for(int y = -1; y <= ( maxY / i ); y++) {
            for(int x = -1; x <= ( maxX / i ); x++) {
                int dx = (int) Math.round(( maxX * x * i * multX ) / maxX);
                int dy = (int) Math.round(( maxY * y * i * multY ) / maxY);
                g.drawLine(dx + cross, dy + cross, dx - ( 1 + cross ), dy - ( 1 + cross ));
                g.drawLine(dx - ( 1 + cross ), dy + cross, dx + cross, dy - ( 1 + cross ));
            }
        }
        g.dispose();
        return img;
    }

    private void paintElement(Element e, Graphics2D g) {
        if(( e.getWidth() > 0 ) && ( e.getHeight() > 0 )) { // invisible? don't waste time
            int elementX = (int) Math.round(e.getX() * ( screen.width / internal.width ) * scale);
            int elementY = (int) Math.round(e.getY() * ( screen.height / internal.height ) * scale);
            int elementW = (int) Math.round(e.getWidth() * ( screen.width / internal.width ) * scale);
            int elementH = (int) Math.round(e.getHeight() * ( screen.height / internal.height ) * scale);
            if(selectedElements.contains(e)) {
                elementX += _offX;
                elementY += _offY;
            }
            Shape bounds = new Rectangle(elementX, elementY, elementW, elementH);
            Shape clip = g.getClip();
            g.setClip(bounds);
            if(e.getFgColor() != null) {
                g.setColor(e.getFgColor());
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, elementBgAlpha));
                g.fillRect(elementX, elementY, elementW - 1, elementH - 1);
            }
            if(e.getImage() != null) {
                if(e.getFgColor() != null) {
                    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, e.getFgColor().getAlpha()));
                }
                g.drawImage(e.getImage(), elementX, elementY, elementW, elementH, this);
            }
            if(selectedElements.contains(e)) {
                g.setColor(Color.CYAN);
            } else {
                g.setColor(Color.GREEN);
            }
            g.drawRect(elementX, elementY, elementW - 1, elementH - 1);
            if(hoveredElement == e) {
                g.setColor(new Color(255 - g.getColor().getRed(), 255 - g.getColor().getGreen(), 255 - g.getColor().getBlue()));
                g.setComposite(acSimple);
                //                g.drawRect(elementX + offX, elementY + offY, e.getWidth() - 1, e.getHeight() - 1); // border
                g.drawRect(elementX + 1, elementY + 1, elementW - 3, elementH - 3); // inner
                //                g.drawRect(elementX + offX - 1, elementY + offY - 1, e.getWidth() + 1,
                // e.getHeight() + 1); // outer
            }
            if(( e.getLabelText() != null ) && !e.getLabelText().isEmpty()) {
                if(e.getFgColor() != null) {
                    g.setColor(e.getFgColor());
                } else {
                    g.setColor(Color.WHITE);
                }
                g.setComposite(acSimple);
                int screenRes = Toolkit.getDefaultToolkit().getScreenResolution();
                int fontSize = (int) Math.round(( 14.0 * screenRes ) / 72.0); // Java2D = 72 DPI
                if(e.getFont() == null) {
                    e.setFont(g.getFont()); // a default
                }
                FontMetrics fm = getFontMetrics(e.getFont());
                int width = fm.stringWidth(e.getLabelText());
                g.setFont(e.getFont());
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if(e.getTextAlignment() == Alignment.Left) {
                    g.drawString(e.getLabelText(), elementX, elementY + fontSize);
                } else if(e.getTextAlignment() == Alignment.Right) {
                    g.drawString(e.getLabelText(), ( elementX + elementW ) - width, elementY + fontSize);
                } else if(e.getTextAlignment() == Alignment.Center) {
                    g.drawString(e.getLabelText(), elementX + ( ( elementW - width ) / 2 ), elementY + fontSize);
                }
            }
            g.setClip(clip);
        }
        //        for(int i = 0; i < e.children.size(); i++) {
        //            paintElement(e.children.get(i), g);
        //        }
    }
}
