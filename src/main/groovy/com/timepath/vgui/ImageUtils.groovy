package com.timepath.vgui

import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.logging.Log

import java.awt.*
import java.awt.image.BufferedImage
/**
 * @author TimePath
 */
@CompileStatic
@TypeChecked
@Log('LOG')
class ImageUtils {

    /**
     * Scales an image to be the desired height, cutting off the sides.
     * @param image
     * @return
     */
    static BufferedImage resizeImage(Image image, int w, int h) {
        int type = BufferedImage.TYPE_INT_ARGB
        def scaled = new BufferedImage(w, h, type)
        // Keep aspect ratio
        int proposedWidth = Math.round((h / image.getHeight(null) as float) * image.getWidth(null))
        // Half the width difference is excess
        int excess = Math.abs(proposedWidth - w) / 2 as int
        // Create graphics context with nice settings
        def g = scaled.createGraphics()
        g.composite = AlphaComposite.SrcOver
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION as RenderingHints.Key, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.setRenderingHint(RenderingHints.KEY_RENDERING as RenderingHints.Key, RenderingHints.VALUE_RENDER_QUALITY)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING as RenderingHints.Key, RenderingHints.VALUE_ANTIALIAS_ON)
        // Draw with excess off screen
        g.drawImage(image, -excess, 0, w + (2 * excess), h, null)
        g.dispose()
        return scaled
    }

    static BufferedImage toCompatibleImage(BufferedImage image) {
        // Obtain the current system graphical settings
        def configuration = GraphicsEnvironment.localGraphicsEnvironment.defaultScreenDevice.defaultConfiguration

        // If image is already compatible and optimized for current system settings, simply return it
        if (image.colorModel == configuration.colorModel) return image
        // Image is not optimized, create a new image that is
        def copy = configuration.createCompatibleImage(image.width, image.height, image.transparency)
        // Get the graphics context of the new image to draw the old image on
        def g2d = copy.graphics as Graphics2D
        g2d.drawImage(image, 0, 0, null)
        g2d.dispose()
        return copy
    }
}
