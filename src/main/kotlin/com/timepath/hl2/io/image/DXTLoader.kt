package com.timepath.hl2.io.image


import com.timepath.Logger
import java.awt.Color
import java.awt.Graphics2D
import java.awt.image.BufferedImage

class DXTLoader private constructor() {
    companion object {

        private val LOG = Logger()
        private val ALPHA_MASK_555 = 1
        private val BLUE_MASK_555 = 31
        private val BLUE_MASK_565 = 31
        private val GREEN_MASK_555 = 992
        private val GREEN_MASK_565 = 2016
        private val RED_MASK_555 = 31744
        private val RED_MASK_565 = 63488

        public fun load(f: ImageFormat, b: ByteArray, width: Int, height: Int): BufferedImage? {
            when (f) {
                ImageFormat.IMAGE_FORMAT_DXT1, ImageFormat.IMAGE_FORMAT_DXT1_ONEBITALPHA -> return loadDXT1(b, width, height)
                ImageFormat.IMAGE_FORMAT_DXT3 -> return loadDXT3(b, width, height)
                ImageFormat.IMAGE_FORMAT_DXT5 -> return loadDXT5(b, width, height)
            }
            return null
        }

        /**
         * 8 bytes per 4*4
         */
        public fun loadDXT1(b: ByteArray, width: Int, height: Int): BufferedImage {
            val bi = BufferedImage(Math.max(width, 4), Math.max(height, 4), BufferedImage.TYPE_INT_ARGB)
            var pos = 0
            run {
                var y = 0
                while (y < height) {
                    run {
                        var x = 0
                        while (x < width) {
                            val color_0 = ((b[pos++].toInt() and 255) + ((b[pos++].toInt() and 255) shl 8)) and 65535 // 2 bytes
                            val color_1 = ((b[pos++].toInt() and 255) + ((b[pos++].toInt() and 255) shl 8)) and 65535 // 2 bytes
                            val colour = arrayOfNulls<Color>(4)
                            colour[0] = extract565(color_0)
                            colour[1] = extract565(color_1)
                            val c0 = colour[0]!!
                            val c1 = colour[1]!!
                            if (color_0 > color_1) {
                                colour[2] = Color(((2 * c0.getRed()) + c1.getRed()) / 3, ((2 * c0.getGreen()) + c1.getGreen()) / 3, ((2 * c0.getBlue()) + c1.getBlue()) / 3)
                                colour[3] = Color(((2 * c1.getRed()) + c0.getRed()) / 3, ((2 * c1.getGreen()) + c0.getGreen()) / 3, ((2 * c1.getBlue()) + c0.getBlue()) / 3)
                            } else {
                                colour[2] = Color((c0.getRed() + c1.getRed()) / 2, (c0.getGreen() + c1.getGreen()) / 2, (c0.getBlue() + c1.getBlue()) / 2)
                                colour[3] = Color(0, 0, 0, 0)
                            }
                            for (y1 in 0..3) {
                                // 16 bits / 4 rows = 4 bits/line = 1 byte/row
                                val rowData = b[pos++]
                                val rowBits = intArrayOf((rowData.toInt() and 192).ushr(6), (rowData.toInt() and 48).ushr(4), (rowData.toInt() and 12).ushr(2), rowData.toInt() and 3)
                                for (x1 in 0..3) {
                                    // column scan
                                    val c = colour[rowBits[3 - x1]]!!
                                    val col = Color(c.getRed(), c.getGreen(), c.getBlue(), c.getAlpha())
                                    bi.setRGB(x + x1, y + y1, col.getRGB())
                                }
                            }
                            x += 4
                        }
                    }
                    y += 4
                }
            }
            return bi
        }

        /**
         * 8 bytes for alpha channel, additional 8 per 4*4 chunk
         * TODO: fully implement correct colors
         */
        private fun loadDXT3(b: ByteArray, width: Int, height: Int): BufferedImage {
            val bi = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
            val g = bi.getGraphics() as Graphics2D
            //        RGB 565: WORD pixel565 = (red_value << 11) | (green_value << 5) | blue_value;
            var xBlocks = width / 4
            if (xBlocks < 1) {
                xBlocks = 1
            }
            var yBlocks = height / 4
            if (yBlocks < 1) {
                yBlocks = 1
            }
            //        System.err.println("SIZE="+xBlocks+", "+yBlocks+" = " + b.length);
            val bits_78 = 3 // last 2 bits
            val bits_56 = 12 // next 2 bits
            val bits_34 = 48 // next 2 bits
            val bits_12 = 192 // first 2 bits
            var pos = 0
            for (y in 0..yBlocks - 1) {
                for (x in 0..xBlocks - 1) {
                    pos += 8 // 64 bits of alpha channel data (two 8 bit alpha values and a 4x4 3 bit lookup table)
                    val color_0 = (b[pos].toInt() and 255) + ((b[pos + 1].toInt() and 255) shl 8) // 2 bytes
                    pos += 2
                    val color_1 = (b[pos].toInt() and 255) + ((b[pos + 1].toInt() and 255) shl 8) // 2 bytes
                    pos += 2
                    val red1 = ((color_0 and RED_MASK_565) shr 11) shl 3
                    val green1 = ((color_0 and GREEN_MASK_565) shr 5) shl 2
                    val blue1 = (color_0 and BLUE_MASK_565) shl 3
                    val c1 = Color(red1, green1, blue1)
                    val red2 = ((color_1 and RED_MASK_565) shr 11) shl 3
                    val green2 = ((color_1 and GREEN_MASK_565) shr 5) shl 2
                    val blue2 = (color_1 and BLUE_MASK_565) shl 3
                    val c2 = Color(red2, green2, blue2)
                    // remaining 4 bytes
                    val next4 = byteArrayOf(b[pos], b[pos + 1], b[pos + 2], b[pos + 3])
                    pos += 4
                    for (y1 in 0..3) {
                        // 16 bits / 4 lines = 4 bits/line = 1 byte/line
                        val i = next4[y1].toInt()
                        val bits = intArrayOf((i and bits_12) shr 6, (i and bits_34) shr 4, (i and bits_56) shr 2, i and bits_78)
                        (0..3).forEach { i ->
                            val bit = bits[i]
                            if (bit == 0) {
                                g.setColor(c1)
                            } else if (bit == 1) {
                                g.setColor(c2)
                            } else if (bit == 2) {
                                val cred = ((2 * c1.getRed()) / 3) + (c2.getRed() / 3)
                                val cgrn = ((2 * c1.getGreen()) / 3) + (c2.getGreen() / 3)
                                val cblu = ((2 * c1.getBlue()) / 3) + (c2.getBlue() / 3)
                                val c = Color(cred, cgrn, cblu)
                                g.setColor(c)
                            } else if (bit == 3) {
                                val cred = (c1.getRed() / 3) + ((2 * c2.getRed()) / 3)
                                val cgrn = (c1.getGreen() / 3) + ((2 * c2.getGreen()) / 3)
                                val cblu = (c1.getBlue() / 3) + ((2 * c2.getBlue()) / 3)
                                val c = Color(cred, cgrn, cblu)
                                g.setColor(c)
                            }
                            g.drawLine(((x * 4) + 4) - i, (y * 4) + y1, ((x * 4) + 4) - i, (y * 4) + y1)
                        }
                    }
                }
            }
            return bi
        }

        /**
         * 8 bytes for alpha channel, additional 8 per 4*4 chunk
         */
        private fun loadDXT5(b: ByteArray, width: Int, height: Int): BufferedImage {
            val bi = BufferedImage(Math.max(width, 4), Math.max(height, 4), BufferedImage.TYPE_INT_ARGB)
            var pos = 0
            run {
                var y = 0
                while (y < height) {
                    run {
                        var x = 0
                        while (x < width) {
                            // Alpha
                            val a = IntArray(8)
                            a[0] = b[pos++].toInt() and 255 // 64 bits of alpha channel data (two 8 bit alpha values and a 4x4 3 bit lookup table)
                            a[1] = b[pos++].toInt() and 255
                            if (a[0] > a[1]) {
                                a[2] = ((6 * a[0]) + a[1]) / 7
                                a[3] = ((5 * a[0]) + (2 * a[1])) / 7
                                a[4] = ((4 * a[0]) + (3 * a[1])) / 7
                                a[5] = ((3 * a[0]) + (4 * a[1])) / 7
                                a[6] = ((2 * a[0]) + (5 * a[1])) / 7
                                a[7] = (a[0] + (6 * a[1])) / 7
                            } else {
                                a[2] = ((4 * a[0]) + a[1]) / 5
                                a[3] = ((3 * a[0]) + (2 * a[1])) / 5
                                a[4] = ((2 * a[0]) + (3 * a[1])) / 5
                                a[5] = (a[0] + (4 * a[1])) / 5
                                a[6] = 0
                                a[7] = 255
                            }
                            val alphas = Array(4, { IntArray(4) })
                            val alphaByte = intArrayOf(b[pos++].toInt() and 255, b[pos++].toInt() and 255, b[pos++].toInt() and 255, b[pos++].toInt() and 255, b[pos++].toInt() and 255, b[pos++].toInt() and 255)
                            var sel1 = ((alphaByte[2] shl 16) or (alphaByte[1] shl 8) or alphaByte[0]) and 16777215
                            var sel2 = ((alphaByte[5] shl 16) or (alphaByte[4] shl 8) or alphaByte[3]) and 16777215
                            for (yi in 0..1) {
                                for (xi in 0..3) {
                                    alphas[yi][xi] = a[sel1 and 7]
                                    sel1 = sel1 ushr 3
                                }
                            }
                            for (yi in 2..3) {
                                for (xi in 0..3) {
                                    alphas[yi][xi] = a[sel2 and 7]
                                    sel2 = sel2 ushr 3
                                }
                            }
                            // DXT1 color info
                            val color_0 = ((b[pos++].toInt() and 255) + ((b[pos++].toInt() and 255) shl 8)) and 65535 // 2 bytes
                            val color_1 = ((b[pos++].toInt() and 255) + ((b[pos++].toInt() and 255) shl 8)) and 65535 // 2 bytes
                            val colour = arrayOfNulls<Color>(4)
                            colour[0] = extract565(color_0)
                            colour[1] = extract565(color_1)
                            val c0 = colour[0]!!
                            val c1 = colour[1]!!
                            if (color_0 > color_1) {
                                colour[2] = Color(((2 * c0.getRed()) + c1.getRed()) / 3, ((2 * c0.getGreen()) + c1.getGreen()) / 3, ((2 * c0.getBlue()) + c1.getBlue()) / 3)
                                colour[3] = Color(((2 * c1.getRed()) + c0.getRed()) / 3, ((2 * c1.getGreen()) + c0.getGreen()) / 3, ((2 * c1.getBlue()) + c0.getBlue()) / 3)
                            } else {
                                colour[2] = Color((c0.getRed() + c1.getRed()) / 2, (c0.getGreen() + c1.getGreen()) / 2, (c0.getBlue() + c1.getBlue()) / 2)
                                colour[3] = Color(0, 0, 0)
                            }
                            for (y1 in 0..3) {
                                // 16 bits / 4 rows = 4 bits/line = 1 byte/row
                                val rowData = b[pos++]
                                val rowBits = intArrayOf((rowData.toInt() and 192).ushr(6), (rowData.toInt() and 48).ushr(4), (rowData.toInt() and 12).ushr(2), rowData.toInt() and 3)
                                for (x1 in 0..3) {
                                    // column scan
                                    val color = colour[rowBits[3 - x1]]!!
                                    val col = Color(color.getRed(), color.getGreen(), color.getBlue(), alphas[y1][x1])
                                    bi.setRGB(x + x1, y + y1, col.getRGB())
                                }
                            }
                            x += 4
                        }
                    }
                    y += 4
                }
            }
            return bi
        }

        private fun extract555(c: Int): Color {
            return createColor((((c and RED_MASK_555).ushr(10)) shl 3).toFloat(), (((c and GREEN_MASK_555).ushr(5)) shl 3).toFloat(), ((c and BLUE_MASK_555) shl 3).toFloat(), ((c and ALPHA_MASK_555) shl 7).toFloat())
        }

        private fun createColor(r: Float, g: Float, b: Float, a: Float): Color {
            return Color(Math.round(r), Math.round(g), Math.round(b), Math.round(a))
        }

        private fun extract565(c: Int): Color {
            return createColor((((c and RED_MASK_565).ushr(11)) shl 3).toFloat(), (((c and GREEN_MASK_565).ushr(5)) shl 2).toFloat(), ((c and BLUE_MASK_565) shl 3).toFloat(), 255f)
        }
    }
}
