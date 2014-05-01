package com.timepath.hl2.io.image;

import com.timepath.hl2.io.image.ImageFormat;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

/**
 *
 * @author TimePath
 */
public class DXTLoader {

    private static final Logger LOG = Logger.getLogger(DXTLoader.class.getName());

    private static final int alpha_mask_555 = 0x1;

    private static final int blue_mask_555 = 0x1F;

    private static final int blue_mask_565 = 0x1F;

    private static final int green_mask_555 = 0x3E0;

    private static final int green_mask_565 = 0x7E0;

    private static final int red_mask_555 = 0x7C00;

    private static final int red_mask_565 = 0xF800;

    public static BufferedImage load(ImageFormat f, byte[] b, int width, int height) {
        switch(f) {
            case IMAGE_FORMAT_DXT1:
            case IMAGE_FORMAT_DXT1_ONEBITALPHA:
                return loadDXT1(b, width, height);
            case IMAGE_FORMAT_DXT3:
                return loadDXT3(b, width, height);
            case IMAGE_FORMAT_DXT5:
                return loadDXT5(b, width, height);
        }
        return null;
    }

    /**
     * 8 bytes per 4*4
     */
    public static BufferedImage loadDXT1(byte[] b, int width, int height) {
        BufferedImage bi = new BufferedImage(Math.max(width, 4), Math.max(height, 4),
                                             BufferedImage.TYPE_INT_ARGB);
        int pos = 0;

        for(int y = 0; y < height; y += 4) {
            for(int x = 0; x < width; x += 4) {
                int color_0 = ((b[pos++] & 0xFF) + ((b[pos++] & 0xFF) << 8)) & 0xFFFF; // 2 bytes
                int color_1 = ((b[pos++] & 0xFF) + ((b[pos++] & 0xFF) << 8)) & 0xFFFF; // 2 bytes
                Color[] colour = new Color[4];
                colour[0] = extract565(color_0);
                colour[1] = extract565(color_1);

                if(color_0 > color_1) {
                    colour[2] = new Color(
                        Math.round(((2 * colour[0].getRed()) + colour[1].getRed()) / 3),
                        Math.round(((2 * colour[0].getGreen()) + colour[1].getGreen()) / 3),
                        Math.round(((2 * colour[0].getBlue()) + colour[1].getBlue()) / 3));
                    colour[3] = new Color(
                        Math.round(((2 * colour[1].getRed()) + colour[0].getRed()) / 3),
                        Math.round(((2 * colour[1].getGreen()) + colour[0].getGreen()) / 3),
                        Math.round(((2 * colour[1].getBlue()) + colour[0].getBlue()) / 3));
                } else {
                    colour[2] = new Color(
                        Math.round((colour[0].getRed() + colour[1].getRed()) / 2),
                        Math.round((colour[0].getGreen() + colour[1].getGreen()) / 2),
                        Math.round((colour[0].getBlue() + colour[1].getBlue()) / 2));
                    colour[3] = new Color(0, 0, 0, 0);
                }

                for(int y1 = 0; y1 < 4; y1++) { // 16 bits / 4 rows = 4 bits/line = 1 byte/row
                    byte rowData = b[pos++];
                    int[] rowBits = {(rowData & 0xC0) >>> 6, (rowData & 0x30) >>> 4,
                                     (rowData & 0xC) >>> 2, rowData & 0x3};

                    for(int x1 = 0; x1 < 4; x1++) { // column scan
                        Color col = new Color(colour[rowBits[3 - x1]].getRed(),
                                              colour[rowBits[3 - x1]].getGreen(),
                                              colour[rowBits[3 - x1]].getBlue(),
                                              colour[rowBits[3 - x1]].getAlpha());
                        bi.setRGB(x + x1, y + y1, col.getRGB());
                    }
                }
            }
        }
        return bi;
    }

