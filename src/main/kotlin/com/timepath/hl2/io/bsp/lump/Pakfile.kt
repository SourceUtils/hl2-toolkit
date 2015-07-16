package com.timepath.hl2.io.bsp.lump

import com.timepath.Logger
import com.timepath.hl2.io.bsp.Lump
import com.timepath.hl2.io.bsp.LumpHandler
import com.timepath.io.OrderedInputStream
import com.timepath.vfs.provider.zip.ZipFileProvider

class Pakfile {
    class Handler : LumpHandler<ZipFileProvider> {
        override fun invoke(l: Lump, ois: OrderedInputStream): ZipFileProvider {
            LOG.info({ "Unzipping ${l}" })
            val data = ByteArray(l.length)
            ois.readFully(data)
            return ZipFileProvider(data)
        }

        companion object {
            private val LOG = Logger()
        }
    }
}
