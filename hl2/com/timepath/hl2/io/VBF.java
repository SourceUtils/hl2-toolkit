package com.timepath.hl2.io;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

/**
 *
 * @author timepath
 */
public class VBF {

    /*
     * Structure with respect to: "materials/vgui/fonts/buttons_32.vbf"
     *
     * // style flags
     * #define BF_BOLD	0x0001
     * #define BF_ITALIC	0x0002
     * #define BF_OUTLINED	0x0004
     * #define BF_DROPSHADOW	0x0008
     * #define BF_BLURRED	0x0010
     * #define BF_SCANLINES	0x0020
     * #define BF_ANTIALIASED	0x0040
     * #define BF_CUSTOM	0x0080
     *
     * char[4] = V,B,S,P
     * int version = 3
     * short width = 256
     * short height = 256
     * short maxcharwidth = 64
     * short maxcharheight = 32
     * short flags = 128
     * short ascent = 0
     * short total+1 = 43
     *
     * byte table[256]
     *
     * be at 278
     * 
     * skip to total (42 hex) *
     *
     * skip to 296 (0x128) (39 padding ints) // dummy3?
     *
     * for(int i = 0; i < 146; i++) {
     * short width
     * short height
     * }
     *
     */
    public static void main(String... args) throws IOException {
        InputStream is = new FileInputStream("/home/timepath/Desktop/ico/hl2/materials/VGUI/fonts/Buttons_32.vbf");
        byte[] array = new byte[is.available()];
        is.read(array);
        ByteBuffer buf = ByteBuffer.wrap(array);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        
        int expectedHeader = (('T' << 24) | ('N' << 16) | ('F' << 8) | ('V'));
        int expectedVersion = 3;
        
        // BitmapFont
        
        int header = buf.getInt();
        int version = buf.getInt();
        short width = buf.getShort();
        short height = buf.getShort();
        short maxcharwidth = buf.getShort();
        short maxcharheight = buf.getShort();
        short flags = buf.getShort();
        short ascent = buf.getShort();
        short totalAnd1 = buf.getShort();
        
        Object[][] dbg = {
            {"Header = ", header},
            {"Version = ", version},
            {"Width = ", width},
            {"Height = ", height},
            {"MaxCharWidth = ", maxcharwidth},
            {"MaxCharHeight = ", maxcharheight},
            {"Flags = ", flags},
            {"Ascent = ", ascent},
            {"Total = ", totalAnd1}
        };
        
        for(int i = 0; i < dbg.length; i++) {
            StringBuilder sb = new StringBuilder();
            for(int x = 0; x < dbg[i].length; x++) {
                sb.append(dbg[i][x]);
            }
            System.out.println(sb.toString());
        }
        
        System.out.println("Characters:");
        
        byte[] table = new byte[256];
        buf.get(table);
        
        // BitmapGlyph
        
        for(int i = 0; i < totalAnd1; i++) {
            short x = buf.getShort();
            short y = buf.getShort();
            short w = buf.getShort();
            short h = buf.getShort();
            short a = buf.getShort();
            short b = buf.getShort();
            short c = buf.getShort();
            if(i != 0) {
                for(int j = 0; j < table.length; j++) {
                    if(table[j] == i) {
                        System.out.println((char) j  + ": (" + x + ", " + y + ")[" + w + ", " + h + "]{" + a + ", " + b + ", " + c + "}");
                    }
                }
            }
        }
    }
    /*
     * Icon mappings with respect to: "materials/vgui/fonts/buttons_32.vtf"
     * 
     * A = A
     * B = B
     * X = X
     * Y = Y
     * Dpad up = U
     * Dpad down = D
     * Dpad left = L
     * Dpad right = R
     * 
     * Dpad = C
     * LT = 0
     * RT = 1
     * LB (double width) = 2
     * RB (double width) = 3
     * back = 4
     * start = 5
     * 
     * LS = 6
     * RS = 7
     * Left = 8
     * Right = 9
     * Up = <
     * Down = >
     * Low = M
     * Medium = N
     * Full = O
     * Warning = !
     * 
     * No = ,
     * Yes = .
     * Volume = V
     * Mute = W
     * Play = s (lowercase)
     * Gs = P
     * Ds = Q
     * Gt = S
     * Dt = T
     * Sx = E
     * Dx = F
     * GSt = G
     * GDt = H
     * Is = I
     * Ds = J
     * It = K
     * Dt = Z
     */
}
