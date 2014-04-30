package com.timepath.hl2.io;

import com.timepath.EnumFlags;
import com.timepath.StringUtils;
import com.timepath.hl2.io.image.CompiledVtfFlags;
import com.timepath.hl2.io.image.ImageFormat;
import com.timepath.image.ImageUtils;
import com.timepath.io.utils.ViewableData;
import java.awt.Graphics2D;
import java.awt.Image;
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
import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * TODO: .360.vtf files seem to be a slightly different format... and LZMA compressed.
 *
 * @author TimePath
 */
public class VTF implements ViewableData {

    private static final int HEADER = (('V') | ('T' << 8) | ('F' << 16));

    private static final Logger LOG = Logger.getLogger(VTF.class.getName());

    protected static final int VTF_RSRC_TEXTURE_CRC = (('C') | ('R' << 8) | ('C' << 16) | ('\2' << 24));

    public static VTF load(String string) throws IOException {
        return load(new FileInputStream(string));
    }

    public static VTF load(InputStream is) throws IOException {
        VTF vtf = new VTF();
        if(vtf.loadFromStream(is)) {
            return vtf;
        }
        return null;
    }

    private ByteBuffer buf;

    private float bumpScale;

    private int depth;

    private int flags;

    private ImageFormat format;

    private int frameCount;

    /**
     * Zero indexed
     */
    private int frameFirst;

    private int headerSize;

    private int height;

    private int mipCount;

    private float[] reflectivity;

    private ImageFormat thumbFormat;

    private int thumbHeight;

    private Image thumbImage;

    private int thumbWidth;

    private int[] version;

    private int width;

    /**
     * @return the bumpScale
     */
    public float getBumpScale() {
        return bumpScale;
    }

