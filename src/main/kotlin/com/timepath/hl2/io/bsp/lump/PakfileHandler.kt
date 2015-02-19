package com.timepath.hl2.io.bsp.lump

import com.timepath.hl2.io.bsp.Lump
import com.timepath.hl2.io.bsp.LumpHandler
import com.timepath.io.OrderedInputStream
import com.timepath.vfs.provider.zip.ZipFileProvider

import java.io.IOException
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author TimePath
 */
class PakfileHandler : LumpHandler<ZipFileProvider> {

    throws(javaClass<IOException>())
    override fun handle(l: Lump, `in`: OrderedInputStream): ZipFileProvider {
        LOG.log(Level.INFO, "Unzipping {0}", array<Any>(l))
        val data = ByteArray(l.length)
        `in`.readFully(data)
        return ZipFileProvider(data)
    }

    class object {

        private val LOG = Logger.getLogger(javaClass<PakfileHandler>().getName())
    }
}
