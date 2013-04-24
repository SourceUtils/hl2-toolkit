package com.timepath.steam.net;

/**
 *
 * @author timepath
 */
public enum Region {
    
    ALL((byte) 255),
        US_EAST((byte) 0),
        US_WEST((byte) 1),
        SOUTH_AMERICA((byte) 2),
        EUROPE((byte) 3),
        ASIA((byte) 4),
        AUSTRALIA((byte) 5),
        MIDDLE_EAST((byte) 6),
        AFRICA((byte) 7);

        private Region(byte code) {
            this.code = code;
        }

        private byte code;

        public byte getCode() {
            return code;
        }
    
}
