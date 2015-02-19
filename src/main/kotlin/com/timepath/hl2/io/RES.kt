package com.timepath.hl2.io

import com.timepath.vgui.Element

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

/**
 * @author TimePath
 */
public class RES {
    class object {

        throws(javaClass<IOException>())
        public fun load(f: File): Element {
            return load(FileInputStream(f))
        }

        throws(javaClass<IOException>())
        public fun load(`is`: InputStream): Element {
            return load(`is`, StandardCharsets.UTF_8)
        }

        throws(javaClass<IOException>())
        public fun load(`is`: InputStream, c: Charset): Element {
            return Element(`is`, c)
        }

        throws(javaClass<IOException>())
        public fun load(f: File, c: Charset): Element {
            return load(FileInputStream(f), c)
        }
    }
}
