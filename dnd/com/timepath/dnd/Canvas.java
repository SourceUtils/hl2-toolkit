package com.timepath.dnd;

import com.timepath.math.Vector3;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/**
 *
 * @author timepath
 */
public class Canvas extends JPanel {

    private static final long serialVersionUID = 1L;

    private ArrayList<Entity> entities = new ArrayList<Entity>(), hovered = new ArrayList<Entity>();

    public void addEntity(Entity e) {
        entities.add(e);
    }

    private Canvas() {
    }

    public static Canvas create() {
        final Canvas c = new Canvas();
        MouseAdapter ma = new MouseAdapter() {

            @Override
            public void mouseMoved(MouseEvent e) {
                c.mouseMoved(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                c.mousePressed(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                c.mouseDragged(e);
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                c.mouseWheelMoved(e);
            }

        };
        c.addMouseListener(ma);
        c.addMouseMotionListener(ma);
        c.addMouseWheelListener(ma);
        return c;
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
        drawEnvironment(g);
        drawEntities(g);
    }

    private void drawEnvironment(Graphics2D g) {
    }

    private void drawEntities(Graphics2D g) {
        for(int i = 0; i < entities.size(); i++) {
            Entity e = entities.get(i);
            if(hovered.contains(e)) {
                g.setColor(Color.BLUE);
                ring(g, e.getLocation(), e.getSize().mult(2));
            }

            g.setColor(Color.GREEN);
            plotPoint(g, e.getLocation(), e.getSize());

            Vector3 scr = toScreen(e.getLocation());
            g.setColor(Color.YELLOW);
            g.drawLine((int)scr.getX(), (int)scr.getY(), (int)(scr.getX() + (e.getDirection().getX() * e.getSize().getX() * focus.getZ())), (int)(scr.getY() + (e.getDirection().getY() * e.getSize().getY() * focus.getZ())));
            String n = e.getName();
            if(n != null) {
                g.setColor(Color.BLACK);
                g.drawString(n, scr.getX() - ((g.getFontMetrics().stringWidth(n) / 2)), (scr.getY() + (e.getSize().getY() * focus.getZ())) + (g.getFontMetrics().getHeight() * 1.5f));
            }
        }
    }

    private void plotPoint(Graphics2D g, Vector3 p, Vector3 b) {
        Vector3 pos = toScreen(p.add(-(b.getX() / 2), -(b.getY() / 2), 0));
        g.fillOval((int) pos.getX(), (int) pos.getY(), (int) (b.getX() * focus.getZ()), (int) (b.getY() * focus.getZ()));
    }

    private void ring(Graphics2D g, Vector3 p, Vector3 b) {
        Vector3 pos = toScreen(p.add(-(b.getX() / 2), -(b.getY() / 2), 0));
        g.drawOval((int) pos.getX(), (int) pos.getY(), (int) (b.getX() * focus.getZ()), (int) (b.getY() * focus.getZ()));
    }

    private Vector3 focus = new Vector3(0, 0, 10);

    private Vector3 toScreen(Vector3 v) {
        return new Vector3((v.getX() * focus.getZ()) - focus.getX(),
                           (v.getY() * focus.getZ()) - focus.getY(),
                           (v.getZ() * focus.getZ()) - focus.getZ());
    }

    private float toWorld(int i) {
        return (i + focus.getX()) / focus.getZ();
    }

    private Point initialDragPoint = new Point();

    private void mousePressed(MouseEvent e) {
        initialDragPoint = e.getPoint();
    }

    private void mouseDragged(MouseEvent e) {
        e.translatePoint(-initialDragPoint.x, -initialDragPoint.y);
        if(SwingUtilities.isMiddleMouseButton(e)) {
            focus.addLocal(-e.getX(), -e.getY(), 0);
            this.repaint();
        }
        e.translatePoint(initialDragPoint.x, initialDragPoint.y);
        initialDragPoint = e.getPoint();
    }

    private static final Logger LOG = Logger.getLogger(Canvas.class.getName());

    private void mouseWheelMoved(MouseWheelEvent e) {
        focus.addLocal(0, 0, -e.getWheelRotation());
        if(focus.getZ() < 1) {
            focus.setZ(1);
        }
        this.repaint();
    }

    private void mouseMoved(MouseEvent e) {
        Vector3 p = new Vector3(e.getX(), e.getY(), 0);
        boolean change = false;
        hovered.clear();
        for(int i = 0; i < entities.size(); i++) {
            Entity en = entities.get(i);
            float d = p.distance(toScreen(en.getLocation()).transform(1, 1, 0));
            if(d <= 1 * focus.getZ()) {
                hovered.add(en);
                change = true;
            }
        }
        this.repaint();
    }
}
