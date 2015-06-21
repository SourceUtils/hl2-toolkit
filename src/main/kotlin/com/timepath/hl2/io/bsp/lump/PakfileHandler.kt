package com.timepath.hl2.io.bsp.lump

import com.timepath.Logger
import com.timepath.hl2.io.bsp.Lump
import com.timepath.hl2.io.bsp.LumpHandler
import com.timepath.io.OrderedInputStream
import com.timepath.vfs.provider.zip.ZipFileProvider
import java.io.IOException

class PakfileHandler : LumpHandler<ZipFileProvider> {

    throws(IOException::class)
    override fun handle(l: Lump, `in`: OrderedInputStream): ZipFileProvider {
        LOG.info({ "Unzipping ${l}" })
        val data = ByteArray(l.length)
        `in`.readFully(data)
        return ZipFileProvider(data)
    }

    companion object {

        private val LOG = Logger()
    }
}
