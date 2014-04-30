package com.timepath.hl2.io.image;

/**
 * https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/public/bitmap/imageformat.h
 */
public enum ImageFormat {

    IMAGE_FORMAT_UNKNOWN(-1),
    /**
     * Red, green, blue, alpha. 32 bpp
     */
    IMAGE_FORMAT_RGBA8888(0),
    /**
     * Alpha, blue, green, red. 32 bpp
     */
    IMAGE_FORMAT_ABGR8888(1),
    /**
     * Red, green, blue. 24 bpp
     */
    IMAGE_FORMAT_RGB888(2),
    /**
     * Blue, green, red. 24 bpp
     */
    IMAGE_FORMAT_BGR888(3),
    /**
     * Red (5), green (6), blue (5). 16 bpp
     */
    IMAGE_FORMAT_RGB565(4),
    /**
     * Luminance. 8 bpp
     */
    IMAGE_FORMAT_I8(5),
    /**
     * Luminance, alpha. 16 bpp
     */
    IMAGE_FORMAT_IA88(6),
    /**
     * Paletted. 8 bpp
     */
    IMAGE_FORMAT_P8(7),
    /**
     * Alpha (color is 255, 255, 255, a). 8 bpp
     */
    IMAGE_FORMAT_A8(8),
    /**
     * Red, green, blue, "blueScreen" alpha. 24 bpp
     */
    IMAGE_FORMAT_RGB888_BLUESCREEN(9),
    /**
     * Red, green, blue, "blueScreen" alpha. 24 bpp
     */
    IMAGE_FORMAT_BGR888_BLUESCREEN(10),
    /**
     * Alpha, red, green, blue. 32 bpp
     */
    IMAGE_FORMAT_ARGB8888(11),
    /**
     * Blue, green, red, alpha. 32 bpp
     */
    IMAGE_FORMAT_BGRA8888(12),
    /**
     * DXT1 compressed. 4 bpp
     */
    IMAGE_FORMAT_DXT1(13),
    /**
     * DXT3 compressed. 8 bpp
     */
    IMAGE_FORMAT_DXT3(14),
    /**
     * DXT5 compressed. 8 bpp
     */
    IMAGE_FORMAT_DXT5(15),
    /**
     * Blue, green, red, padding. 32 bpp
     */
    IMAGE_FORMAT_BGRX8888(16),
    /**
     * Blue (5), green (6), red (5). 16 bpp
     */
    IMAGE_FORMAT_BGR565(17),
    /**
     * Blue (5), green (5), red (5), padding (1). 16 bpp
     */
    IMAGE_FORMAT_BGRX5551(18),
    /**
     * Blue (4), green (4), red (4), alpha (4). 16 bpp
     */
    IMAGE_FORMAT_BGRA4444(19),
    /**
     * DXT1 with alpha (1), special case. 4 bpp
     */
    IMAGE_FORMAT_DXT1_ONEBITALPHA(20),
    /**
     * Blue (5), green (5), red (5), alpha (1). 16 bpp
     */
    IMAGE_FORMAT_BGRA5551(21),
    /**
     * 2 channel DuDv/normal maps. 16 bpp
     */
    IMAGE_FORMAT_UV88(22),
    /**
     * 4 channel DuDv/normal maps. 16 bpp
     */
    IMAGE_FORMAT_UVWQ8888(23),
    /**
     * Red (16), green (16), blue (16), alpha (16). 64 bpp
     */
    IMAGE_FORMAT_RGBA16161616F(24),
    /**
     * Red (16), green (16), blue (16), alpha (16) with mantissa. 64 bpp
     */
    IMAGE_FORMAT_RGBA16161616(25),
    /**
     * 4 channel DuDv/normal maps. 16 bpp
     */
    IMAGE_FORMAT_UVLX8888(26),
    /**
     * Luminance. 32 bpp
     */
    IMAGE_FORMAT_R32F(27),
    /**
     * Red (32), green (32), blue (32). 96 bpp
     */
    IMAGE_FORMAT_RGB323232F(28),
    /**
     * Red (32), green (32), blue (32), alpha (32). 128 bpp
     */
    IMAGE_FORMAT_RGBA32323232F(29);

    private final int index;

    private ImageFormat(int index) {
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    public static ImageFormat getEnumForIndex(int index) {
        ImageFormat[] values = ImageFormat.values();
        for(ImageFormat eachValue : values) {
            if(eachValue.getIndex() == index) {
                return eachValue;
            }
        }
        return null;
    }

}
