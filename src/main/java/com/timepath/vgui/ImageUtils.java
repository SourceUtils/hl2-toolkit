package com.timepath.vgui;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

/**
 * @author TimePath
 */
public class ImageUtils {
    /**
     * Scales an image to be the desired height, cutting off the sides.
     */
    public static BufferedImage resizeImage(Image image, int w, int h) {
        int type = BufferedImage.TYPE_INT_ARGB;
        BufferedImage scaled = new BufferedImage(w, h, type);
        // Keep aspect ratio
        int proposedWidth = (int) Math.round((double) ((float) (h / image.getHeight(null))) * image.getWidth(null));
        // Half the width difference is excess
        int excess = Math.abs(proposedWidth - w) / 2;
        // Create graphics context with nice settings
        Graphics2D g = scaled.createGraphics();
        g.setComposite(AlphaComposite.SrcOver);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Draw with excess off screen
        g.drawImage(image, -excess, 0, w + (2 * excess), h, null);
        g.dispose();
        return ((BufferedImage) (scaled));
    }

    public static BufferedImage toCompatibleImage(BufferedImage image) {
        // Obtain the current system graphical settings
        GraphicsConfiguration configuration = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration();

        // If image is already compatible and optimized for current system settings, simply return it
        if (image.getColorModel().equals(configuration.getColorModel()))
            return image;
        // Image is not optimized, create a new image that is
        BufferedImage copy = configuration.createCompatibleImage(image.getWidth(), image.getHeight(), image.getTransparency());
        // Get the graphics context of the new image to draw the old image on
        Graphics2D g2d = (Graphics2D) copy.getGraphics();
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();
        return ((BufferedImage) (copy));
    }

}
