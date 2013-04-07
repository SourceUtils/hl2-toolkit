package com.timepath.hl2.io;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Logger;

/**
 *
 * Valve Bitmap Font
 *
 * @author timepath
 */
public class VBF {

    private static final Logger LOG = Logger.getLogger(VBF.class.getName());

    private static int expectedHeader = (('V') | ('F' << 8) | ('N' << 16) | ('T' << 24));

    private static int expectedVersion = 3;

    private int version;

    private short width;

    public short getWidth() {
        return width;
    }

    private short height;

    public short getHeight() {
        return height;
    }

    /**
     * style flags
     *
     * #define BF_BOLD	0x0001
     * #define BF_ITALIC	0x0002
     * #define BF_OUTLINED	0x0004
     * #define BF_DROPSHADOW	0x0008
     * #define BF_BLURRED	0x0010
     * #define BF_SCANLINES	0x0020
     * #define BF_ANTIALIASED	0x0040
     * #define BF_CUSTOM	0x0080
     */
    private short flags;

    private short ascent;

    /**
     * Maps characters to the glyph table
     */
    private byte[] table;
    
    public byte[] getTable() {
        return table;
    }

    private BitmapGlyph[] glyphs;
    
    public BitmapGlyph[] getGlyphs() {
        return glyphs;
    }

    private VBF() {
    }

    public static VBF create() {
        VBF v = new VBF();
        v.version = 3;
        return v;
    }

    public static VBF load(InputStream is) throws IOException {
        VBF v = new VBF();
        byte[] array = new byte[is.available()];
        is.read(array);
        ByteBuffer buf = ByteBuffer.wrap(array);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        //<editor-fold defaultstate="collapsed" desc="Parse">
        int header = buf.getInt();
        v.version = buf.getInt();
        v.width = buf.getShort();
        v.height = buf.getShort();
        short maxcharwidth = buf.getShort();
        short maxcharheight = buf.getShort();
        v.flags = buf.getShort();
        v.ascent = buf.getShort();
        /**
         * This number is 1 higher because there is a 'default' glyph
         */
        short total = buf.getShort();

        v.table = new byte[256];
        buf.get(v.table);

        // BitmapGlyph @ offset 278
        v.glyphs = new BitmapGlyph[total];

        for(int i = 0; i < v.glyphs.length; i++) {
            BitmapGlyph g = new BitmapGlyph();
            g.bounds = new Rectangle(buf.getShort(), buf.getShort(), buf.getShort(), buf.getShort());
            g.a = buf.getShort();
            g.b = buf.getShort();
            g.c = buf.getShort();
            v.glyphs[i] = g;
        }
        //</editor-fold>

        //<editor-fold defaultstate="collapsed" desc="Debug">
        Object[][] dbg = {
            {"Header = ", header},
            {"Version = ", v.version},
            {"Width = ", v.width},
            {"Height = ", v.height},
            {"MaxCharWidth = ", maxcharwidth},
            {"MaxCharHeight = ", maxcharheight},
            {"Flags = ", v.flags},
            {"Ascent = ", v.ascent},
            {"Total = ", total}
        };

        for(int i = 0; i < dbg.length; i++) {
            StringBuilder sb = new StringBuilder();
            for(int x = 0; x < dbg[i].length; x++) {
                sb.append(dbg[i][x]);
            }
            System.out.println(sb.toString());
        }

        for(char i = 0; i < v.table.length; i++) { // for each character
            int glyphIndex = v.table[i];
            if(glyphIndex == 0) { // don't care about the default glyph
                continue;
            }
            BitmapGlyph g = v.glyphs[glyphIndex];
            System.out.println(i + ": (" + g.bounds + ")\t{" + g.a + ", " + g.b + ", " + g.c + "}");
        }
        //</editor-fold>
        return v;
    }

    public static class BitmapGlyph {

        private Rectangle bounds;
        
        public Rectangle getBounds() {
            return bounds;
        }

        short a, b, c; // b seems to equal bounds.w

    }

}