    public void getControls() throws IOException {
        buf.position(this.headerSize - 8); // 8 bytes for CRC or other things. I have no idea what the data after the first 64 bytes up until here are for
        int crcHead = buf.getInt();
        int crc = buf.getInt();

        if(crcHead != VTF_RSRC_TEXTURE_CRC) {
            LOG.log(Level.WARNING, "CRC header {0} is invalid", crcHead);
        } else {
            LOG.log(Level.INFO, "CRC=0x{0}", Integer.toHexString(crc).toUpperCase());
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

    public Icon getIcon() {
        try {
            return new ImageIcon(this.getThumbImage());
        } catch(IOException ex) {
            LOG.log(Level.WARNING, null, ex);
        }
        return null;
    }

    /**
     * Return the image for the given level of detail
     * <p/>
     * @param level From 0 to {@link #mipCount}-1
     * <p/>
     * @return
     *         <p/>
     * @throws IOException
     */
    public Image getImage(int level) throws IOException {
        return getImage(level, this.frameFirst);
    }

    /**
     * Return the image for the given level of detail and frame
     * <p/>
     * @param level From 0 to {@link #mipCount}-1
     * @param frame From 0 to {@link #frameCount}-1
     * <p/>
     * @return
     *         <p/>
     * @throws IOException
     */
    public Image getImage(int level, int frame) throws IOException {
        if(level < 0 || level >= this.mipCount) {
            return null;
        }
        if(frame < 0 || frame >= this.frameCount) {
            return null;
        }
        int thumbLen = (Math.max(thumbWidth, 4) * Math.max(thumbHeight, 4) / 2); // Thumbnail is a minimum of 4*4
        if(thumbWidth == 0 || thumbHeight == 0) {
            thumbLen = 0;
        }
        buf.position(this.headerSize + thumbLen);

        BufferedImage image = null;

        int[] sizesX = new int[this.mipCount]; // smallest -> largest {1, 2, 4, 8, 16, 32, 64, 128}
        int[] sizesY = new int[this.mipCount];
        for(int n = 0; n < this.mipCount; n++) {
            sizesX[this.mipCount - 1 - n] = Math.max(this.width >>> n, 1);
            sizesY[this.mipCount - 1 - n] = Math.max(this.height >>> n, 1);
        }
        for(int i = 0; i < this.mipCount; i++) {
            int w = sizesX[i];
            int h = sizesY[i];

            LOG.log(Level.FINE, "{0}, {1}", new Object[] {w, h});

            int nBytes;

            if(this.format == ImageFormat.IMAGE_FORMAT_DXT1) {
                nBytes = Math.max(w, 4) * Math.max(h, 4) / 2; // Each 'block' is 4*4 pixels + some other data. 16 pixels become 8 bytes [64 bits] (2 * 16 bit colours, 4*4 2 bit indicies)
            } else if(this.format == ImageFormat.IMAGE_FORMAT_DXT5) {
                nBytes = Math.max(w, 4) * Math.max(h, 4); // Each 'block' is 4*4 pixels + some other data. 16 pixels become 16 bytes [128 bits] (2 * 8 bit alpha values, 4x4 3 bit alpha indicies, 2 * 16 bit colours, 4*4 2 bit indicies)
            } else if(this.format == ImageFormat.IMAGE_FORMAT_BGRA8888 || this.format
                                                                              == ImageFormat.IMAGE_FORMAT_RGBA8888) {
                nBytes = w * h * 4; // Each pixel is 4 bytes: rgba
            } else if(this.format == ImageFormat.IMAGE_FORMAT_BGR888) {
                nBytes = w * h * 3; // Each pixel is 3 bytes: rgb
            } else if(this.format == ImageFormat.IMAGE_FORMAT_UV88) {
                nBytes = w * h * 2; // Each pixel is 3 bytes: rgb
            } else {
                LOG.log(Level.WARNING, "Unrecognised VTF format {0}", this.format);
                return null;
            }
            if(i == level) {
                byte[] imageData = new byte[nBytes * this.frameCount];
                try {
                    buf.get(imageData);
                } catch(BufferUnderflowException e) {
                    LOG.log(Level.SEVERE, "Underflow; {0}", nBytes);
                }
                System.arraycopy(imageData, frame * nBytes, imageData, 0, nBytes);
                image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = (Graphics2D) image.getGraphics();
                BufferedImage bi = null;
                if(this.format == ImageFormat.IMAGE_FORMAT_DXT1) {
                    bi = ImageUtils.loadDXT1(imageData, w, h);
                } else if(this.format == ImageFormat.IMAGE_FORMAT_DXT5) {
                    bi = ImageUtils.loadDXT5(imageData, w, h);
                } else if(this.format == ImageFormat.IMAGE_FORMAT_BGRA8888) {
                    bi = ImageUtils.loadBGRA(imageData, w, h);
                } else if(this.format == ImageFormat.IMAGE_FORMAT_RGBA8888) {
                    bi = ImageUtils.loadRGBA(imageData, w, h);
                } else if(this.format == ImageFormat.IMAGE_FORMAT_BGR888) {
                    bi = ImageUtils.loadBGR(imageData, w, h);
                } else if(this.format == ImageFormat.IMAGE_FORMAT_UV88) {
                    bi = ImageUtils.loadUV(imageData, w, h);
                }
                g.drawImage(bi, 0, 0, w, h, null);
            } else {
                buf.get(new byte[nBytes * this.frameCount]);
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

    public Image getThumbImage() throws IOException {
        if(thumbImage == null) {
            buf.position(this.headerSize);
            byte[] thumbData = new byte[Math.max(this.thumbWidth, 4) * Math.max(this.thumbHeight, 4) / 2]; // DXT1. Each 'block' is 4*4 pixels. 16 pixels become 8 bytes
            buf.get(thumbData);
            thumbImage = ImageUtils.loadDXT1(thumbData, this.thumbWidth, this.thumbHeight);
        }
        return thumbImage;
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

    public boolean loadFromStream(InputStream is) throws IOException {
        if(((is.read()) | (is.read() << 8) | (is.read() << 16) | (is.read() << 24)) != HEADER) {
//            LOG.fine("Invalid VTF file");
            return false;
        }

        byte[] array = new byte[4 + is.available()];
        is.read(array, 4, array.length - 4);
        buf = ByteBuffer.wrap(array);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.position(4);

        version = new int[] {buf.getInt(), buf.getInt()};
        headerSize = buf.getInt();
        width = buf.getShort();
        height = buf.getShort();
        flags = buf.getInt();
        EnumSet<CompiledVtfFlags> c = EnumFlags.decode(flags, CompiledVtfFlags.class);
        frameCount = buf.getShort();
        frameFirst = buf.getShort();
        buf.get(new byte[4]);
        reflectivity = new float[] {buf.getFloat(), buf.getFloat(), buf.getFloat()};
        buf.get(new byte[4]);
        bumpScale = buf.getFloat();
        format = ImageFormat.getEnumForIndex(buf.getInt());
        mipCount = buf.get();
        thumbFormat = ImageFormat.getEnumForIndex(buf.getInt());
        thumbWidth = buf.get();
        thumbHeight = buf.get();
        depth = buf.getShort();

        Object[][] debug = {
            {"Width = ", width},
            {"Height = ", height},
            {"Frames = ", frameCount},
            {"Flags = ", c},
            {"Format = ", format},
            {"MipCount = ", mipCount},};

        LOG.fine(StringUtils.fromDoubleArray(debug, "VTF:"));
        return true;
    }

}
