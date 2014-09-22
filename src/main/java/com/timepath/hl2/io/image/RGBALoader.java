package com.timepath.hl2.io.image;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
class RGBALoader {

    private static final Logger LOG = Logger.getLogger(RGBALoader.class.getName());

    private RGBALoader() {
    }

    public static BufferedImage load(byte[] d, int width, int height, byte[] order, byte[] len) {
        int bpp = 0;
        for (int l : len) {
            if ((l % 8) != 0) {
                throw new UnsupportedOperationException("Not supported yet.");
            }
            bpp += l;
        }
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        WritableRaster raster = bi.getRaster();
        int pos = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                raster.setSample(x, y, 3, 255);
                for (byte b : order) {
                    raster.setSample(x, y, b, d[pos]);
                    pos += bpp / 8 / order.length;
                }
            }
        }
        return bi;
    }
}
