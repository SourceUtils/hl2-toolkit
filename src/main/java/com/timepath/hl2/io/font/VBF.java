package com.timepath.hl2.io.font;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Valve Bitmap Font
 *
 * @author TimePath
 * @see <a>https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/public/BitmapFontFile.h</a>
 * @see <a>https://github.com/LestaD/SourceEngine2007/blob/master/src_main/utils/xbox/FontMaker/glyphs.cpp</a>
 */
public class VBF {

    private static final int BITMAPFONT_ID = 'V' | ('F' << 8) | ('N' << 16) | ('T' << 24);
    private static final int BITMAPFONT_VERSION = 3;
    private static final Logger LOG = Logger.getLogger(VBF.class.getName());
    private final ArrayList<BitmapGlyph> glyphs = new ArrayList<>(1);
    /**
     * Maps characters to the glyph table
     */
    private final byte[] table = new byte[256];
    private short ascent;
    /**
     * style flags
     * <table>
     * <tr>
     * <td>BF_BOLD</td>
     * <td>0x0001</td>
     * </tr><tr>
     * <td>BF_ITALIC</td>
     * <td>0x0002</td>
     * </tr><tr>
     * <td>BF_OUTLINED</td>
     * <td>0x0004</td>
     * </tr><tr>
     * <td>BF_DROPSHADOW</td>
     * <td>0x0008</td>
     * </tr><tr>
     * <td>BF_BLURRED</td>
     * <td>0x0010</td>
     * </tr><tr>
     * <td>BF_SCANLINES</td>
     * <td>0x0020</td>
     * </tr><tr>
     * <td>BF_ANTIALIASED</td>
     * <td>0x0040</td>
     * </tr><tr>
     * <td>BF_CUSTOM</td>
     * <td>0x0080</td>
     * </tr>
     * </table>
     */
    private short flags;
    private short pageHeight;
    private short pageWidth;

    public VBF() {
    }

    public VBF(InputStream is) throws IOException {
        byte[] array = new byte[is.available()]; // XXX: TODO
        is.read(array);
        ByteBuffer buf = ByteBuffer.wrap(array);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int id = buf.getInt();
        int version = buf.getInt();
        pageWidth = buf.getShort();
        pageHeight = buf.getShort();
        short maxCharWidth = buf.getShort();
        short maxCharHeight = buf.getShort();
        flags = buf.getShort();
        ascent = buf.getShort();
        /**
         * This number is 1 higher because there is a 'default' glyph
         */
        short numGlyphs = buf.getShort();
        buf.get(table);
        // BitmapGlyph @ offset 278
        glyphs.ensureCapacity(numGlyphs);
        for (int i = 0; i < numGlyphs; i++) {
            BitmapGlyph g = new BitmapGlyph();
            g.setIndex((byte) i);
            g.bounds = new Rectangle(buf.getShort(), buf.getShort(), buf.getShort(), buf.getShort());
            g.a = buf.getShort();
            g.b = buf.getShort();
            g.c = buf.getShort();
            glyphs.add(g);
        }
        // Debugging
        Object[][] dbg = {
                {"Header = ", id},
                {"Version = ", version},
                {"Width = ", pageWidth},
                {"Height = ", pageHeight},
                {"MaxCharWidth = ", maxCharWidth},
                {"MaxCharHeight = ", maxCharHeight},
                {"Flags = ", flags},
                {"Ascent = ", ascent},
                {"Total = ", numGlyphs}
        };
        StringBuilder sb = new StringBuilder(0);
        for (int i = 0; i < dbg.length; i++) {
            for (Object item : dbg[i]) {
                sb.append(item);
            }
            if (i < dbg.length) {
                sb.append('\n');
            }
        }
        LOG.fine(sb.toString());
        for (char i = 0; i < table.length; i++) { // for each character
            int glyphIndex = table[i];
            if (glyphIndex == 0) { // don't care about the default glyph
                continue;
            }
            BitmapGlyph g = glyphs.get(glyphIndex);
            LOG.log(Level.FINE, "{0}: ({1})\t'{'{2}, {3}, {4}'}'", new Object[]{i, g.bounds, g.a, g.b, g.c});
        }
    }

    public short getHeight() {
        return pageHeight;
    }

    public void setHeight(int height) {
        pageHeight = (short) height;
    }

    public byte[] getTable() {
        return table;
    }

    public short getWidth() {
        return pageWidth;
    }

    public void setWidth(int width) {
        pageWidth = (short) width;
    }

    public boolean hasGlyph(int i) {
        for (BitmapGlyph g : getGlyphs()) {
            if (g.getIndex() == i) {
                return true;
            }
        }
        return false;
    }

    public Collection<BitmapGlyph> getGlyphs() {
        return glyphs;
    }

    public void save(OutputStream os) throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(22 + 256 + (glyphs.size() * 14));
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.putInt(BITMAPFONT_ID);
        buf.putInt(BITMAPFONT_VERSION);
        buf.putShort(pageWidth);
        buf.putShort(pageHeight);
        short maxCharWidth = 0;
        short maxCharHeight = 0;
        for (BitmapGlyph glyph : glyphs) {
            Rectangle r = glyph.getBounds();
            if ((r.width != 0) && (r.height != 0)) {
                maxCharWidth = (short) Math.max(maxCharWidth, r.width);
                maxCharHeight = (short) Math.max(maxCharHeight, r.height);
            }
        }
        buf.putShort(maxCharWidth);
        buf.putShort(maxCharHeight);
        buf.putShort(flags);
        buf.putShort(ascent);
        buf.putShort((short) glyphs.size());
        buf.put(table);
        for (BitmapGlyph g : glyphs) {
            Rectangle b = g.getBounds();
            buf.putShort((short) b.x);
            buf.putShort((short) b.y);
            int width = (b.height == 0) ? 0 : b.width;
            int height = (b.width == 0) ? 0 : b.height;
            buf.putShort((short) width);
            buf.putShort((short) height);
            buf.putShort(g.a);
            buf.putShort((short) width); // XXX: why?
            buf.putShort(g.c);
        }
        os.write(buf.array());
        os.flush();
        os.close();
    }

    public static class BitmapGlyph {

        short a;
        /**
         * b seems to equal bounds.width
         *
         * @see <a>https://github.com/LestaD/SourceEngine2007/blob/master/src_main/utils/xbox/FontMaker/glyphs.cpp#L1407</a>
         * ...
         * 'ABC structure' is a thing
         * @see <a>http://msdn.microsoft.com/en-us/library/windows/desktop/dd162454(v=vs.85).aspx</a>
         */
        short b;
        Rectangle bounds = new Rectangle();
        short c;
        private byte index;

        public BitmapGlyph() {
        }

        public Rectangle getBounds() {
            return bounds;
        }

        public void setBounds(Rectangle bounds) {
            this.bounds = bounds;
        }

        public byte getIndex() {
            return index;
        }

        public void setIndex(byte index) {
            this.index = index;
        }

        @Override
        public String toString() {
            return "#" + index;
        }
    }
}
