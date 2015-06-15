package com.timepath.vgui

import com.timepath.steam.io.VDFNode
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author TimePath
 */
public class HudFont(private val fontname: String, node: VDFNode) {
    var name: String? = null
    var tall: Int = 0
    var aa: Boolean = false

    init {
        for (p in node.getProperties()) {
            val key = p.getKey().toLowerCase()
            val value = p.getValue().toString().toLowerCase()
            when (key) {
                "name" -> name = value
                "tall" -> tall = value.toInt()
                "antialias" -> aa = value.toInt() == 1
            }
        }
    }

    public val font: Font? get() {
        val screenRes = Toolkit.getDefaultToolkit().getScreenResolution()
        val fontSize = Math.round((tall * screenRes).toDouble() / 72.0).toInt()
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val fontFamilies = ge.getAvailableFontFamilyNames()
        if (name in fontFamilies) {
            // System font
            return Font(name, Font.PLAIN, fontSize)
        }
        try {
            LOG.log(Level.INFO, "Loading font: {0}... ({1})", arrayOf(fontname, name!!))
            fontFileForName(name!!)?.let {
                ge.registerFont(it)
                return Font(fontname, Font.PLAIN, fontSize)
            }
        } catch (ex: Exception) {
            LOG.log(Level.SEVERE, null, ex)
        }
        return null
    }

    companion object {

        private val LOG = Logger.getLogger(javaClass<HudFont>().getName())

        private fun fontFileForName(name: String): Font? {
            File("").listFiles { dir, name ->
                name.endsWith(".ttf") // XXX: hardcoded
            }?.forEach {
                val f = Font.createFont(Font.TRUETYPE_FONT, it)
                //            System.out.println(f.getFamily().toLowerCase());
                if (f.getFamily().equals(name, ignoreCase = true)) {
                    LOG.log(Level.INFO, "Found font for {0}", name)
                    return f
                }
            }
            return null
        }
    }
}
