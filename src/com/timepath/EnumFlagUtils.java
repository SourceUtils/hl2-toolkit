package com.timepath;

import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class EnumFlagUtils {

    @SuppressWarnings("unchecked")
    public static <C extends Enum<C> & EnumFlags<C>> EnumSet<C> decode(int encoded, Class<C> enumClass) {
        C[] map = enumClass
                 /*.getDeclaringClass()*/
                 .getEnumConstants();
        Arrays.sort(map, new Comparator<C>() {
            public int compare(C e1, C e2) {
                // Mixed bits at the top, single bits at the bottom, in order of size
                int i1 = e1.getId();
                int i2 = e2.getId();

                int diff = Integer.bitCount(i2) - Integer.bitCount(i1);
                if(diff == 0) { // TODO: split into groups
                    return i2 - i1;
                } else {
                    return diff;
                }
            }
        });

        for(int i = 0; i < map.length; i++) {
            if(map[i].getId() == encoded) {
//                LOG.info(map[i] + " - " + encoded);
                return EnumSet.of(map[i]);
            }
        }

        EnumSet<C> ret = EnumSet.noneOf(enumClass);
        for(int i = 0; i < map.length; i++) {
            if(map[i].getId() == 0) {
                continue;
            }
            if((encoded & map[i].getId()) == map[i].getId()) {
                ret.add(map[i]);
            }
        }
//        LOG.info(ret + " - " + encoded);
        return ret;
    }

    public static <C extends Enum<C>> int encode(EnumSet<C> set) {
        int ret = 0;

        for(C val : set) {
            ret |= (1 << val.ordinal());
        }

        return ret;
    }

    private static final Logger LOG = Logger.getLogger(EnumFlagUtils.class.getName());

    private EnumFlagUtils() {
    }
}