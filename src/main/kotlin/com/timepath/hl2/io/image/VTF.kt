package com.timepath.hl2.io.image

import com.timepath.EnumFlags
import com.timepath.StringUtils
import com.timepath.io.utils.ViewableData
import java.awt.Graphics2D
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.logging.Level
import java.util.logging.Logger
import javax.swing.ImageIcon

/**
 * TODO: .360.vtf files seem to be a slightly different format... and LZMA compressed.
 *
 * @author TimePath
 */
public class VTF : ViewableData {
    private var buf: ByteBuffer? = null
    public var bumpScale: Float = 0f
        private set
    public var depth: Int = 0
        private set
    public var flags: Int = 0
        private set
    public var format: ImageFormat? = null
        private set
    public var frameCount: Int = 0
        private set
    /**
     * Zero indexed
     */
    public var frameFirst: Int = 0
        private set
    public var headerSize: Int = 0
        private set
    public var height: Int = 0
        private set
    public var mipCount: Int = 0
        private set
    public var reflectivity: FloatArray? = null
        private set
    public var thumbFormat: ImageFormat? = null
        private set
    public var thumbHeight: Int = 0
        private set
    private var thumbImage: Image? = null
    public var thumbWidth: Int = 0
        private set
    public var version: IntArray? = null
        private set
    public var width: Int = 0
        private set

    throws(IOException::class)
    fun loadFromStream(`is`: InputStream): Boolean {
        val magic = `is`.read() or (`is`.read() shl 8) or (`is`.read() shl 16)
        val type = `is`.read()
        if (magic != HEADER) {
            LOG.log(Level.FINE, "Invalid VTF file: {0}", magic)
            return false
        }
        val array = ByteArray(4 + `is`.available())
        `is`.read(array, 4, array.size() - 4)
        buf = ByteBuffer.wrap(array)
        buf!!.order(ByteOrder.LITTLE_ENDIAN)
        buf!!.position(4)
        version = intArrayOf(buf!!.getInt(), buf!!.getInt())
        headerSize = buf!!.getInt()
        if (type == 'X'.toInt()) {
            flags = buf!!.getInt()
        }
        width = buf!!.getShort().toInt()
        height = buf!!.getShort().toInt()
        if (type == 'X'.toInt()) {
            depth = buf!!.getShort().toInt()
        }
        if (type == 0) {
            flags = buf!!.getInt()
        }
        val enumSet = EnumFlags.decode<VTFFlags>(flags, javaClass<VTFFlags>())
        frameCount = buf!!.getShort().toInt()
        if (type == 0) {
            frameFirst = buf!!.getShort().toInt()
            buf!!.get(ByteArray(4))
        } else if (type == 'X'.toInt()) {
            val preloadDataSize = buf!!.getShort()
            val mipSkipCount = buf!!.get()
            val numResources = buf!!.get()
        }
        reflectivity = floatArrayOf(buf!!.getFloat().toFloat(), buf!!.getFloat().toFloat(), buf!!.getFloat().toFloat())
        if (type == 0) {
            buf!!.get(ByteArray(4))
        }
        bumpScale = buf!!.getFloat()
        format = ImageFormat.getEnumForIndex(buf!!.getInt())
        if (type == 0) {
            mipCount = buf!!.get().toInt()
            thumbFormat = ImageFormat.getEnumForIndex(buf!!.getInt())
            thumbWidth = buf!!.get().toInt()
            thumbHeight = buf!!.get().toInt()
            depth = buf!!.getShort().toInt()
        } else if (type == 'X'.toInt()) {
            val lowResImageSample = ByteArray(4)
            buf!!.get(lowResImageSample)
            val compressedSize = buf!!.getInt()
        }
        val debug = arrayOf(arrayOf("Width = ", width), arrayOf("Height = ", height), arrayOf("Frames = ", frameCount), arrayOf<Any?>("Flags = ", enumSet), arrayOf("Format = ", format), arrayOf("MipCount = ", mipCount))
        if (LOG.isLoggable(Level.FINE)) {
            LOG.fine(StringUtils.fromDoubleArray(debug, "VTF:"))
        }
        return true
    }

