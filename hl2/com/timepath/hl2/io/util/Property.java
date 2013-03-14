package com.timepath.hl2.io.util;

import java.util.logging.Logger;

/**
 *
 * aka KeyValue
 *
 * @author timepath
 */
public class Property {

    private String key;

    private String value;

    private String info;

    public Property() {
    }

    public Property(String key, String value, String info) {
        this.key = key;
        this.value = value;
        this.info = info;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value.toString();
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    @Override
    public String toString() {
        return key + ":" + value + ":" + info;
    }

    private static final Logger LOG = Logger.getLogger(Property.class.getName());
}