    /**
     *
     * 8 bytes for alpha channel, additional 8 per 4*4 chunk
     *
     * TODO: fully implement correct colors
     */
    public static BufferedImage loadDXT3(byte[] b, int width, int height) {
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
                    int[] bits = new int[] {(next4[y1] & bits_12) >> 6, (next4[y1] & bits_34) >> 4,
                                            (next4[y1] & bits_56) >> 2, next4[y1] & bits_78};

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

    /**
     * 8 bytes for alpha channel, additional 8 per 4*4 chunk
     */
    public static BufferedImage loadDXT5(byte[] b, int width, int height) {
        BufferedImage bi = new BufferedImage(Math.max(width, 4), Math.max(height, 4),
                                             BufferedImage.TYPE_INT_ARGB);
        int pos = 0;

        for(int y = 0; y < height; y += 4) {
            for(int x = 0; x < width; x += 4) {

                //<editor-fold defaultstate="collapsed" desc="Alpha">
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
                int[] alphaByte = {b[pos++] & 0xFF, b[pos++] & 0xFF, b[pos++] & 0xFF,
                                   b[pos++] & 0xFF, b[pos++] & 0xFF, b[pos++] & 0xFF};
                int sel1 = ((alphaByte[2] << 16) | (alphaByte[1] << 8) | alphaByte[0]) & 0xFFFFFF;
                int sel2 = ((alphaByte[5] << 16) | (alphaByte[4] << 8) | alphaByte[3]) & 0xFFFFFF;
                for(int yi = 0; yi < 2; yi++) {
                    for(int xi = 0; xi < 4; xi++) {
                        alphas[yi][xi] = a[sel1 & 0x7];
                        sel1 >>>= 3;
                    }
                }
                for(int yi = 2; yi < 4; yi++) {
                    for(int xi = 0; xi < 4; xi++) {
                        alphas[yi][xi] = a[sel2 & 0x7];
                        sel2 >>>= 3;
                    }
                }
                //</editor-fold>

                //<editor-fold defaultstate="collapsed" desc="DXT1 color info">
                int color_0 = ((b[pos++] & 0xFF) + ((b[pos++] & 0xFF) << 8)) & 0xFFFF; // 2 bytes
                int color_1 = ((b[pos++] & 0xFF) + ((b[pos++] & 0xFF) << 8)) & 0xFFFF; // 2 bytes
                Color[] colour = new Color[4];
                colour[0] = extract565(color_0);
                colour[1] = extract565(color_1);

                if(color_0 > color_1) {
                    colour[2] = new Color(
                        Math.round(((2 * colour[0].getRed()) + colour[1].getRed()) / 3),
                        Math.round(((2 * colour[0].getGreen()) + colour[1].getGreen()) / 3),
                        Math.round(((2 * colour[0].getBlue()) + colour[1].getBlue()) / 3));
                    colour[3] = new Color(
                        Math.round(((2 * colour[1].getRed()) + colour[0].getRed()) / 3),
                        Math.round(((2 * colour[1].getGreen()) + colour[0].getGreen()) / 3),
                        Math.round(((2 * colour[1].getBlue()) + colour[0].getBlue()) / 3));
                } else {
                    colour[2] = new Color(
                        Math.round((colour[0].getRed() + colour[1].getRed()) / 2),
                        Math.round((colour[0].getGreen() + colour[1].getGreen()) / 2),
                        Math.round((colour[0].getBlue() + colour[1].getBlue()) / 2));
                    colour[3] = new Color(0, 0, 0);
                }

                for(int y1 = 0; y1 < 4; y1++) { // 16 bits / 4 rows = 4 bits/line = 1 byte/row
                    byte rowData = b[pos++];
                    int[] rowBits = {(rowData & 0xC0) >>> 6, (rowData & 0x30) >>> 4,
                                     (rowData & 0xC) >>> 2, rowData & 0x3};

                    for(int x1 = 0; x1 < 4; x1++) { // column scan
                        Color col = new Color(colour[rowBits[3 - x1]].getRed(),
                                              colour[rowBits[3 - x1]].getGreen(),
                                              colour[rowBits[3 - x1]].getBlue(),
                                              alphas[y1][x1]);
                        bi.setRGB((x + x1), (y + y1), col.getRGB());
                    }
                }
                //</editor-fold>

            }
        }

        return bi;
    }

    private static Color createColor(float r, float g, float b, float a) {
        return new Color(Math.round(r), Math.round(g), Math.round(b), Math.round(a));
    }

    private static Color extract555(int c) {
        return createColor((((c & red_mask_555) >>> 10) << 3), (((c & green_mask_555) >>> 5) << 3),
                           ((c & blue_mask_555) << 3), ((c & alpha_mask_555) << 7));
    }

    private static Color extract565(int c) {
        return createColor((((c & red_mask_565) >>> 11) << 3), (((c & green_mask_565) >>> 5) << 2),
                           ((c & blue_mask_565) << 3), 255);
    }

    private DXTLoader() {
    }

}
