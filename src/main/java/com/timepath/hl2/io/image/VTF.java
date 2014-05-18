package com.timepath.hl2.io.image;

import com.timepath.EnumFlags;
import com.timepath.StringUtils;
import com.timepath.io.utils.ViewableData;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.EnumSet;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * TODO: .360.vtf files seem to be a slightly different format... and LZMA compressed.
 *
 * @author TimePath
 */
public class VTF implements ViewableData {

    /**
     * 'VTF\0' as little endian
     */
    private static final int    HEADER               = 0x00_46_54_56;
    private static final Logger LOG                  = Logger.getLogger(VTF.class.getName());
    /**
     * 'CRC\2' as little endian
     */
    private static final int    VTF_RSRC_TEXTURE_CRC = 0x02_43_52_43;
    private ByteBuffer  buf;
    private float       bumpScale;
    private int         depth;
    private int         flags;
    private ImageFormat format;
    private int         frameCount;
    /**
     * Zero indexed
     */
    private int         frameFirst;
    private int         headerSize;
    private int         height;
    private int         mipCount;
    private float[]     reflectivity;
    private ImageFormat thumbFormat;
    private int         thumbHeight;
    private Image       thumbImage;
    private int         thumbWidth;
    private int[]       version;
    private int         width;

    public VTF() {}

    public static VTF load(String s) throws IOException {
        return load(new FileInputStream(s));
    }

    public static VTF load(InputStream is) throws IOException {
        VTF vtf = new VTF();
        if(vtf.loadFromStream(is)) {
            return vtf;
        }
        return null;
    }

    boolean loadFromStream(InputStream is) throws IOException {
        int magic = is.read() | ( is.read() << 8 ) | ( is.read() << 16 ) | ( is.read() << 24 );
        if(magic != HEADER) {
            LOG.log(Level.FINE, "Invalid VTF file: {0}", magic);
            return false;
        }
        byte[] array = new byte[4 + is.available()];
        is.read(array, 4, array.length - 4);
        buf = ByteBuffer.wrap(array);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.position(4);
        version = new int[] { buf.getInt(), buf.getInt() };
        headerSize = buf.getInt();
        width = buf.getShort();
        height = buf.getShort();
        flags = buf.getInt();
        EnumSet<CompiledVtfFlags> enumSet = EnumFlags.decode(flags, CompiledVtfFlags.class);
        frameCount = buf.getShort();
        frameFirst = buf.getShort();
        buf.get(new byte[4]);
        reflectivity = new float[] { buf.getFloat(), buf.getFloat(), buf.getFloat() };
        buf.get(new byte[4]);
        bumpScale = buf.getFloat();
        format = ImageFormat.getEnumForIndex(buf.getInt());
        mipCount = buf.get();
        thumbFormat = ImageFormat.getEnumForIndex(buf.getInt());
        thumbWidth = buf.get();
        thumbHeight = buf.get();
        depth = buf.getShort();
        Object[][] debug = {
                { "Width = ", width },
                { "Height = ", height },
                { "Frames = ", frameCount },
                { "Flags = ", enumSet },
                { "Format = ", format },
                { "MipCount = ", mipCount },
        };
        if(LOG.isLoggable(Level.FINE)) {LOG.fine(StringUtils.fromDoubleArray(debug, "VTF:"));}
        return true;
    }

    /**
     * @return the bumpScale
     */
    public float getBumpScale() {
        return bumpScale;
    }

    public void getControls() {
        buf.position(headerSize - 8); // 8 bytes for CRC or other things. I have no idea what the data after the first 64 bytes up
        // until here are for
        int crcHead = buf.getInt();
        int crc = buf.getInt();
        if(crcHead == VTF_RSRC_TEXTURE_CRC) {
            LOG.log(Level.INFO, "CRC=0x{0}", Integer.toHexString(crc).toUpperCase());
        } else {
            LOG.log(Level.WARNING, "CRC header {0} is invalid", crcHead);
        }
    }

    /**
     * @return the depth
     */
    public int getDepth() {
        return depth;
    }

    /**
     * @return the flags
     */
    public int getFlags() {
        return flags;
    }

