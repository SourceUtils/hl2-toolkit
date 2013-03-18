package com.timepath;

/**
 *
 * @author timepath
 */
public interface EnumFlags<T extends Enum<T> & EnumFlags> {

    public int getId();
    
}
