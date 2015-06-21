package com.timepath.hl2.io

import com.timepath.Logger
import com.timepath.hl2.io.image.VTF
import com.timepath.steam.io.VDFNode
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

public object VMT {

    public class VMTNode @throws(IOException::class) constructor
    (`is`: InputStream, c: Charset) : VDFNode() {
        public val root: VDFNode

        init {
            VDFNode(`is`, c, this)
            root = getNodes()[0]
            LOG.info({ "Shader: ${root.getCustom()}" })
        }

        throws(IOException::class)
        public val texture: VTF?
            get() = VTF.load(root.getValue("\$basetexture") as String)

        companion object {

            private val LOG = Logger()
        }
    }

    throws(IOException::class)
    public fun load(f: File): VMTNode {
        return load(FileInputStream(f))
    }

    throws(IOException::class)
    public fun load(`is`: InputStream): VMTNode {
        return load(`is`, StandardCharsets.UTF_8)
    }

    throws(IOException::class)
    public fun load(`is`: InputStream, c: Charset): VMTNode {
        return VMTNode(`is`, c)
    }

    throws(IOException::class)
    public fun load(f: File, c: Charset): VMTNode {
        return load(FileInputStream(f), c)
    }
}
