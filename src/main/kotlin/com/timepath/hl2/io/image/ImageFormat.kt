package com.timepath.hl2.io.image


import java.awt.image.BufferedImage

/**
 * @see <a>https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/public/bitmap/imageformat.h</a>
 */
public enum class ImageFormat(val index: Int, private val bpp: Int) {
    IMAGE_FORMAT_UNKNOWN(-1, 0) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            throw UnsupportedOperationException()
        }
    },
    /**
     * Red, green, blue, alpha. 32 bpp
     */
    IMAGE_FORMAT_RGBA8888(0, 32) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArrayOf(0, 1, 2, 3), byteArrayOf(8, 8, 8, 8));
        }
    },
    /**
     * Alpha, blue, green, red. 32 bpp
     */
    IMAGE_FORMAT_ABGR8888(1, 32) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArrayOf(3, 2, 1, 0), byteArrayOf(8, 8, 8, 8));
        }
    },
    /**
     * Red, green, blue. 24 bpp
     */
    IMAGE_FORMAT_RGB888(2, 24) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArrayOf(0, 1, 2), byteArrayOf(8, 8, 8));
        }
    },
    /**
     * Blue, green, red. 24 bpp
     */
    IMAGE_FORMAT_BGR888(3, 24) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArrayOf(2, 1, 0), byteArrayOf(8, 8, 8));
        }
    },
    /**
     * Red (5), green (6), blue (5). 16 bpp
     */
    IMAGE_FORMAT_RGB565(4, 16) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArrayOf(0, 1, 2), byteArrayOf(5, 6, 5));
        }
    },
    /**
     * Luminance. 8 bpp
     */
    IMAGE_FORMAT_I8(5, 8) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            throw UnsupportedOperationException()
        }
    },
    /**
     * Luminance, alpha. 16 bpp
     */
    IMAGE_FORMAT_IA88(6, 16) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            throw UnsupportedOperationException()
        }
    },
    /**
     * Paletted. 8 bpp
     */
    IMAGE_FORMAT_P8(7, 8) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            throw UnsupportedOperationException()
        }
    },
    /**
     * Alpha (color is 255, 255, 255, a). 8 bpp
     */
    IMAGE_FORMAT_A8(8, 8) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            throw UnsupportedOperationException()
        }
    },
    /**
     * Red, green, blue, "blueScreen" alpha. 24 bpp
     */
    IMAGE_FORMAT_RGB888_BLUESCREEN(9, 24) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            throw UnsupportedOperationException()
        }
    },
    /**
     * Red, green, blue, "blueScreen" alpha. 24 bpp
     */
    IMAGE_FORMAT_BGR888_BLUESCREEN(10, 24) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            throw UnsupportedOperationException()
        }
    },
    /**
     * Alpha, red, green, blue. 32 bpp
     */
    IMAGE_FORMAT_ARGB8888(11, 32) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArrayOf(3, 0, 1, 2), byteArrayOf(8, 8, 8, 8));
        }
    },
    /**
     * Blue, green, red, alpha. 32 bpp
     */
    IMAGE_FORMAT_BGRA8888(12, 32) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArrayOf(2, 1, 0, 3), byteArrayOf(8, 8, 8, 8));
        }
    },
    /**
     * DXT1 compressed. 4 bpp
     */
    IMAGE_FORMAT_DXT1(13, 4) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return DXTLoader.load(this, b, width, height);
        }

        /**
         * Each 'block' is 4*4 pixels + some other data.
         * 16 pixels become 8 bytes [64 bits] (2 * 16 bit colours, 4*4 2 bit indicies)
         */
        override fun getBytes(w: Int, h: Int): Int {
            return (Math.max(w, 4) * Math.max(h, 4)) / 2;
        }
    },
    /**
     * Each 'block' is 4*4 pixels + some other data.
     * 16 pixels become 8 bytes [64 bits] (2 * 16 bit colours, 4*4 2 bit indicies)
     */
    /**
     * DXT3 compressed. 8 bpp
     */
    IMAGE_FORMAT_DXT3(14, 8) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return DXTLoader.load(this, b, width, height);
        }
    },
    /**
     * DXT5 compressed. 8 bpp
     */
    IMAGE_FORMAT_DXT5(15, 8) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return DXTLoader.load(this, b, width, height);
        }

        /**
         * Each 'block' is 4*4 pixels + some other data.
         * 16 pixels become 16 bytes [128 bits] (2 * 8 bit alpha values,
         * 4x4 3 bit alpha indicies, 2 * 16 bit colours, 4*4 2 bit indicies)
         */
        override fun getBytes(w: Int, h: Int): Int {
            return Math.max(w, 4) * Math.max(h, 4);
        }
    },
    /**
     * Each 'block' is 4*4 pixels + some other data.
     * 16 pixels become 16 bytes [128 bits] (2 * 8 bit alpha values,
     * 4x4 3 bit alpha indicies, 2 * 16 bit colours, 4*4 2 bit indicies)
     */
    /**
     * Blue, green, red, padding. 32 bpp
     */
    IMAGE_FORMAT_BGRX8888(16, 32) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArrayOf(2, 1, 0, -1), byteArrayOf(8, 8, 8, 8));
        }
    },
    /**
     * Blue (5), green (6), red (5). 16 bpp
     */
    IMAGE_FORMAT_BGR565(17, 16) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArrayOf(2, 1, 0), byteArrayOf(5, 6, 5));
        }
    },
    /**
     * Blue (5), green (5), red (5), padding (1). 16 bpp
     */
    IMAGE_FORMAT_BGRX5551(18, 16) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArrayOf(2, 1, 0, -1), byteArrayOf(5, 5, 5, 1));
        }
    },
    /**
     * Blue (4), green (4), red (4), alpha (4). 16 bpp
     */
    IMAGE_FORMAT_BGRA4444(19, 16) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArrayOf(2, 1, 0, 3), byteArrayOf(4, 4, 4, 4));
        }
    },
    /**
     * DXT1 with alpha (1), special case. 4 bpp
     */
    IMAGE_FORMAT_DXT1_ONEBITALPHA(20, 4) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return DXTLoader.load(this, b, width, height);
        }
    },
    /**
     * Blue (5), green (5), red (5), alpha (1). 16 bpp
     */
    IMAGE_FORMAT_BGRA5551(21, 16) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArrayOf(2, 1, 0, 3), byteArrayOf(5, 5, 5, 1));
        }
    },
    /**
     * 2 channel DuDv/normal maps. 16 bpp
     */
    IMAGE_FORMAT_UV88(22, 16) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return UVLoader.load(b, width, height, 2);
        }
    },
    /**
     * 4 channel DuDv/normal maps. 32 bpp
     */
    IMAGE_FORMAT_UVWQ8888(23, 32) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return UVLoader.load(b, width, height, 4);
        }
    },
    /**
     * Red (16), green (16), blue (16), alpha (16). 64 bpp
     */
    IMAGE_FORMAT_RGBA16161616F(24, 64) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArrayOf(0, 1, 2, 3), byteArrayOf(16, 16, 16, 16));
        }
    },
    /**
     * Red (16), green (16), blue (16), alpha (16) with mantissa. 64 bpp
     */
    IMAGE_FORMAT_RGBA16161616(25, 64) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            throw UnsupportedOperationException()
        }
    },
    /**
     * 4 channel DuDv/normal maps. 32 bpp
     */
    IMAGE_FORMAT_UVLX8888(26, 32) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return UVLoader.load(b, width, height, 4);
        }
    },
    /**
     * Luminance. 32 bpp
     */
    IMAGE_FORMAT_R32F(27, 32) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            throw UnsupportedOperationException()
        }
    },
    /**
     * Red (32), green (32), blue (32). 96 bpp
     */
    IMAGE_FORMAT_RGB323232F(28, 96) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArrayOf(0, 1, 2), byteArrayOf(32, 32, 32));
        }
    },
    /**
     * Red (32), green (32), blue (32), alpha (32). 128 bpp
     */
    IMAGE_FORMAT_RGBA32323232F(29, 128) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArrayOf(0, 1, 2, 3), byteArrayOf(32, 32, 32, 32));
        }
    };

    companion object {
        public fun getEnumForIndex(index: Int): ImageFormat? {
            val values = ImageFormat.values()
            for (eachValue in values) {
                if (eachValue.index == index) {
                    return eachValue
                }
            }
            return null
        }
    }

    public abstract fun load(b: ByteArray, width: Int, height: Int): BufferedImage?

    public open fun getBytes(w: Int, h: Int): Int {
        return (bpp * w * h) / 8
    }
}
