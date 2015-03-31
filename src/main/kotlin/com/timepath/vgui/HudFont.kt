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

fun HudFont(font: String, node: VDFNode): HudFont {
    val __ = HudFont(font)
    for (p in node.getProperties()) {
        val key = p.getKey().toLowerCase()
        val value = java.lang.String.valueOf(p.getValue()).toLowerCase()
        when (key) {
            "name" -> __.name = value
            "tall" -> __.tall = Integer.parseInt(value)
            "antialias" -> __.aa = Integer.parseInt(value) == 1
        }
    }
    return __
}

public class HudFont(private val fontname: String? = null) {
    var name: String? = null
    var tall: Int = 0
    var aa: Boolean = false

    public val font: Font? get() {
        val screenRes = Toolkit.getDefaultToolkit().getScreenResolution()
        val fontSize = Math.round((tall * screenRes).toDouble() / 72.0).toInt()
        val ge = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val fontFamilies = ge.getAvailableFontFamilyNames()
        if (name in fontFamilies) {
            // System font
            return Font(name, Font.PLAIN, fontSize)
        }
        var f1: Font? = null
        try {
            LOG.log(Level.INFO, "Loading font: {0}... ({1})", array<Any>(fontname!!, name!!))
            f1 = fontFileForName(name!!)
            if (f1 == null) {
                return null
            }
            ge.registerFont(f1!!) // For some reason, this works but the bottom return does not
            return Font(fontname, Font.PLAIN, fontSize)
        } catch (ex: Exception) {
            LOG.log(Level.SEVERE, null, ex)
        }

        if (f1 == null) {
            return null
        }
        LOG.log(Level.INFO, "Loaded {0}", fontname)
        return f1!!.deriveFont(fontSize.toFloat())
    }

    companion object {

        private val LOG = Logger.getLogger(javaClass<HudFont>().getName())

        throws(javaClass<Exception>())
        private fun fontFileForName(name: String): Font? {
            val files = File("").listFiles { dir, name -> name.endsWith(".ttf") } // XXX: hardcoded
            if (files != null) {
                for (file in files) {
                    val f = Font.createFont(Font.TRUETYPE_FONT, file)
                    //            System.out.println(f.getFamily().toLowerCase());
                    if (f.getFamily().toLowerCase() == name.toLowerCase()) {
                        LOG.log(Level.INFO, "Found font for {0}", name)
                        return f
                    }
                }
            }
            return null
        }
    }
}
