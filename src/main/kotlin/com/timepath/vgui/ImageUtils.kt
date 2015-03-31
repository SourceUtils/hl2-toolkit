package com.timepath.vgui

import java.awt.*
import java.awt.image.BufferedImage

/**
 * @author TimePath
 */
public class ImageUtils {
    companion object {
        /**
         * Scales an image to be the desired height, cutting off the sides.
         */
        public fun resizeImage(image: Image, w: Int, h: Int): BufferedImage {
            val type = BufferedImage.TYPE_INT_ARGB
            val scaled = BufferedImage(w, h, type)
            // Keep aspect ratio
            val proposedWidth = Math.round(((h / image.getHeight(null)).toFloat()).toDouble() * image.getWidth(null).toDouble()).toInt()
            // Half the width difference is excess
            val excess = Math.abs(proposedWidth - w) / 2
            // Create graphics context with nice settings
            val g = scaled.createGraphics()
            g.setComposite(AlphaComposite.SrcOver)
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            // Draw with excess off screen
            g.drawImage(image, -excess, 0, w + (2 * excess), h, null)
            g.dispose()
            return scaled
        }

        public fun toCompatibleImage(image: BufferedImage): BufferedImage {
            // Obtain the current system graphical settings
            val configuration = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration()

            // If image is already compatible and optimized for current system settings, simply return it
            if (image.getColorModel() == configuration.getColorModel())
                return image
            // Image is not optimized, create a new image that is
            val copy = configuration.createCompatibleImage(image.getWidth(), image.getHeight(), image.getTransparency())
            // Get the graphics context of the new image to draw the old image on
            val g2d = copy.getGraphics() as Graphics2D
            g2d.drawImage(image, 0, 0, null)
            g2d.dispose()
            return copy
        }
    }

}
