package com.timepath.hl2.io.image;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.image.BufferedImage;

/**
 * @see <a>https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/public/bitmap/imageformat.h</a>
 */
public enum ImageFormat {
    IMAGE_FORMAT_UNKNOWN(-1, 0) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            throw new UnsupportedOperationException("Not supported.");
        }
    },
    /**
     * Red, green, blue, alpha. 32 bpp
     */
    IMAGE_FORMAT_RGBA8888(0, 32) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            return RGBALoader.load(b, width, height, new byte[]{0, 1, 2, 3}, new byte[]{8, 8, 8, 8});
        }
    },
    /**
     * Alpha, blue, green, red. 32 bpp
     */
    IMAGE_FORMAT_ABGR8888(1, 32) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            return RGBALoader.load(b, width, height, new byte[]{3, 2, 1, 0}, new byte[]{8, 8, 8, 8});
        }
    },
    /**
     * Red, green, blue. 24 bpp
     */
    IMAGE_FORMAT_RGB888(2, 24) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            return RGBALoader.load(b, width, height, new byte[]{0, 1, 2}, new byte[]{8, 8, 8});
        }
    },
    /**
     * Blue, green, red. 24 bpp
     */
    IMAGE_FORMAT_BGR888(3, 24) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            return RGBALoader.load(b, width, height, new byte[]{2, 1, 0}, new byte[]{8, 8, 8});
        }
    },
    /**
     * Red (5), green (6), blue (5). 16 bpp
     */
    IMAGE_FORMAT_RGB565(4, 16) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            return RGBALoader.load(b, width, height, new byte[]{0, 1, 2}, new byte[]{5, 6, 5});
        }
    },
    /**
     * Luminance. 8 bpp
     */
    IMAGE_FORMAT_I8(5, 8) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    },
    /**
     * Luminance, alpha. 16 bpp
     */
    IMAGE_FORMAT_IA88(6, 16) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    },
    /**
     * Paletted. 8 bpp
     */
    IMAGE_FORMAT_P8(7, 8) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    },
    /**
     * Alpha (color is 255, 255, 255, a). 8 bpp
     */
    IMAGE_FORMAT_A8(8, 8) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    },
    /**
     * Red, green, blue, "blueScreen" alpha. 24 bpp
     */
    IMAGE_FORMAT_RGB888_BLUESCREEN(9, 24) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    },
    /**
     * Red, green, blue, "blueScreen" alpha. 24 bpp
     */
    IMAGE_FORMAT_BGR888_BLUESCREEN(10, 24) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    },
    /**
     * Alpha, red, green, blue. 32 bpp
     */
    IMAGE_FORMAT_ARGB8888(11, 32) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            return RGBALoader.load(b, width, height, new byte[]{3, 0, 1, 2}, new byte[]{8, 8, 8, 8});
        }
    },
    /**
     * Blue, green, red, alpha. 32 bpp
     */
    IMAGE_FORMAT_BGRA8888(12, 32) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            return RGBALoader.load(b, width, height, new byte[]{2, 1, 0, 3}, new byte[]{8, 8, 8, 8});
        }
    },
    /**
     * DXT1 compressed. 4 bpp
     */
    IMAGE_FORMAT_DXT1(13, 4) {
        @Nullable
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            return DXTLoader.load(this, b, width, height);
        }

        /**
         * Each 'block' is 4*4 pixels + some other data.
         * 16 pixels become 8 bytes [64 bits] (2 * 16 bit colours, 4*4 2 bit indicies)
         */
        @Override
        public int getBytes(int w, int h) {
            return (Math.max(w, 4) * Math.max(h, 4)) / 2;
        }
    },
    /**
     * DXT3 compressed. 8 bpp
     */
    IMAGE_FORMAT_DXT3(14, 8) {
        @Nullable
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            return DXTLoader.load(this, b, width, height);
        }
    },
    /**
     * DXT5 compressed. 8 bpp
     */
    IMAGE_FORMAT_DXT5(15, 8) {
        @Nullable
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            return DXTLoader.load(this, b, width, height);
        }

        /**
         * Each 'block' is 4*4 pixels + some other data.
         * 16 pixels become 16 bytes [128 bits] (2 * 8 bit alpha values,
         * 4x4 3 bit alpha indicies, 2 * 16 bit colours, 4*4 2 bit indicies)
         */
        @Override
        public int getBytes(int w, int h) {
            return Math.max(w, 4) * Math.max(h, 4);
        }
    },
    /**
     * Blue, green, red, padding. 32 bpp
     */
    IMAGE_FORMAT_BGRX8888(16, 32) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            return RGBALoader.load(b, width, height, new byte[]{2, 1, 0, -1}, new byte[]{8, 8, 8, 8});
        }
    },
    /**
     * Blue (5), green (6), red (5). 16 bpp
     */
    IMAGE_FORMAT_BGR565(17, 16) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            return RGBALoader.load(b, width, height, new byte[]{2, 1, 0}, new byte[]{5, 6, 5});
        }
    },
    /**
     * Blue (5), green (5), red (5), padding (1). 16 bpp
     */
    IMAGE_FORMAT_BGRX5551(18, 16) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            return RGBALoader.load(b, width, height, new byte[]{2, 1, 0, -1}, new byte[]{5, 5, 5, 1});
        }
    },
    /**
     * Blue (4), green (4), red (4), alpha (4). 16 bpp
     */
    IMAGE_FORMAT_BGRA4444(19, 16) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            return RGBALoader.load(b, width, height, new byte[]{2, 1, 0, 3}, new byte[]{4, 4, 4, 4});
        }
    },
    /**
     * DXT1 with alpha (1), special case. 4 bpp
     */
    IMAGE_FORMAT_DXT1_ONEBITALPHA(20, 4) {
        @Nullable
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            return DXTLoader.load(this, b, width, height);
        }
    },
    /**
     * Blue (5), green (5), red (5), alpha (1). 16 bpp
     */
    IMAGE_FORMAT_BGRA5551(21, 16) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            return RGBALoader.load(b, width, height, new byte[]{2, 1, 0, 3}, new byte[]{5, 5, 5, 1});
        }
    },
    /**
     * 2 channel DuDv/normal maps. 16 bpp
     */
    IMAGE_FORMAT_UV88(22, 16) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            return UVLoader.load(b, width, height, 2);
        }
    },
    /**
     * 4 channel DuDv/normal maps. 32 bpp
     */
    IMAGE_FORMAT_UVWQ8888(23, 32) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            return UVLoader.load(b, width, height, 4);
        }
    },
    /**
     * Red (16), green (16), blue (16), alpha (16). 64 bpp
     */
    IMAGE_FORMAT_RGBA16161616F(24, 64) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            return RGBALoader.load(b, width, height, new byte[]{0, 1, 2, 3}, new byte[]{16, 16, 16, 16});
        }
    },
    /**
     * Red (16), green (16), blue (16), alpha (16) with mantissa. 64 bpp
     */
    IMAGE_FORMAT_RGBA16161616(25, 64) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    },
    /**
     * 4 channel DuDv/normal maps. 32 bpp
     */
    IMAGE_FORMAT_UVLX8888(26, 32) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            return UVLoader.load(b, width, height, 4);
        }
    },
    /**
     * Luminance. 32 bpp
     */
    IMAGE_FORMAT_R32F(27, 32) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    },
    /**
     * Red (32), green (32), blue (32). 96 bpp
     */
    IMAGE_FORMAT_RGB323232F(28, 96) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            return RGBALoader.load(b, width, height, new byte[]{0, 1, 2}, new byte[]{32, 32, 32});
        }
    },
    /**
     * Red (32), green (32), blue (32), alpha (32). 128 bpp
     */
    IMAGE_FORMAT_RGBA32323232F(29, 128) {
        @NotNull
        @Override
        public BufferedImage load(byte[] b, int width, int height) {
            return RGBALoader.load(b, width, height, new byte[]{0, 1, 2, 3}, new byte[]{32, 32, 32, 32});
        }
    };
    private final int index, bpp;

    ImageFormat(int index, int bpp) {
        this.index = index;
        this.bpp = bpp;
    }

    @Nullable
    public static ImageFormat getEnumForIndex(int index) {
        ImageFormat[] values = ImageFormat.values();
        for (@NotNull ImageFormat eachValue : values) {
            if (eachValue.index == index) {
                return eachValue;
            }
        }
        return null;
    }

    int getIndex() {
        return index;
    }

    @Nullable
    public abstract BufferedImage load(byte[] b, int width, int height);

    public int getBytes(int w, int h) {
        return (bpp * w * h) / 8;
    }
}
