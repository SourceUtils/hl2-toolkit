package com.timepath.hl2.io.bsp

import com.timepath.hl2.io.bsp.lump.LumpType
import com.timepath.vfs.SimpleVFile
import com.timepath.vfs.provider.ProviderPlugin
import com.timepath.vfs.provider.zip.ZipFileProvider
import org.kohsuke.MetaInfServices

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author TimePath
 */
MetaInfServices
public class BSPPlugin : ProviderPlugin {

    override fun register(): SimpleVFile.FileHandler {
        return object : SimpleVFile.FileHandler {
            throws(IOException::class)
            override fun handle(file: File): Collection<SimpleVFile>? {
                if (!file.getName().endsWith(".bsp")) return null
                val name = file.getName().replace(".bsp", "")
                return setOf(object : SimpleVFile() {
                    fun checkBSP() {
                        if (z != null) return
                        LOG.log(Level.INFO, "Loading {0}", file)
                        try {
                            FileInputStream(file).use { `is` ->
                                BSP.load(`is`)?.let {
                                    z = it.getLump<ZipFileProvider>(LumpType.LUMP_PAKFILE)
                                }
                            }
                        } catch (e: IOException) {
                            LOG.log(Level.SEVERE, null, e)
                        }

                    }

                    var z: ZipFileProvider? = null

                    override val isDirectory = true
                    override val name = name

                    override fun openStream(): InputStream? {
                        return null
                    }

                    override fun list(): Collection<SimpleVFile> {
                        checkBSP()
                        return z?.let { it.list() } ?: emptyList()
                    }

                    override fun get(name: String): SimpleVFile? {
                        checkBSP()
                        return z?.let { it[name] }
                    }
                })
            }
        }
    }

    companion object {

        private val LOG = Logger.getLogger(javaClass<BSPPlugin>().getName())
    }
}
