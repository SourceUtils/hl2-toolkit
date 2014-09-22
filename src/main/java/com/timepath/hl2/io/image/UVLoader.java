package com.timepath.hl2.io.image;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
class UVLoader {

    private static final Logger LOG = Logger.getLogger(UVLoader.class.getName());

    private UVLoader() {
    }

    public static BufferedImage load(byte[] d, int width, int height, int channels) {
        return loadUV(d, width, height);
    }

    private static BufferedImage loadUV(byte[] b, int width, int height) {
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = (Graphics2D) bi.getGraphics();
        int pos = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                g.setColor(new Color((b[pos] & 0xff) + ((b[pos + 1] & 0xff) << 16) + ((255 & 0xff) << 24)));
                pos += 2;
                g.drawLine(x, y, x, y);
            }
        }
        return bi;
    }
}
