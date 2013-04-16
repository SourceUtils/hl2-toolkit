package com.timepath.math;

import java.util.Random;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class DiceRoll {

    private static final Random r = new Random(0);

    public static int roll(int d) {
        return r.nextInt(d);
    }

    private DiceRoll() {
    }

    private static final Logger LOG = Logger.getLogger(DiceRoll.class.getName());

}
