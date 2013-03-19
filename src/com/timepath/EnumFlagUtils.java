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
    public static <E extends Enum<E> & EnumFlags<E>> EnumSet<E> decode(int encoded, Class<E> enumClass) {
        E[] map = enumClass/*.getDeclaringClass()*/.getEnumConstants();
        Arrays.sort(map, new Comparator() {

            public int compare(Object e1, Object e2) {
                // Mixed bits at the top, single bits at the bottom, in order of size
                int i1 = ((E) e1).getId();
                int i2 = ((E) e2).getId();
                
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
//                System.out.println(map[i] + " - " + encoded);
                return EnumSet.of(map[i]);
            }
        }
        
        EnumSet ret = EnumSet.noneOf(enumClass);
        for(int i = 0; i < map.length; i++) {
            if(map[i].getId() == 0) {
                continue;
            }
            if((encoded & map[i].getId()) == map[i].getId()) {
                ret.add(map[i]);
            }
        }
//        System.err.println(ret + " - " + encoded);
        return ret;
    }
    
    public static <E extends Enum<E>> int encode(EnumSet<E> set) {
        int ret = 0;

        for(E val : set) {
            ret |= (1 << val.ordinal());
        }

        return ret;
    }
    
    private static final Logger LOG = Logger.getLogger(EnumFlagUtils.class.getName());

    private EnumFlagUtils() {
    }

}