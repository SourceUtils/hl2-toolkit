package com.timepath.hl2.io;

import com.timepath.steam.io.storage.GCF;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import com.timepath.steam.SteamUtils;
import com.timepath.io.utils.ViewableData;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * TODO: .360.vtf files seem to be a slightly different format... and LZMA compressed.
 *
 * @author timepath
 */
public class VTF implements ViewableData {

    private static final Logger LOG = Logger.getLogger(VTF.class.getName());

    public VTF() {
    }

    //<editor-fold defaultstate="collapsed" desc="Properties">
    /**
     * 8 bytes
     */
    public int[] version;
    
    /**
     * 2 bytes
     * The size of the VTF header. Image data comes after this
     */
    public int headerSize;
    
    /**
     * 2 bytes
     */
    public int width;
    
    /**
     * 2 bytes
     */
    public int height;
    
    /**
     * 4 bytes
     */
    public int flags;
    
    /**
     * 2 bytes
     */
    public int frameCount;
    
    /**
     * 2 bytes
     * Zero indexed
     */
    public int frameFirst;
    
    /**
     * 12 bytes
     */
    public float[] reflectivity;
    
    /**
     * 4 bytes
     */
    public float bumpScale;
    
    /**
     * 4 bytes
     */
    public Format format;
    
    /**
     * 1 byte
     */
    public int mipCount;
    
    /**
     * 4 bytes
     */
    public Format thumbFormat;
    
    /**
     * 1 byte
     */
    public int thumbWidth;
    
    /**
     * 1 byte
     */
    public int thumbHeight;
    
    /**
     * 1 byte
     * The documentation says 2, but I don't think so...
     */
    public int depth;
    //</editor-fold>

    private File file;
    
    private static int expectedCrcHead = (('C') | ('R' << 8) | ('C' << 16) | ('\2' << 24));

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

    private Image thumbImage;

    public Image getThumbImage() throws IOException {
        if(thumbImage == null) {
            buf.position(this.headerSize);
            byte[] thumbData = new byte[Math.max(this.thumbWidth, 4) * Math.max(this.thumbHeight, 4) / 2]; // DXT1. Each 'block' is 4*4 pixels. 16 pixels become 8 bytes
            buf.get(thumbData);
            thumbImage = loadDXT1(thumbData, this.thumbWidth, this.thumbHeight);
        }
        return thumbImage;
    }

    /**
     *
     * @param level 0 is full size
     *
     * @return
     *
     * @throws IOException
     */
    public Image getImage(int level) throws IOException {
        if(level >= this.mipCount) {
            return null;
        }
        buf.position(this.headerSize + (Math.max(thumbWidth, 4) * Math.max(thumbHeight, 4) / 2));

        BufferedImage image = null;

        int[] sizesX = new int[this.mipCount]; // smallest -> largest {1, 2, 4, 8, 16, 32, 64, 128}
        int[] sizesY = new int[this.mipCount];
        for(int n = 0; n < this.mipCount; n++) {
            sizesX[n] = Math.max((this.width >>> (this.mipCount - n - 1)), 1);
            sizesY[n] = Math.max((this.height >>> (this.mipCount - n - 1)), 1);
//                System.out.println("sizesX["+n+"] = " + sizesX[n] + "\n" + "sizesY["+n+"] = " + sizesY[n]);
        }
        for(int i = 0; i < this.mipCount; i++) {
            int w = sizesX[i];
            int h = sizesY[i];

            int nBytes;

            if(this.format == Format.IMAGE_FORMAT_DXT1) {
                nBytes = Math.max(w, 4) * Math.max(h, 4) / 2; // DXT1. Each 'block' is 4*4 pixels + some other data. 16 pixels become 8 bytes [64 bits] (2 * 16 bit colours, 4*4 2 bit indicies)
            } else if(this.format == Format.IMAGE_FORMAT_DXT5) {
                nBytes = Math.max(w, 4) * Math.max(h, 4); // DXT5. Each 'block' is 4*4 pixels + some other data. 16 pixels become 16 bytes [128 bits] (2 * 8 bit alpha values, 4x4 3 bit alpha indicies, 2 * 16 bit colours, 4*4 2 bit indicies)
            } else if(this.format == Format.IMAGE_FORMAT_BGRA8888) {
                nBytes = w * h * 4; // BGRA8888. Each pixel is 4 bytes -  r g b a
            } else if(this.format == Format.IMAGE_FORMAT_BGR888) {
                nBytes = w * h * 3; // BGR888. Each pixel is 3 bytes -  r g b
            } else if(this.format == Format.IMAGE_FORMAT_UV88) {
                nBytes = w * h * 2; // BGR888. Each pixel is 3 bytes -  r g b
            } else {
                LOG.log(Level.WARNING, "Unrecognised VTF format {0}", this.format);
                return null;
            }

            if(this.mipCount - 1 - i == level && nBytes > 0) { // biggest
                byte[] imageData = new byte[nBytes];
                buf.get(imageData);
                image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g = (Graphics2D) image.getGraphics();
                if(this.format == Format.IMAGE_FORMAT_DXT1) {
                    g.drawImage(loadDXT1(imageData, w, h), 0, 0, w, h, null);
                } else if(this.format == Format.IMAGE_FORMAT_DXT5) {
                    g.drawImage(loadDXT5(imageData, w, h), 0, 0, w, h, null);
                } else if(this.format == Format.IMAGE_FORMAT_BGRA8888) {
                    g.drawImage(loadBGRA(imageData, w, h), 0, 0, w, h, null);
                } else if(this.format == Format.IMAGE_FORMAT_BGR888) {
                    g.drawImage(loadBGR(imageData, w, h), 0, 0, w, h, null);
                } else if(this.format == Format.IMAGE_FORMAT_UV88) {
                    g.drawImage(loadUV(imageData, w, h), 0, 0, w, h, null);
                }
            } else {
//                rf.skipBytes(nBytes);
                buf.get(new byte[nBytes]);
            }
        }
        return image;
    }

