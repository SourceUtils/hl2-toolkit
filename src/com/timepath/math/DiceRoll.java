package com.timepath.math;

import java.util.Random;

/**
 *
 * @author timepath
 */
public class DiceRoll {

    private static final Random r = new Random(0);

    public static int roll(int d) {
        return r.nextInt(d);
    }

}
