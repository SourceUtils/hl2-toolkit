package com.timepath.hl2.io.image


import java.awt.image.BufferedImage

/**
 * @see <a>https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/public/bitmap/imageformat.h</a>
 */
public enum class ImageFormat(val index: Int, private val bpp: Int) {
    IMAGE_FORMAT_UNKNOWN : ImageFormat(-1, 0) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            throw UnsupportedOperationException()
        }
    }
    /**
     * Red, green, blue, alpha. 32 bpp
     */
    IMAGE_FORMAT_RGBA8888 : ImageFormat(0, 32) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArray(0, 1, 2, 3), byteArray(8, 8, 8, 8));
        }
    }
    /**
     * Alpha, blue, green, red. 32 bpp
     */
    IMAGE_FORMAT_ABGR8888 : ImageFormat(1, 32) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArray(3, 2, 1, 0), byteArray(8, 8, 8, 8));
        }
    }
    /**
     * Red, green, blue. 24 bpp
     */
    IMAGE_FORMAT_RGB888 : ImageFormat(2, 24) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArray(0, 1, 2), byteArray(8, 8, 8));
        }
    }
    /**
     * Blue, green, red. 24 bpp
     */
    IMAGE_FORMAT_BGR888 : ImageFormat(3, 24) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArray(2, 1, 0), byteArray(8, 8, 8));
        }
    }
    /**
     * Red (5), green (6), blue (5). 16 bpp
     */
    IMAGE_FORMAT_RGB565 : ImageFormat(4, 16) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArray(0, 1, 2), byteArray(5, 6, 5));
        }
    }
    /**
     * Luminance. 8 bpp
     */
    IMAGE_FORMAT_I8 : ImageFormat(5, 8) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            throw UnsupportedOperationException()
        }
    }
    /**
     * Luminance, alpha. 16 bpp
     */
    IMAGE_FORMAT_IA88 : ImageFormat(6, 16) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            throw UnsupportedOperationException()
        }
    }
    /**
     * Paletted. 8 bpp
     */
    IMAGE_FORMAT_P8 : ImageFormat(7, 8) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            throw UnsupportedOperationException()
        }
    }
    /**
     * Alpha (color is 255, 255, 255, a). 8 bpp
     */
    IMAGE_FORMAT_A8 : ImageFormat(8, 8) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            throw UnsupportedOperationException()
        }
    }
    /**
     * Red, green, blue, "blueScreen" alpha. 24 bpp
     */
    IMAGE_FORMAT_RGB888_BLUESCREEN : ImageFormat(9, 24) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            throw UnsupportedOperationException()
        }
    }
    /**
     * Red, green, blue, "blueScreen" alpha. 24 bpp
     */
    IMAGE_FORMAT_BGR888_BLUESCREEN : ImageFormat(10, 24) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            throw UnsupportedOperationException()
        }
    }
    /**
     * Alpha, red, green, blue. 32 bpp
     */
    IMAGE_FORMAT_ARGB8888 : ImageFormat(11, 32) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArray(3, 0, 1, 2), byteArray(8, 8, 8, 8));
        }
    }
    /**
     * Blue, green, red, alpha. 32 bpp
     */
    IMAGE_FORMAT_BGRA8888 : ImageFormat(12, 32) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArray(2, 1, 0, 3), byteArray(8, 8, 8, 8));
        }
    }
    /**
     * DXT1 compressed. 4 bpp
     */
    IMAGE_FORMAT_DXT1 : ImageFormat(13, 4) {
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
    }
    /**
     * Each 'block' is 4*4 pixels + some other data.
     * 16 pixels become 8 bytes [64 bits] (2 * 16 bit colours, 4*4 2 bit indicies)
     */
    /**
     * DXT3 compressed. 8 bpp
     */
    IMAGE_FORMAT_DXT3 : ImageFormat(14, 8) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return DXTLoader.load(this, b, width, height);
        }
    }
    /**
     * DXT5 compressed. 8 bpp
     */
    IMAGE_FORMAT_DXT5 : ImageFormat(15, 8) {
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
    }
    /**
     * Each 'block' is 4*4 pixels + some other data.
     * 16 pixels become 16 bytes [128 bits] (2 * 8 bit alpha values,
     * 4x4 3 bit alpha indicies, 2 * 16 bit colours, 4*4 2 bit indicies)
     */
    /**
     * Blue, green, red, padding. 32 bpp
     */
    IMAGE_FORMAT_BGRX8888 : ImageFormat(16, 32) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArray(2, 1, 0, -1), byteArray(8, 8, 8, 8));
        }
    }
    /**
     * Blue (5), green (6), red (5). 16 bpp
     */
    IMAGE_FORMAT_BGR565 : ImageFormat(17, 16) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArray(2, 1, 0), byteArray(5, 6, 5));
        }
    }
    /**
     * Blue (5), green (5), red (5), padding (1). 16 bpp
     */
    IMAGE_FORMAT_BGRX5551 : ImageFormat(18, 16) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArray(2, 1, 0, -1), byteArray(5, 5, 5, 1));
        }
    }
    /**
     * Blue (4), green (4), red (4), alpha (4). 16 bpp
     */
    IMAGE_FORMAT_BGRA4444 : ImageFormat(19, 16) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArray(2, 1, 0, 3), byteArray(4, 4, 4, 4));
        }
    }
    /**
     * DXT1 with alpha (1), special case. 4 bpp
     */
    IMAGE_FORMAT_DXT1_ONEBITALPHA : ImageFormat(20, 4) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return DXTLoader.load(this, b, width, height);
        }
    }
    /**
     * Blue (5), green (5), red (5), alpha (1). 16 bpp
     */
    IMAGE_FORMAT_BGRA5551 : ImageFormat(21, 16) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArray(2, 1, 0, 3), byteArray(5, 5, 5, 1));
        }
    }
    /**
     * 2 channel DuDv/normal maps. 16 bpp
     */
    IMAGE_FORMAT_UV88 : ImageFormat(22, 16) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return UVLoader.load(b, width, height, 2);
        }
    }
    /**
     * 4 channel DuDv/normal maps. 32 bpp
     */
    IMAGE_FORMAT_UVWQ8888 : ImageFormat(23, 32) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return UVLoader.load(b, width, height, 4);
        }
    }
    /**
     * Red (16), green (16), blue (16), alpha (16). 64 bpp
     */
    IMAGE_FORMAT_RGBA16161616F : ImageFormat(24, 64) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArray(0, 1, 2, 3), byteArray(16, 16, 16, 16));
        }
    }
    /**
     * Red (16), green (16), blue (16), alpha (16) with mantissa. 64 bpp
     */
    IMAGE_FORMAT_RGBA16161616 : ImageFormat(25, 64) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            throw UnsupportedOperationException()
        }
    }
    /**
     * 4 channel DuDv/normal maps. 32 bpp
     */
    IMAGE_FORMAT_UVLX8888 : ImageFormat(26, 32) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return UVLoader.load(b, width, height, 4);
        }
    }
    /**
     * Luminance. 32 bpp
     */
    IMAGE_FORMAT_R32F : ImageFormat(27, 32) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            throw UnsupportedOperationException()
        }
    }
    /**
     * Red (32), green (32), blue (32). 96 bpp
     */
    IMAGE_FORMAT_RGB323232F : ImageFormat(28, 96) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArray(0, 1, 2), byteArray(32, 32, 32));
        }
    }
    /**
     * Red (32), green (32), blue (32), alpha (32). 128 bpp
     */
    IMAGE_FORMAT_RGBA32323232F : ImageFormat(29, 128) {
        override fun load(b: ByteArray, width: Int, height: Int): BufferedImage? {
            return RGBALoader.load(b, width, height, byteArray(0, 1, 2, 3), byteArray(32, 32, 32, 32));
        }
    }

    class object {
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
