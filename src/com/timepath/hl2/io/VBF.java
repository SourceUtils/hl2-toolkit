package com.timepath.hl2.io;

import java.awt.Rectangle;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Valve Bitmap Font
 *
 * @author TimePath
 */
public class VBF {

    private static final Logger LOG = Logger.getLogger(VBF.class.getName());

    private static final int expectedHeader = (('V') | ('F' << 8) | ('N' << 16) | ('T' << 24));

    private static final int expectedVersion = 3;

    public static VBF load(InputStream is) throws IOException {
        VBF v = new VBF();
        byte[] array = new byte[is.available()]; // XXX: TODO
        is.read(array);
        ByteBuffer buf = ByteBuffer.wrap(array);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        int header = buf.getInt();
        int version = buf.getInt();

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

        buf.get(v.table);

        // BitmapGlyph @ offset 278
        v.glyphs.ensureCapacity(total);

        for(int i = 0; i < total; i++) {
            BitmapGlyph g = new BitmapGlyph();
            g.setIndex((byte) i);
            g.bounds = new Rectangle(buf.getShort(), buf.getShort(), buf.getShort(), buf.getShort());
            g.a = buf.getShort();
            g.b = buf.getShort();
            g.c = buf.getShort();
            v.glyphs.add(g);
        }

        // Debugging
        Object[][] dbg = {
            {"Header = ", header},
            {"Version = ", version},
            {"Width = ", v.width},
            {"Height = ", v.height},
            {"MaxCharWidth = ", maxcharwidth},
            {"MaxCharHeight = ", maxcharheight},
            {"Flags = ", v.flags},
            {"Ascent = ", v.ascent},
            {"Total = ", total}
        };

        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < dbg.length; i++) {
            for(Object item : dbg[i]) {
                sb.append(item);
            }
            if(i < dbg.length) {
                sb.append("\n");
            }
        }
        LOG.fine(sb.toString());

        for(char i = 0; i < v.table.length; i++) { // for each character
            int glyphIndex = v.table[i];
            if(glyphIndex == 0) { // don't care about the default glyph
                continue;
            }
            BitmapGlyph g = v.glyphs.get(glyphIndex);
            LOG.log(Level.FINE, "{0}: ({1})\t'{'{2}, {3}, {4}'}'", new Object[] {i, g.bounds, g.a, g.b, g.c});
        }
        return v;
    }

    private short width;

    private short height;

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
    private final byte[] table = new byte[256];

    private final ArrayList<BitmapGlyph> glyphs = new ArrayList<BitmapGlyph>();

    public VBF() {
    }

    public void setWidth(int width) {
        this.width = (short) width;
    }

    public short getWidth() {
        return width;
    }

    public void setHeight(int height) {
        this.height = (short) height;
    }

    public short getHeight() {
        return height;
    }

    public byte[] getTable() {
        return table;
    }

    public ArrayList<BitmapGlyph> getGlyphs() {
        return glyphs;
    }

    public boolean hasGlyph(int i) {
        for(BitmapGlyph g : getGlyphs()) {
            if(g.getIndex() == i) {
                return true;
            }
        }
        return false;
    }

    public void save(File f) throws IOException {
        f.getParentFile().mkdirs();
        f.createNewFile();
        OutputStream fos = new BufferedOutputStream(new FileOutputStream(f));
        ByteBuffer buf = ByteBuffer.allocate(22 + 256 + (glyphs.size() * 14));
        buf.order(ByteOrder.LITTLE_ENDIAN);

        buf.putInt(VBF.expectedHeader);
        buf.putInt(VBF.expectedVersion);
        buf.putShort(width);
        buf.putShort(height);
        short maxcharwidth = 0;
        short maxcharheight = 0;
        for(int i = 0; i < glyphs.size(); i++) {
            if(glyphs.get(i).getBounds() == null) {
                glyphs.get(i).setBounds(new Rectangle());
            }
            Rectangle r = glyphs.get(i).getBounds();
            if(r.width == 0 || r.height == 0) {
                continue;
            }
            if(r.width > maxcharwidth) {
                maxcharwidth = (short) r.width;
            }
            if(r.height > maxcharheight) {
                maxcharheight = (short) r.height;
            }
        }
        buf.putShort(maxcharwidth);
        buf.putShort(maxcharheight);
        buf.putShort(flags);
        buf.putShort(ascent);
        buf.putShort((short) glyphs.size());

        buf.put(table);

        for(BitmapGlyph g : glyphs) {
            Rectangle b = g.bounds;
            buf.putShort((short) b.x);
            buf.putShort((short) b.y);
            int width = b.height != 0 ? b.width : 0;
            int height = b.width != 0 ? b.height : 0;
            buf.putShort((short) width);
            buf.putShort((short) height);
            buf.putShort(g.a);
            buf.putShort((short) width); // XXX: why?
            buf.putShort(g.c);
        }

        fos.write(buf.array());
        fos.flush();
        fos.close();
    }

    public static class BitmapGlyph {

        private byte index;

        private Rectangle bounds = new Rectangle();

        short a, b, c; // b seems to equal bounds.width

        public void setIndex(byte index) {
            this.index = index;
        }

        public byte getIndex() {
            return index;
        }

        public void setBounds(Rectangle bounds) {
            this.bounds = bounds;
        }

        public Rectangle getBounds() {
            return bounds;
        }

        @Override
        public String toString() {
            return "#" + index;
        }

    }

}