    // STATIC METHODS
    public static GCF mats = null;

    private static int expectedHeader = (('V') | ('T' << 8) | ('F' << 16) | ('\0' << 24));

    private InputStream stream;
    private ByteBuffer buf;

    public static VTF load(InputStream is) throws IOException {
        if(((is.read()) | (is.read() << 8) | (is.read() << 16) | (is.read() << 24)) != expectedHeader) {
//            LOG.fine("Invalid VTF file");
//            cache.put(file, null);
            return null;
        }
        
        VTF vtf = new VTF();
        vtf.stream = is;
        byte[] array = new byte[4 + is.available()/*65*/];
        is.read(array, 4, array.length - 4);
        vtf.buf = ByteBuffer.wrap(array);
        vtf.buf.order(ByteOrder.LITTLE_ENDIAN);
        vtf.buf.position(4);
        
        vtf.version = new int[]{vtf.buf.getInt(), vtf.buf.getInt()};
        vtf.headerSize = vtf.buf.getInt();
        vtf.width = vtf.buf.getShort();
        vtf.height = vtf.buf.getShort();
        vtf.flags = vtf.buf.getInt();
        vtf.frameCount = vtf.buf.getShort();
        vtf.frameFirst = vtf.buf.getShort();
        vtf.buf.get(new byte[4]);
        vtf.reflectivity = new float[]{vtf.buf.getFloat(), vtf.buf.getFloat(), vtf.buf.getFloat()};
        vtf.buf.get(new byte[4]);
        vtf.bumpScale = vtf.buf.getFloat();
        vtf.format = Format.getEnumForIndex(vtf.buf.getInt());
        vtf.mipCount = vtf.buf.get();
        vtf.thumbFormat = Format.getEnumForIndex(vtf.buf.getInt());
        vtf.thumbWidth = vtf.buf.get();
        vtf.thumbHeight = vtf.buf.get();
        vtf.depth = vtf.buf.getShort();

        LOG.log(Level.FINE, "Format: {0}", vtf.format);

        if(vtf.frameCount > 1) {
            LOG.log(Level.WARNING, "FRAMES = {0}", vtf.frameCount); // zero indexed
            if(vtf.frameFirst != 0) {
                LOG.log(Level.WARNING, "FIRSTFRAME = {0}", vtf.frameFirst); // zero indexed
            }
        }
        return vtf;
    }