    /**
     * @return the format
     */
    public ImageFormat getFormat() {
        return format;
    }

    /**
     * @return the frameCount
     */
    public int getFrameCount() {
        return frameCount;
    }

    /**
     * @return the frameFirst
     */
    public int getFrameFirst() {
        return frameFirst;
    }

    /**
     * @return the headerSize
     */
    public int getHeaderSize() {
        return headerSize;
    }

    /**
     * @return the height
     */
    public int getHeight() {
        return height;
    }

    @Override
    public Icon getIcon() {
        return new ImageIcon(getThumbImage());
    }

    public Image getThumbImage() {
        if(thumbImage == null) {
            buf.position(headerSize);
            byte[] thumbData = new byte[( Math.max(thumbWidth, 4) * Math.max(thumbHeight, 4) ) /
                                        2]; // DXT1. Each 'block' is 4*4 pixels. 16 pixels become 8
            // bytes
            buf.get(thumbData);
            thumbImage = DXTLoader.loadDXT1(thumbData, thumbWidth, thumbHeight);
        }
        return thumbImage;
    }

    /**
     * Return the image for the given level of detail
     *
     * @param level
     *         From 0 to {@link #mipCount}-1
     *
     * @return *
     *
     * @throws IOException
     */
    public Image getImage(int level) throws IOException {
        return getImage(level, frameFirst);
    }

    /**
     * Return the image for the given level of detail and frame
     *
     * @param level
     *         From 0 to {@link #mipCount}-1
     * @param frame
     *         From 0 to {@link #frameCount}-1
     */
    public Image getImage(int level, int frame) {
        if(( level < 0 ) || ( level >= mipCount )) {
            return null;
        }
        if(( frame < 0 ) || ( frame >= frameCount )) {
            return null;
        }
        int thumbLen = ( Math.max(thumbWidth, 4) * Math.max(thumbHeight, 4) ) / 2; // Thumbnail is a minimum of 4*4
        if(( thumbWidth == 0 ) || ( thumbHeight == 0 )) {
            thumbLen = 0;
        }
        buf.position(headerSize + thumbLen);
        int[] sizesX = new int[mipCount]; // smallest -> largest {1, 2, 4, 8, 16, 32, 64, 128}
        int[] sizesY = new int[mipCount];
        for(int n = 0; n < mipCount; n++) {
            sizesX[mipCount - 1 - n] = Math.max(width >>> n, 1);
            sizesY[mipCount - 1 - n] = Math.max(height >>> n, 1);
        }
        BufferedImage image = null;
        for(int i = 0; i < mipCount; i++) {
            int w = sizesX[i];
            int h = sizesY[i];
            LOG.log(Level.FINE, "{0}, {1}", new Object[] { w, h });
            int nBytes = format.getBytes(w, h);
            if(i == ( mipCount - level - 1 )) {
                byte[] imageData = new byte[nBytes * frameCount];
                try {
                    buf.get(imageData);
                } catch(BufferUnderflowException ignored) {
                    LOG.log(Level.SEVERE, "Underflow; {0}", nBytes);
                }
                System.arraycopy(imageData, frame * nBytes, imageData, 0, nBytes);
                LOG.log(Level.INFO, "VTF format {0}", format);
                image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = (Graphics2D) image.getGraphics();
                g.drawImage(format.load(imageData, w, h), 0, 0, w, h, null);
            } else {
                buf.get(new byte[nBytes * frameCount]);
            }
        }
        return image;
    }

    /**
     * @return the mipCount
     */
    public int getMipCount() {
        return mipCount;
    }

    /**
     * @return the reflectivity
     */
    public float[] getReflectivity() {
        return reflectivity;
    }

    /**
     * @return the thumbFormat
     */
    public ImageFormat getThumbFormat() {
        return thumbFormat;
    }

    /**
     * @return the thumbHeight
     */
    public int getThumbHeight() {
        return thumbHeight;
    }

    /**
     * @return the thumbWidth
     */
    public int getThumbWidth() {
        return thumbWidth;
    }

    /**
     * @return the version
     */
    public int[] getVersion() {
        return version;
    }

    /**
     * @return the width
     */
    public int getWidth() {
        return width;
    }
}
