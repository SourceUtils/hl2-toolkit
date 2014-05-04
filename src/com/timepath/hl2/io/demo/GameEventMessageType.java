package com.timepath.hl2.io.demo;

import com.timepath.io.BitBuffer;

/**
 *
 * @author TimePath
 */
public enum GameEventMessageType {

    /**
     * A zero terminated string
     */
    STRING(1) {

            @Override
            public Object parse(BitBuffer bb) {
                return bb.getString();
            }

        },
    /**
     * Float, 32 bit
     */
    FLOAT(2) {

            @Override
            public Object parse(BitBuffer bb) {
                return bb.getFloat();
            }

        },
    /**
     * Signed int, 32 bit
     */
    LONG(3) {

            @Override
            public Object parse(BitBuffer bb) {
                return bb.getInt();
            }

        },
    /**
     * Signed int, 16 bit
     */
    SHORT(4) {

            @Override
            public Object parse(BitBuffer bb) {
                return bb.getShort();
            }

        },
    /**
     * Unsigned int, 8 bit
     */
    BYTE(5) {

            @Override
            public Object parse(BitBuffer bb) {
                return bb.getByte();
            }

        },
    /**
     * Unsigned int, 1 bit
     */
    BOOL(6) {

            @Override
            public Object parse(BitBuffer bb) {
                return bb.getBoolean();
            }

        },
    /**
     * Any data, but not networked to clients
     */
    LOCAL(7);

    GameEventMessageType(int i) {

    }

    static GameEventMessageType get(int i) {
        GameEventMessageType[] vals = values();
        if(i < 1 || i > vals.length) {
            return null;
        }
        return vals[i - 1];
    }

    public Object parse(BitBuffer bb) {
        return null;
    }

}
