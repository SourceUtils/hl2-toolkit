package com.timepath.hl2.io

import com.timepath.vgui.Element

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

public object RES {

    throws(IOException::class)
    public fun load(f: File): Element {
        return load(FileInputStream(f))
    }

    throws(IOException::class)
    public fun load(`is`: InputStream): Element {
        return load(`is`, StandardCharsets.UTF_8)
    }

    throws(IOException::class)
    public fun load(`is`: InputStream, c: Charset): Element {
        return Element(`is`, c)
    }

    throws(IOException::class)
    public fun load(f: File, c: Charset): Element {
        return load(FileInputStream(f), c)
    }
}
