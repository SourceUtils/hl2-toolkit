package com.timepath.hl2.io.demo;

/**
 *
 * @author TimePath
 */
public enum GameEventMessageType {

    /**
     * A zero terminated string
     */
    STRING(1),
    /**
     * Float, 32 bit
     */
    FLOAT(2),
    /**
     * Signed int, 32 bit
     */
    LONG(3),
    /**
     * Signed int, 16 bit
     */
    SHORT(4),
    /**
     * Unsigned int, 8 bit
     */
    BYTE(5),
    /**
     * Unsigned int, 1 bit
     */
    BOOL(6),
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

}
