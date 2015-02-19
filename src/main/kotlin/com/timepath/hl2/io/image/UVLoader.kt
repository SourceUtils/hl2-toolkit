package com.timepath.hl2.io.image


import java.awt.*
import java.awt.image.BufferedImage
import java.util.logging.Logger

/**
 * @author TimePath
 */
class UVLoader private() {
    class object {

        private val LOG = Logger.getLogger(javaClass<UVLoader>().getName())

        public fun load(d: ByteArray, width: Int, height: Int, channels: Int): BufferedImage {
            return loadUV(d, width, height)
        }

        private fun loadUV(b: ByteArray, width: Int, height: Int): BufferedImage {
            val bi = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val g = bi.getGraphics() as Graphics2D
            var pos = 0
            for (y in 0..height - 1) {
                for (x in 0..width - 1) {
                    g.setColor(Color((b[pos].toInt() and 255) + ((b[pos + 1].toInt() and 255) shl 16) + ((255 and 255) shl 24)))
                    pos += 2
                    g.drawLine(x, y, x, y)
                }
            }
            return bi
        }
    }
}