    public static VTF load(String path) throws IOException {
        path = new File(path).getAbsolutePath();
        if(mats == null) {
            try {
                mats = new GCF(new File(SteamUtils.getSteamApps(), "Team Fortress 2 Materials.gcf"));
            } catch(IOException ex) {
                Logger.getLogger(VTF.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        File f = null;
        try {
            File dest = File.createTempFile(path.replaceAll("/", "_"), "");
            LOG.log(Level.INFO, "Extracting {0} to {1}", new Object[]{path, dest});
            if(mats.extract(mats.find(path).get(0), dest) != null) {
                f = new File(path);
                LOG.log(Level.INFO, "Loading {0}", f);
            }
        } catch(IOException ex) {
            Logger.getLogger(VTF.class.getName()).log(Level.SEVERE, null, ex);
        }
        return VTF.load(new FileInputStream(f));
    }

    private static HashMap<InputStream, VTF> cache = new HashMap<InputStream, VTF>();

    /**
     * 8 bytes per 4*4
     */
    private static BufferedImage loadDXT1(byte[] b, int width, int height) {
        BufferedImage bi = new BufferedImage(Math.max(width, 4), Math.max(height, 4), BufferedImage.TYPE_INT_ARGB);
        int pos = 0;

        for(int y = 0; y < height; y += 4) {
            for(int x = 0; x < width; x += 4) {
                int color_0 = ((b[pos++] & 0xFF) + ((b[pos++] & 0xFF) << 8)) & 0xFFFF; // 2 bytes
                int color_1 = ((b[pos++] & 0xFF) + ((b[pos++] & 0xFF) << 8)) & 0xFFFF; // 2 bytes
                Color[] colour = new Color[4];

                if(color_0 > color_1) {
                    colour[0] = extract565(color_0);
                    colour[1] = extract565(color_1);
                    colour[2] = new Color(Math.round(((2 * colour[0].getRed()) + colour[1].getRed()) / 3), Math.round(((2 * colour[0].getGreen()) + colour[1].getGreen()) / 3), Math.round(((2 * colour[0].getBlue()) + colour[1].getBlue()) / 3));
                    colour[3] = new Color(Math.round(((2 * colour[1].getRed()) + colour[0].getRed()) / 3), Math.round(((2 * colour[1].getGreen()) + colour[0].getGreen()) / 3), Math.round(((2 * colour[1].getBlue()) + colour[0].getBlue()) / 3));
                } else {
                    colour[0] = extract565(color_0);
                    colour[1] = extract565(color_1);
                    colour[2] = new Color(Math.round((colour[0].getRed() + colour[1].getRed()) / 2), Math.round((colour[0].getGreen() + colour[1].getGreen()) / 2), Math.round((colour[0].getBlue() + colour[1].getBlue()) / 2));
                    colour[3] = new Color(0, 0, 0, 0);
                }

                // remaining 4 bytes
//                int sel = b[pos++];
//        	    sel |= b[pos++] << 8;
//        	    sel |= b[pos++] << 16;
//        	    sel |= b[pos++] << 24;
                for(int y1 = 0; y1 < 4/*
                         * - (height % 4)
                         */; y1++) { // 16 bits / 4 rows = 4 bits/line = 1 byte/row
                    byte rowData = b[pos++];
                    int[] rowBits = {(rowData & 0xC0) >>> 6, (rowData & 0x30) >>> 4, (rowData & 0xC) >>> 2, rowData & 0x3};

                    for(int x1 = 0; x1 < 4/*
                             * - (width % 4)
                             */; x1++) { // column scan
                        bi.setRGB((x) + x1, (y) + y1, colour[rowBits[3 - x1]].getRGB()); // c is taken from 3 to ensure everything is drawn the correct way around
//                        bi.setRGB((x+x1), (y+y1), colour[sel & 3].getRGB());
//                        sel >>= 2;
                    }
                }
            }
        }
        return bi;
    }

    //<editor-fold defaultstate="collapsed" desc="Currently unimplemented">
    /**
     *
     * 8 bytes for alpha channel, additional 8 per 4*4 chunk
     *
     * TODO: fully implement correct colours
     */
    BufferedImage loadDXT3(byte[] b, int width, int height) {
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) bi.getGraphics();
        int pos = 0;

        int bits_12 = 0xC0; // first 2 bits
        int bits_34 = 0x30; // next 2 bits
        int bits_56 = 0xC; // next 2 bits
        int bits_78 = 0x3; // last 2 bits
        //        RGB 565: WORD pixel565 = (red_value << 11) | (green_value << 5) | blue_value;

        int xBlocks = (width / 4);
        if(xBlocks < 1) {
            xBlocks = 1;
        }
        int yBlocks = (height / 4);
        if(yBlocks < 1) {
            yBlocks = 1;
        }

        //        System.err.println("SIZE="+xBlocks+", "+yBlocks+" = " + b.length);
        for(int y = 0; y < yBlocks; y++) {
            for(int x = 0; x < xBlocks; x++) {
                pos += 8; // 64 bits of alpha channel data (two 8 bit alpha values and a 4x4 3 bit lookup table)
                int color_0 = (b[pos] & 0xff) + ((b[pos + 1] & 0xff) << 8); // 2 bytes
                pos += 2;
                int color_1 = (b[pos] & 0xff) + ((b[pos + 1] & 0xff) << 8); // 2 bytes
                pos += 2;

                int red1, green1, blue1, red2, green2, blue2;
                Color c1, c2;

                red1 = ((color_0 & red_mask_565) >> 11) << 3;
                green1 = ((color_0 & green_mask_565) >> 5) << 2;
                blue1 = ((color_0 & blue_mask_565) << 3);
                c1 = new Color(red1, green1, blue1);

                red2 = ((color_1 & red_mask_565) >> 11) << 3;
                green2 = ((color_1 & green_mask_565) >> 5) << 2;
                blue2 = (color_1 & blue_mask_565) << 3;
                c2 = new Color(red2, green2, blue2);

                // remaining 4 bytes
                byte[] next4 = {b[pos], b[pos + 1], b[pos + 2], b[pos + 3]};
                pos += 4;
                for(int y1 = 0; y1 < 4; y1++) { // 16 bits / 4 lines = 4 bits/line = 1 byte/line
                    int[] bits = new int[]{(next4[y1] & bits_12) >> 6, (next4[y1] & bits_34) >> 4, (next4[y1] & bits_56) >> 2, next4[y1] & bits_78};

                    for(int i = 0; i < 4; i++) { // horizontal scan
                        int bit = bits[i];
                        if(bit == 0) {
                            g.setColor(c1);
                        } else if(bit == 1) {
                            g.setColor(c2);
                        } else if(bit == 2) {
                            int cred = (2 * (c1.getRed() / 3)) + (c2.getRed() / 3);
                            int cgrn = (2 * (c1.getGreen() / 3)) + (c2.getGreen() / 3);
                            int cblu = (2 * (c1.getBlue() / 3)) + (c2.getBlue() / 3);
                            Color c = new Color(cred, cgrn, cblu);
                            g.setColor(c);
                        } else if(bit == 3) {
                            int cred = (c1.getRed() / 3) + (2 * (c2.getRed() / 3));
                            int cgrn = (c1.getGreen() / 3) + (2 * (c2.getGreen() / 3));
                            int cblu = (c1.getBlue() / 3) + (2 * (c2.getBlue() / 3));
                            Color c = new Color(cred, cgrn, cblu);
                            g.setColor(c);
                        }
                        g.drawLine((x * 4) + 4 - i, (y * 4) + y1,
                                   (x * 4) + 4 - i, (y * 4) + y1);
                    }
                }
            }
        }
        return bi;
    }
    //</editor-fold>

    /**
     *
     * 8 bytes for alpha channel, additional 8 per 4*4 chunk
     *
     * TODO: fully implement correct colours
     */
    private static BufferedImage loadDXT5(byte[] b, int width, int height) {
        boolean alphaEnabled = true;
        BufferedImage bi = new BufferedImage(Math.max(width, 4), Math.max(height, 4), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) bi.getGraphics();
        g.setComposite(AlphaComposite.Src);
        int pos = 0;

        for(int y = 0; y < height; y += 4) {
            for(int x = 0; x < width; x += 4) {

                int[] a = new int[8];
                a[0] = (b[pos++] & 0xFF); // 64 bits of alpha channel data (two 8 bit alpha values and a 4x4 3 bit lookup table)
                a[1] = (b[pos++] & 0xFF);
                if(a[0] > a[1]) {
                    a[2] = Math.round((6 * a[0] + 1 * a[1]) / 7);
                    a[3] = Math.round((5 * a[0] + 2 * a[1]) / 7);
                    a[4] = Math.round((4 * a[0] + 3 * a[1]) / 7);
                    a[5] = Math.round((3 * a[0] + 4 * a[1]) / 7);
                    a[6] = Math.round((2 * a[0] + 5 * a[1]) / 7);
                    a[7] = Math.round((1 * a[0] + 6 * a[1]) / 7);
                } else {
                    a[2] = Math.round((4 * a[0] + 1 * a[1]) / 5);
                    a[3] = Math.round((3 * a[0] + 2 * a[1]) / 5);
                    a[4] = Math.round((2 * a[0] + 3 * a[1]) / 5);
                    a[5] = Math.round((1 * a[0] + 4 * a[1]) / 5);
                    a[6] = 0;
                    a[7] = 255;
                }

                int[][] alphas = new int[4][4];
                if(alphaEnabled) {
                    int[] alphaByte = {b[pos++] & 0xFF, b[pos++] & 0xFF, b[pos++] & 0xFF, b[pos++] & 0xFF, b[pos++] & 0xFF, b[pos++] & 0xFF};
                    int sel1 = (((alphaByte[2] << 16) & 0xFF0000) | ((alphaByte[1] << 8) & 0xFF00) | alphaByte[0]) & 0xFFFFFF;
                    int sel2 = (((alphaByte[5] << 16) & 0xFF0000) | ((alphaByte[4] << 8) & 0xFF00) | alphaByte[3]) & 0xFFFFFF;
                    for(int yi = 0; yi < 2; yi++) {
                        for(int xi = 0; xi < 4; xi++) {
                            alphas[yi][xi] = a[((sel1) & 0x7)];
                            sel1 >>= 3;
                        }
                    }
                    for(int yi = 0; yi < 2; yi++) {
                        for(int xi = 0; xi < 4; xi++) {
                            alphas[2 + yi][xi] = a[((sel2) & 0x7)];
                            sel2 >>= 3;
                        }
                    }
                } else {
                    pos += 8;
                }

                // Copy-paste from DXT1

                int c0 = (b[pos++] & 0xff);
                c0 |= ((b[pos++] & 0xff) << 8); // 2 bytes
                int c1 = (b[pos++] & 0xff);
                c1 |= ((b[pos++] & 0xff) << 8); // 2 bytes
                Color[] colour = new Color[4];
                colour[0] = extract565(c0);
                colour[1] = extract565(c1);
                colour[2] = new Color(Math.round(((2 * colour[0].getRed()) + colour[1].getRed()) / 3), Math.round(((2 * colour[0].getGreen()) + colour[1].getGreen()) / 3), Math.round(((2 * colour[0].getBlue()) + colour[1].getBlue()) / 3));
                colour[3] = new Color(Math.round(((2 * colour[1].getRed()) + colour[0].getRed()) / 3), Math.round(((2 * colour[1].getGreen()) + colour[0].getGreen()) / 3), Math.round(((2 * colour[1].getBlue()) + colour[0].getBlue()) / 3));

                // remaining 4 bytes
                if(width >= 4 && height >= 4) {
                    for(int y1 = 0; y1 < 4; y1++) { // 16 bits / 4 rows = 4 bits/line = 1 byte/row
                        int rowData = b[pos++] & 0xff;
                        int[] rowBits = {(rowData & 0xC0) >>> 6, (rowData & 0x30) >>> 4, (rowData & 0xC) >>> 2, rowData & 0x3};

                        for(int x1 = 0; x1 < 4; x1++) { // column scan
//                            if(alphas[y1][x1] > 0)
//                                System.out.println(alphas[y1][x1]);
                            Color col = new Color(colour[rowBits[3 - x1]].getRed(), colour[rowBits[3 - x1]].getGreen(), colour[rowBits[3 - x1]].getBlue(), alphas[y1][x1]); // c is taken from 3 to ensure everything is drawn the correct way around
                            bi.setRGB((x + x1), (y + y1), col.getRGB());
//                            System.out.println("(" + y + "," + x + ") = " + (y1) + ":" + (x1) + " = (" + (y+y1) + "," + (x+x1) + ")");
                        }
//                        System.out.println();
                    }
                } else {
                    pos += 4;
                }
            }
        }
        return bi;
    }

    private static BufferedImage loadUV(byte[] b, int width, int height) {
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) bi.getGraphics();
        int pos = 0;
        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                g.setColor(new Color((b[pos] & 0xff) + ((b[pos + 1] & 0xff) << 16) + ((255 & 0xff) << 24)));
                pos += 2;
                g.drawLine(x, y, x, y);
            }
        }
        return bi;
    }

    private static BufferedImage loadBGR(byte[] b, int width, int height) {
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) bi.getGraphics();
        int pos = 0;
        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                g.setColor(new Color((b[pos] & 0xff) + ((b[pos + 1] & 0xff) << 8) + ((b[pos + 2] & 0xff) << 16) + ((255 & 0xff) << 24)));
                pos += 3;
                g.drawLine(x, y, x, y);
            }
        }
        return bi;
    }

    private static BufferedImage loadBGRA(byte[] b, int width, int height) {
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) bi.getGraphics();
        g.setComposite(AlphaComposite.Src);
        int pos = 0;
        for(int y = 0; y < height; y++) {
            for(int x = 0; x < width; x++) {
                g.setColor(new Color((b[pos + 2] & 0xff), (b[pos + 1] & 0xff), (b[pos] & 0xff), (b[pos + 3] & 0xff)));
                pos += 4;
                g.drawLine(x, y, x, y);
            }
        }
        return bi;
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

    //<editor-fold defaultstate="collapsed" desc="Colour helpers">
    private static Color createColor(float r, float g, float b, float a) {
        return new Color(Math.round(r), Math.round(g), Math.round(b), Math.round(a));
    }

    /**
     * First 5 bits
     */
    private static final int red_mask_565 = 0xF800;

    /**
     * Next 6 bits
     */
    private static final int green_mask_565 = 0x7E0;

    /**
     * Last 5 bits
     */
    private static final int blue_mask_565 = 0x1F;

    private static Color extract565(int c) {
        return createColor((float) (((c & red_mask_565) >>> 11) << 3), (float) (((c & green_mask_565) >>> 5) << 2), (float) ((c & blue_mask_565) << 3), 255);
    }

    private static final int red_mask_555 = 0x7C00; // first 5 bits

    private static final int green_mask_555 = 0x3E0; // next 5 bits

    private static final int blue_mask_555 = 0x1F;

    private static final int alpha_mask_555 = 0x1;

    private static Color extract555(int c) {
        return createColor((float) (((c & red_mask_555) >>> 10) << 3), (float) (((c & green_mask_555) >>> 5) << 3), (float) ((c & blue_mask_555) << 3), (float) ((c & alpha_mask_555) << 7));
    }
    //</editor-fold>

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
    public static enum Flags {
        // Flags from the *.txt config file

        TEXTUREFLAGS_POINTSAMPLE(0x00000001),
        TEXTUREFLAGS_TRILINEAR(0x00000002),
        TEXTUREFLAGS_CLAMPS(0x00000004),
        TEXTUREFLAGS_CLAMPT(0x00000008),
        TEXTUREFLAGS_ANISOTROPIC(0x00000010),
        TEXTUREFLAGS_HINT_DXT5(0x00000020),
        TEXTUREFLAGS_PWL_CORRECTED(0x00000040),
        TEXTUREFLAGS_NORMAL(0x00000080),
        TEXTUREFLAGS_NOMIP(0x00000100),
        TEXTUREFLAGS_NOLOD(0x00000200),
        TEXTUREFLAGS_ALL_MIPS(0x00000400),
        TEXTUREFLAGS_PROCEDURAL(0x00000800),
        // These are automatically generated by vtex from the texture data.
        TEXTUREFLAGS_ONEBITALPHA(0x00001000),
        TEXTUREFLAGS_EIGHTBITALPHA(0x00002000),
        // Newer flags from the *.txt config file
        TEXTUREFLAGS_ENVMAP(0x00004000),
        TEXTUREFLAGS_RENDERTARGET(0x00008000),
        TEXTUREFLAGS_DEPTHRENDERTARGET(0x00010000),
        TEXTUREFLAGS_NODEBUGOVERRIDE(0x00020000),
        TEXTUREFLAGS_SINGLECOPY(0x00040000),
        TEXTUREFLAGS_PRE_SRGB(0x00080000),
        TEXTUREFLAGS_UNUSED_00100000(0x00100000),
        TEXTUREFLAGS_UNUSED_00200000(0x00200000),
        TEXTUREFLAGS_UNUSED_00400000(0x00400000),
        TEXTUREFLAGS_NODEPTHBUFFER(0x00800000),
        TEXTUREFLAGS_UNUSED_01000000(0x01000000),
        TEXTUREFLAGS_CLAMPU(0x02000000),
        TEXTUREFLAGS_VERTEXTEXTURE(0x04000000),
        TEXTUREFLAGS_SSBUMP(0x08000000),
        TEXTUREFLAGS_UNUSED_10000000(0x10000000),
        TEXTUREFLAGS_BORDER(0x20000000),
        TEXTUREFLAGS_UNUSED_40000000(0x40000000),
        TEXTUREFLAGS_UNUSED_80000000(0x80000000);

        private int mask;

        private Flags(int mask) {
            this.mask = mask;
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
    }
    //</editor-fold>

    @Override
    public String toString() {
        return file.getName();
    }

    public Icon getIcon() {
        Icon i;
        try {
            i = new ImageIcon(this.getThumbImage());
        } catch(IOException ex) {
            LOG.log(Level.WARNING, null, ex);
        }
        return null;
    }
}