package com.timepath.hl2.io.image


import java.awt.image.BufferedImage
import java.awt.image.WritableRaster
import java.util.logging.Logger

/**
 * @author TimePath
 */
class RGBALoader private() {
    class object {

        private val LOG = Logger.getLogger(javaClass<RGBALoader>().getName())

        public fun load(d: ByteArray, width: Int, height: Int, order: ByteArray, len: ByteArray): BufferedImage {
            var bpp = 0
            for (l in len) {
                if ((l % 8) != 0) {
                    throw UnsupportedOperationException("Not supported yet.")
                }
                bpp += l
            }
            val bi = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val raster = bi.getRaster()
            var pos = 0
            for (y in 0..height - 1) {
                for (x in 0..width - 1) {
                    raster.setSample(x, y, 3, 255)
                    for (b in order) {
                        raster.setSample(x, y, b.toInt(), d[pos].toInt())
                        pos += bpp / 8 / order.size
                    }
                }
            }
            return bi
        }
    }
}
