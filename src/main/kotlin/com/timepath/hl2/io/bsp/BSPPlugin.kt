package com.timepath.hl2.io.bsp

import com.timepath.Logger
import com.timepath.hl2.io.bsp.lump.LumpType
import com.timepath.vfs.SimpleVFile
import com.timepath.vfs.provider.ProviderPlugin
import com.timepath.vfs.provider.zip.ZipFileProvider
import org.kohsuke.MetaInfServices
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.logging.Level
import kotlin.properties.Delegates

@MetaInfServices
public class BSPPlugin : ProviderPlugin {

    override fun register() = object : SimpleVFile.FileHandler {
        override fun handle(file: File): Collection<SimpleVFile>? {
            if (!file.getName().endsWith(".bsp")) return null
            val name = file.getName().replace(".bsp", "")
            return setOf(object : SimpleVFile() {
                val z: ZipFileProvider? by Delegates.lazy {
                    LOG.info { "Loading ${file}" }
                    try {
                        FileInputStream(file).use {
                            BSP.load(it)?.getLump<ZipFileProvider>(LumpType.LUMP_PAKFILE)
                        }
                    } catch (e: IOException) {
                        LOG.log(Level.SEVERE, { null }, e)
                        null
                    }
                }
                override val isDirectory = true
                override val name = name
                override fun openStream() = null
                override fun list() = z?.list() ?: emptyList()
                override fun get(name: String) = z?.get(name)
            })
        }
    }

    companion object {
        private val LOG = Logger()
    }
}