    public fun getControls() {
        buf!!.position(headerSize - 8) // 8 bytes for CRC or other things. I have no idea what the data after the first 64 bytes up
        // until here are for
        val crcHead = buf!!.getInt()
        val crc = buf!!.getInt()
        if (crcHead == VTF_RSRC_TEXTURE_CRC) {
            LOG.log(Level.INFO, "CRC=0x{0}", Integer.toHexString(crc).toUpperCase())
        } else {
            LOG.log(Level.WARNING, "CRC header {0} is invalid", crcHead)
        }
    }

    override fun getIcon() = ImageIcon(getThumbImage())

    public fun getThumbImage(): Image {
        if (thumbImage == null) {
            buf!!.position(headerSize)
            val thumbData = ByteArray((Math.max(thumbWidth, 4) * Math.max(thumbHeight, 4)) / 2) // DXT1. Each 'block' is 4*4 pixels. 16 pixels become 8
            // bytes
            buf!!.get(thumbData)
            thumbImage = DXTLoader.loadDXT1(thumbData, thumbWidth, thumbHeight)
        }
        return thumbImage!!
    }

    /**
     * Return the image for the given level of detail
     *
     * @param level From 0 to {@link #mipCount}-1
     * @return *
     * @throws IOException
     */
    throws(IOException::class)
    public fun getImage(level: Int): Image? = getImage(level, frameFirst)

    /**
     * Return the image for the given level of detail and frame
     *
     * @param level From 0 to {@link #mipCount}-1
     * @param frame From 0 to {@link #frameCount}-1
     */
    public fun getImage(level: Int, frame: Int): Image? {
        if ((level < 0) || (level >= mipCount)) {
            return null
        }
        if ((frame < 0) || (frame >= frameCount)) {
            return null
        }
        var thumbLen = (Math.max(thumbWidth, 4) * Math.max(thumbHeight, 4)) / 2 // Thumbnail is a minimum of 4*4
        if ((thumbWidth == 0) || (thumbHeight == 0)) {
            thumbLen = 0
        }
        buf!!.position(headerSize + thumbLen)
        val sizesX = IntArray(mipCount) // smallest -> largest {1, 2, 4, 8, 16, 32, 64, 128}
        val sizesY = IntArray(mipCount)
        for (n in 0..mipCount - 1) {
            sizesX[mipCount - 1 - n] = Math.max(width.ushr(n), 1)
            sizesY[mipCount - 1 - n] = Math.max(height.ushr(n), 1)
        }
        var image: BufferedImage? = null
        for (i in 0..mipCount - 1) {
            val w = sizesX[i]
            val h = sizesY[i]
            LOG.log(Level.FINE, "{0}, {1}", arrayOf<Any>(w, h))
            val nBytes = format!!.getBytes(w, h)
            if (i == (mipCount - level - 1)) {
                val imageData = ByteArray(nBytes * frameCount)
                try {
                    buf!![imageData]
                } catch (ignored: BufferUnderflowException) {
                    LOG.log(Level.SEVERE, "Underflow; {0}", nBytes)
                }

                System.arraycopy(imageData, frame * nBytes, imageData, 0, nBytes)
                LOG.log(Level.INFO, "VTF format {0}", format)
                image = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
                val g = image.getGraphics() as Graphics2D
                g.drawImage(format!!.load(imageData, w, h), 0, 0, w, h, null)
            } else {
                buf!![ByteArray(nBytes * frameCount)]
            }
        }
        return image
    }

    companion object {

        /**
         * 'VTF\0' as little endian
         */
        private val HEADER = 4609110
        private val LOG = Logger.getLogger(javaClass<VTF>().getName())
        /**
         * 'CRC\2' as little endian
         */
        private val VTF_RSRC_TEXTURE_CRC = 37966403

        throws(IOException::class)
        public fun load(s: String): VTF? {
            return load(FileInputStream(s))
        }

        throws(IOException::class)
        public fun load(`is`: InputStream): VTF? {
            val vtf = VTF()
            if (vtf.loadFromStream(`is`)) {
                return vtf
            }
            return null
        }
    }
}
