package com.timepath.hl2.io.image;

import com.timepath.EnumFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @see <a>https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/public/vtf/vtf.h</a>
 * @see <a>https://developer.valvesoftware.com/wiki/Valve_Texture_Format#Image_flags</a>
 */
public enum VTFFlags implements EnumFlag {
    // Flags from the *.txt config file
    POINTSAMPLE(0x00000001, "Point Sample"),
    TRILINEAR(0x00000002, "Trilinear"),
    CLAMPS(0x00000004, "Clamp S"),
    CLAMPT(0x00000008, "Clamp T"),
    ANISOTROPIC(0x00000010, "Anisotropic"),
    HINT_DXT5(0x00000020, "Hint DXT5"),
    PWL_CORRECTED(0x00000040, "SRGB"),
    NORMAL(0x00000080, "Normal Map"),
    NOMIP(0x00000100, "No Mipmap"),
    NOLOD(0x00000200, "No Level Of Detail"),
    ALL_MIPS(0x00000400, "No Minimum Mipmap"),
    PROCEDURAL(0x00000800, "Procedural"),
    /**
     * Automatically generated by vtex from the texture data.
     */
    ONEBITALPHA(0x00001000, "One Bit Alpha (Format Specified)"),
    /**
     * Automatically generated by vtex from the texture data.
     */
    EIGHTBITALPHA(0x00002000, "Eight Bit Alpha (Format Specified)"),
    ENVMAP(0x00004000, "Environment Map (Format Specified)"),
    RENDERTARGET(0x00008000, "Render Target"),
    DEPTHRENDERTARGET(0x00010000, "Depth Render Target"),
    NODEBUGOVERRIDE(0x00020000, "No Debug Override"),
    SINGLECOPY(0x00040000, "Single Copy"),
    PRE_SRGB(0x00080000),
    UNUSED_00100000(0x00100000),
    UNUSED_00200000(0x00200000),
    UNUSED_00400000(0x00400000),
    NODEPTHBUFFER(0x00800000, "No Depth Buffer"),
    UNUSED_01000000(0x01000000),
    CLAMPU(0x02000000, "Clamp U"),
    /**
     * Usable as a vertex texture
     */
    VERTEXTEXTURE(0x04000000, "Vertex Texture"),
    SSBUMP(0x08000000, "SSBump"),
    UNUSED_10000000(0x10000000),
    /**
     * Clamp to border color on all texture coordinates
     */
    BORDER(0x20000000, "Clamp All"),
    UNUSED_40000000(0x40000000),
    UNUSED_80000000(0x80000000);
    private final int mask;
    private final String title;

    VTFFlags(int mask) {
        this(mask, "Unused");
    }

    VTFFlags(int mask, String name) {
        this.mask = mask;
        title = name;
    }

    @Nullable
    public static VTFFlags getEnumForMask(int mask) {
        VTFFlags[] values = VTFFlags.values();
        for (@NotNull VTFFlags eachValue : values) {
            if (eachValue.mask == mask) {
                return eachValue;
            }
        }
        return null;
    }

    int getMask() {
        return mask;
    }

    @Override
    public int getId() {
        return mask;
    }

    @Override
    public String toString() {
        return title;
    }
}
