package com.timepath.hl2.io.swing;

import com.timepath.hl2.io.VBF;
import com.timepath.hl2.io.VBF.BitmapGlyph;
import com.timepath.hl2.io.VTF;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JPanel;

/**
 *
 * @author timepath
 */
public class VBFCanvas extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = Logger.getLogger(VBFCanvas.class.getName());

    /**
     * Creates new form VBFTest
     */
    public VBFCanvas() {
    }

    private static int padding = 32 * 0;

    private static AffineTransform at = AffineTransform.getTranslateInstance(padding, padding);

    @Override
    protected void paintComponent(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, this.getWidth(), this.getHeight());
//        g.setTransform(at);
        if(img == null && vtf != null) {
            try {
                img = vtf.getImage(0);
            } catch(IOException ex) {
                Logger.getLogger(VBFCanvas.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        if(img != null) {
            g.drawImage(img, 0, 0, this);
        }
        if(this.vbf != null) {
            BitmapGlyph[] glyphs = vbf.getGlyphs();
            for(int i = 0; i < glyphs.length; i++) {
                if(glyphs[i] == null) {
                    continue;
                }
                Rectangle bounds = glyphs[i].getBounds();
                if(bounds == null) {
                    continue;
                }
                g.setColor(Color.GREEN);
                g.drawRect(bounds.x, bounds.y, bounds.width - 1, bounds.height - 1);
                char associatedCharacter = 0;
                for(char j = 0; j < vbf.getTable().length; j++) {
                    if(vbf.getTable()[j] == i) {
                        associatedCharacter = j;
                        break;
                    }
                }
//                 TODO: Negative font folor
//                Map<TextAttribute, Object> map = new Hashtable<TextAttribute, Object>();
//                map.put(TextAttribute.SWAP_COLORS, TextAttribute.SWAP_COLORS_ON);
//                map.put(TextAttribute.FOREGROUND, Color.BLACK);
//                map.put(TextAttribute.BACKGROUND, Color.TRANSLUCENT);
//                Font f = this.getFont().deriveFont(map);
//                g.setFont(f);
                g.drawString(Character.toString(associatedCharacter), bounds.x + 1, bounds.y + bounds.height - 1);
            }
        }
    }

    private VBF vbf;

    public void setVBF(VBF b) {
        this.vbf = b;
    }

    private Image img;

    private VTF vtf;

    public void setVTF(VTF t) {
        this.vtf = t;
    }
    
}
