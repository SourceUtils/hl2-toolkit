package com.timepath.hl2.io;

import com.timepath.EnumFlag;
import com.timepath.EnumFlags;
import com.timepath.StringUtils;
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

    private static final Logger LOG = Logger.getLogger(VTF.class.getName());
    
    private static int HEADER = (('V') | ('T' << 8) | ('F' << 16) | ('\0' << 24));

    private static int expectedCrcHead = (('C') | ('R' << 8) | ('C' << 16) | ('\2' << 24));
    
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

    //<editor-fold defaultstate="collapsed" desc="Helpers">
    private static int nextPowerOf2(int n) {
        n |= (n >> 16);
        n |= (n >> 8);
        n |= (n >> 4);
        n |= (n >> 2);
        n |= (n >> 1);
        ++n;
        return n;
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">
    public int[] version;

    public int headerSize;

    public int width;

    public int height;

    public int flags;

    public int frameCount;

    /**
     * Zero indexed
     */
    public int frameFirst;

    public float[] reflectivity;

    public float bumpScale;

    public Format format;

    public int mipCount;

    public Format thumbFormat;

    public int thumbWidth;

    public int thumbHeight;

    public int depth;
    //</editor-fold>

    private Image thumbImage;

    private ByteBuffer buf;

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
        EnumSet<Flags> c = EnumFlags.decode(flags, Flags.class);
        frameCount = buf.getShort();
        frameFirst = buf.getShort();
        buf.get(new byte[4]);
        reflectivity = new float[] {buf.getFloat(), buf.getFloat(), buf.getFloat()};
        buf.get(new byte[4]);
        bumpScale = buf.getFloat();
        format = Format.getEnumForIndex(buf.getInt());
        mipCount = buf.get();
        thumbFormat = Format.getEnumForIndex(buf.getInt());
        thumbWidth = buf.get();
        thumbHeight = buf.get();
        depth = buf.getShort();
        
        Object[][] debug = {
            {"Width = ", width},
            {"Height = ", height},
            {"Frames = ", frameCount},
            {"Flags = ", c},
            {"Format = ", format},
            {"MipCount = ", mipCount},
        };
        
        LOG.info(StringUtils.fromDoubleArray(debug, "VTF:"));
        return true;
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

    public Image getImage(int level) throws IOException {
        return getImage(level, this.frameFirst);
    }

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
            
            LOG.log(Level.INFO, "{0}, {1}", new Object[] {w, h});

            int nBytes;

            if(this.format == Format.IMAGE_FORMAT_DXT1) {
                nBytes = Math.max(w, 4) * Math.max(h, 4) / 2; // Each 'block' is 4*4 pixels + some other data. 16 pixels become 8 bytes [64 bits] (2 * 16 bit colours, 4*4 2 bit indicies)
            } else if(this.format == Format.IMAGE_FORMAT_DXT5) {
                nBytes = Math.max(w, 4) * Math.max(h, 4); // Each 'block' is 4*4 pixels + some other data. 16 pixels become 16 bytes [128 bits] (2 * 8 bit alpha values, 4x4 3 bit alpha indicies, 2 * 16 bit colours, 4*4 2 bit indicies)
            } else if(this.format == Format.IMAGE_FORMAT_BGRA8888) {
                nBytes = w * h * 4; // Each pixel is 4 bytes: rgba
            } else if(this.format == Format.IMAGE_FORMAT_BGR888) {
                nBytes = w * h * 3; // Each pixel is 3 bytes: rgb
            } else if(this.format == Format.IMAGE_FORMAT_UV88) {
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
                if(this.format == Format.IMAGE_FORMAT_DXT1) {
                    bi = ImageUtils.loadDXT1(imageData, w, h);
                } else if(this.format == Format.IMAGE_FORMAT_DXT5) {
                    bi = ImageUtils.loadDXT5(imageData, w, h);
                } else if(this.format == Format.IMAGE_FORMAT_BGRA8888) {
                    bi = ImageUtils.loadBGRA(imageData, w, h);
                } else if(this.format == Format.IMAGE_FORMAT_BGR888) {
                    bi = ImageUtils.loadBGR(imageData, w, h);
                } else if(this.format == Format.IMAGE_FORMAT_UV88) {
                    bi = ImageUtils.loadUV(imageData, w, h);
                }
                g.drawImage(bi, 0, 0, w, h, null);
            } else {
                buf.get(new byte[nBytes * this.frameCount]);
            }
        }
        return image;
    }
    public void getControls() throws IOException {
        buf.position(this.headerSize - 8); // 8 bytes for CRC or other things. I have no idea what the data after the first 64 bytes up until here are for
        int crcHead = buf.getInt();
        int crc = buf.getInt();

        if(crcHead != expectedCrcHead) {
            LOG.log(Level.WARNING, "CRC header {0} is invalid", crcHead);
        } else {
            LOG.log(Level.INFO, "CRC=0x{0}", Integer.toHexString(crc).toUpperCase());
        }
    }

    public Icon getIcon() {
        try {
            return new ImageIcon(this.getThumbImage());
        } catch(IOException ex) {
            LOG.log(Level.WARNING, null, ex);
        }
        return null;
    }

    public static enum Format {

        IMAGE_FORMAT_NONE(-1),
        IMAGE_FORMAT_RGBA8888(0),
        IMAGE_FORMAT_ABGR8888(1),
        IMAGE_FORMAT_RGB888(2),
        IMAGE_FORMAT_BGR888(3),
        IMAGE_FORMAT_RGB565(4),
        /**
         * One value, 8 bits
         */
        IMAGE_FORMAT_I8(5),
        /**
         * One value + alpha, 16 bits
         */
        IMAGE_FORMAT_IA88(6),
        IMAGE_FORMAT_P8(7),
        /**
         * One alpha value, 8 bits. (colour is 255, 255, 255, a)
         */
        IMAGE_FORMAT_A8(8),
        IMAGE_FORMAT_RGB888_BLUESCREEN(9),
        IMAGE_FORMAT_BGR888_BLUESCREEN(10),
        IMAGE_FORMAT_ARGB8888(11),
        IMAGE_FORMAT_BGRA8888(12),
        IMAGE_FORMAT_DXT1(13),
        IMAGE_FORMAT_DXT3(14),
        IMAGE_FORMAT_DXT5(15),
        IMAGE_FORMAT_BGRX8888(16),
        IMAGE_FORMAT_BGR565(17),
        IMAGE_FORMAT_BGRX5551(18),
        IMAGE_FORMAT_BGRA4444(19),
        IMAGE_FORMAT_DXT1_ONEBITALPHA(20),
        IMAGE_FORMAT_BGRA5551(21),
        IMAGE_FORMAT_UV88(22),
        IMAGE_FORMAT_UVWQ8888(23),
        IMAGE_FORMAT_RGBA16161616F(24),
        IMAGE_FORMAT_RGBA16161616(25),
        IMAGE_FORMAT_UVLX8888(26);

        private int index;

        private Format(int index) {
            this.index = index;
        }

        public int getIndex() {
            return index;
        }

        public static Format getEnumForIndex(int index) {
            Format[] values = Format.values();
            for(Format eachValue : values) {
                if(eachValue.getIndex() == index) {
                    return eachValue;
                }
            }
            return null;
        }

    }

    /**
     * https://developer.valvesoftware.com/wiki/Valve_Texture_Format#Image_flags
     */
    public static enum Flags implements EnumFlag {
        // Flags from the *.txt config file

        TEXTUREFLAGS_POINTSAMPLE(0x00000001, "Point Sample"),
        TEXTUREFLAGS_TRILINEAR(0x00000002, "Trilinear"),
        TEXTUREFLAGS_CLAMPS(0x00000004, "Clamp S"),
        TEXTUREFLAGS_CLAMPT(0x00000008, "Clamp T"),
        TEXTUREFLAGS_ANISOTROPIC(0x00000010, "Anisotropic"),
        TEXTUREFLAGS_HINT_DXT5(0x00000020, "Hint DXT5"),
        TEXTUREFLAGS_PWL_CORRECTED(0x00000040, "SRGB"),
        TEXTUREFLAGS_NORMAL(0x00000080, "Normal Map"),
        TEXTUREFLAGS_NOMIP(0x00000100, "No Mipmap"),
        TEXTUREFLAGS_NOLOD(0x00000200, "No Level Of Detail"),
        TEXTUREFLAGS_ALL_MIPS(0x00000400, "No Minimum Mipmap"),
        TEXTUREFLAGS_PROCEDURAL(0x00000800, "Procedural"),
        // These are automatically generated by vtex from the texture data.
        TEXTUREFLAGS_ONEBITALPHA(0x00001000, "One Bit Alpha (Format Specified)"),
        TEXTUREFLAGS_EIGHTBITALPHA(0x00002000, "Eight Bit Alpha (Format Specified)"),
        // Newer flags from the *.txt config file
        TEXTUREFLAGS_ENVMAP(0x00004000, "Environment Map (Format Specified)"),
        TEXTUREFLAGS_RENDERTARGET(0x00008000, "Render Target"),
        TEXTUREFLAGS_DEPTHRENDERTARGET(0x00010000, "Depth Render Target"),
        TEXTUREFLAGS_NODEBUGOVERRIDE(0x00020000, "No Debug Override"),
        TEXTUREFLAGS_SINGLECOPY(0x00040000, "Single Copy"),
        TEXTUREFLAGS_PRE_SRGB(0x00080000),
        TEXTUREFLAGS_UNUSED_00100000(0x00100000),
        TEXTUREFLAGS_UNUSED_00200000(0x00200000),
        TEXTUREFLAGS_UNUSED_00400000(0x00400000),
        TEXTUREFLAGS_NODEPTHBUFFER(0x00800000, "No Depth Buffer"),
        TEXTUREFLAGS_UNUSED_01000000(0x01000000),
        TEXTUREFLAGS_CLAMPU(0x02000000, "Clamp U"),
        TEXTUREFLAGS_VERTEXTEXTURE(0x04000000, "Vertex Texture"),
        TEXTUREFLAGS_SSBUMP(0x08000000, "SSBump"),
        TEXTUREFLAGS_UNUSED_10000000(0x10000000),
        TEXTUREFLAGS_BORDER(0x20000000, "Clamp All"),
        TEXTUREFLAGS_UNUSED_40000000(0x40000000),
        TEXTUREFLAGS_UNUSED_80000000(0x80000000);

        private int mask;

        private String title;

        private Flags(int mask) {
            this(mask, "Unused");
        }
        
        private Flags(int mask, String name) {
            this.mask = mask;
            this.title = name;
        }

        public int getMask() {
            return mask;
        }

        public static Flags getEnumForMask(int mask) {
            Flags[] values = Flags.values();
            for(Flags eachValue : values) {
                if(eachValue.getMask() == mask) {
                    return eachValue;
                }
            }
            return null;
        }

        public int getId() {
            return mask;
        }

        @Override
        public String toString() {
            return title;
        }

    }
    //</editor-fold>

}