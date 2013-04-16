package com.timepath;

/**
 *
 * @param <C> Enum class
 *
 * @author timepath
 */
public interface EnumFlags<C extends Enum<C> & EnumFlags<C>> {

    public int getId();
}