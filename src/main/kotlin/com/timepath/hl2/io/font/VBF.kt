package com.timepath.hl2.io.font


import java.awt.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.ArrayList
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Valve Bitmap Font

 * @author TimePath
 * *
 * @see [https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/public/BitmapFontFile.h](null)

 * @see [https://github.com/LestaD/SourceEngine2007/blob/master/src_main/utils/xbox/FontMaker/glyphs.cpp](null)
 */
public class VBF() {
    val glyphs: MutableList<BitmapGlyph> = ArrayList(1)
    /** Maps characters to the glyph table */
    public val table: ByteArray = ByteArray(256)
    private var ascent: Short = 0
    /**
     * style flags
     *
     * BF_BOLD = 0x0001
     * BF_ITALIC = 0x0002
     * BF_OUTLINED = 0x0004
     * BF_DROPSHADOW = 0x0008
     * BF_BLURRED = 0x0010
     * BF_SCANLINES = 0x0020
     * BF_ANTIALIASED = 0x0040
     * BF_CUSTOM = 0x0080
     */
    private var flags: Short = 0
    public var height: Short = 0
    public var width: Short = 0

    throws(javaClass<IOException>())
    public constructor(input: InputStream) : this() {
        val array = ByteArray(input.available()) // XXX: TODO
        input.read(array)
        val buf = ByteBuffer.wrap(array)
        buf.order(ByteOrder.LITTLE_ENDIAN)
        val id = buf.getInt()
        val version = buf.getInt()
        width = buf.getShort()
        height = buf.getShort()
        val maxCharWidth = buf.getShort()
        val maxCharHeight = buf.getShort()
        flags = buf.getShort()
        ascent = buf.getShort()
        /**
         * This number is 1 higher because there is a 'default' glyph
         */
        val numGlyphs = buf.getShort()
        buf.get(table)
        // BitmapGlyph @ offset 278
        (glyphs as? ArrayList)?.let { it.ensureCapacity(numGlyphs.toInt()) }
        for (i in 0..numGlyphs - 1) {
            val g = BitmapGlyph()
            g.index = i.toByte()
            g.bounds = Rectangle(buf.getShort().toInt(), buf.getShort().toInt(), buf.getShort().toInt(), buf.getShort().toInt())
            g.a = buf.getShort()
            g.b = buf.getShort()
            g.c = buf.getShort()
            glyphs.add(g)
        }
        // Debugging
        val dbg = array(
                array("Header = ", id),
                array("Version = ", version),
                array("Width = ", width),
                array("Height = ", height),
                array("MaxCharWidth = ", maxCharWidth),
                array("MaxCharHeight = ", maxCharHeight),
                array("Flags = ", flags),
                array("Ascent = ", ascent),
                array("Total = ", numGlyphs)
        )
        val sb = StringBuilder(0)
        for (i in dbg.indices) {
            for (item in dbg[i]) {
                sb.append(item)
            }
            if (i < dbg.size()) {
                sb.append('\n')
            }
        }
        LOG.fine(sb.toString())
        for (i in table.indices) {
            val glyphIndex = table[i].toInt()
            if (glyphIndex == 0) {
                // don't care about the default glyph
                continue
            }
            val g = glyphs.get(glyphIndex)
            LOG.log(Level.FINE, "{0}: ({1})\t'{'{2}, {3}, {4}'}'", array(i, g.bounds, g.a, g.b, g.c))
        }
    }

    public fun contains(i: Int): Boolean {
        for (g in glyphs) {
            if (g.index.toInt() == i) {
                return true
            }
        }
        return false
    }

    throws(javaClass<IOException>())
    public fun save(output: OutputStream) {
        val buf = ByteBuffer.allocate(22 + 256 + (glyphs.size() * 14))
        buf.order(ByteOrder.LITTLE_ENDIAN)
        buf.putInt(BITMAPFONT_ID)
        buf.putInt(BITMAPFONT_VERSION)
        buf.putShort(width)
        buf.putShort(height)
        var maxCharWidth: Short = 0
        var maxCharHeight: Short = 0
        for (glyph in glyphs) {
            val r = glyph.bounds
            if ((r.width != 0) && (r.height != 0)) {
                maxCharWidth = Math.max(maxCharWidth.toInt(), r.width).toShort()
                maxCharHeight = Math.max(maxCharHeight.toInt(), r.height).toShort()
            }
        }
        buf.putShort(maxCharWidth)
        buf.putShort(maxCharHeight)
        buf.putShort(flags)
        buf.putShort(ascent)
        buf.putShort(glyphs.size().toShort())
        buf.put(table)
        for (g in glyphs) {
            val b = g.bounds
            buf.putShort(b.x.toShort())
            buf.putShort(b.y.toShort())
            val width = if ((b.height == 0)) 0 else b.width
            val height = if ((b.width == 0)) 0 else b.height
            buf.putShort(width.toShort())
            buf.putShort(height.toShort())
            buf.putShort(g.a)
            buf.putShort(width.toShort()) // XXX: why?
            buf.putShort(g.c)
        }
        output.write(buf.array())
        output.flush()
        output.close()
    }

    /**
     * b seems to equal bounds.width
     * @see [source](https://github.com/LestaD/SourceEngine2007/blob/master/src_main/utils/xbox/FontMaker/glyphs.cpp.L1407)
     * ...
     * 'ABC structure' is a thing
     * @see [msdn](http://msdn.microsoft.com/en-us/library/windows/desktop/dd162454)
     */
    public class BitmapGlyph(
            var a: Short = 0,
            var b: Short = 0,
            var c: Short = 0,
            public var bounds: Rectangle = Rectangle(),
            public var index: Byte = 0) {

        override fun toString() = "#$index"
    }

    companion object {

        private val BITMAPFONT_ID = "VFNT".mapIndexed { i, it -> it.toInt() shl (i * 8) }.fold(0) { p, it -> p or it }
        private val BITMAPFONT_VERSION = 3
        private val LOG = Logger.getLogger(javaClass<VBF>().getName())
    }
}
