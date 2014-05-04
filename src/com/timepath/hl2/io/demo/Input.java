package com.timepath.hl2.io.demo;

import java.util.LinkedList;
import java.util.List;

/**
 * https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/game/shared/in_buttons.h
 * <p/>
 * @author TimePath
 */
public enum Input {

    ATTACK(1),
    JUMP(1 << 1),
    DUCK(1 << 2),
    FORWARD(1 << 3),
    BACK(1 << 4),
    USE(1 << 5),
    CANCEL(1 << 6),
    LEFT(1 << 7),
    RIGHT(1 << 8),
    MOVELEFT(1 << 9),
    MOVERIGHT(1 << 10),
    ATTACK2(1 << 11),
    RUN(1 << 12),
    RELOAD(1 << 13),
    ALT1(1 << 14),
    ALT2(1 << 15),
    SCORE(1 << 16),
    SPEED(1 << 17),
    WALK(1 << 18),
    /**
     * Zoom key for HUD zoom
     */
    ZOOM(1 << 19),
    /**
     * Weapon defines these bits
     */
    WEAPON1(1 << 20),
    /**
     * Weapon defines these bits
     */
    WEAPON2(1 << 21),
    BULLRUSH(1 << 22),
    GRENADE1(1 << 23),
    GRENADE2(1 << 24),
    ATTACK3(1 << 25);
    
    public final int mask;
    
    Input(int mask) {
        this.mask = mask;
    }

    public static List<Input> get(int bits) {
        List<Input> l = new LinkedList<Input>();
        for(Input name : Input.values()) {
            if((name.mask & bits) != 0) {
                l.add(name);
            }
        }
        return l;
    }

}
