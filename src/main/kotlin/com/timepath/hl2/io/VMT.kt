package com.timepath.hl2.io

import com.timepath.hl2.io.image.VTF
import com.timepath.steam.io.VDFNode

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author TimePath
 */
public class VMT {

    public class VMTNode [throws(javaClass<IOException>())]
    (`is`: InputStream, c: Charset) : VDFNode() {
        public val root: VDFNode

        {
            VDFNode(`is`, c, this)
            root = getNodes()[0]
            LOG.log(Level.INFO, "Shader: {0}", root.getCustom())
        }

        throws(javaClass<IOException>())
        public val texture: VTF?
            get() = VTF.load(root.getValue("\$basetexture") as String)

        class object {

            private val LOG = Logger.getLogger(javaClass<VMTNode>().getName())
        }
    }

    class object {

        throws(javaClass<IOException>())
        public fun load(f: File): VMTNode {
            return load(FileInputStream(f))
        }

        throws(javaClass<IOException>())
        public fun load(`is`: InputStream): VMTNode {
            return load(`is`, StandardCharsets.UTF_8)
        }

        throws(javaClass<IOException>())
        public fun load(`is`: InputStream, c: Charset): VMTNode {
            return VMTNode(`is`, c)
        }

        throws(javaClass<IOException>())
        public fun load(f: File, c: Charset): VMTNode {
            return load(FileInputStream(f), c)
        }
    }
}
