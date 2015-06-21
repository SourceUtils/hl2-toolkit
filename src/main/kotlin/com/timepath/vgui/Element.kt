package com.timepath.vgui

import com.timepath.Logger
import com.timepath.io.utils.ViewableData
import com.timepath.steam.io.VDFNode
import java.awt.Color
import java.awt.Font
import java.awt.Image
import java.beans.PropertyVetoException
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.LinkedHashMap
import java.util.LinkedList
import java.util.logging.Level
import javax.swing.Icon
import javax.swing.UIManager

/**
 * Conditional handling:
 * <br/>
 * If there are multiple values with supported conditionals, the last one specified wins.
 * <br/>
 * Some tags:
 * <table>
 * <tr>
 * <th>Conditional</th>
 * <th>Meaning</th>
 * </tr>
 * <tr>
 * <td>$WIN32</td>
 * <td>Not a console</td>
 * </tr>
 * <tr>
 * <td>$WINDOWS</td>
 * <td>Windows</td>
 * </tr>
 * <tr>
 * <td>$POSIX</td>
 * <td>OSX or Linux</td>
 * </tr>
 * <tr>
 * <td>$OSX</td>
 * <td>Mac OSX</td>
 * </tr>
 * <tr>
 * <td>$LINUX</td>
 * <td>Linux</td>
 * </tr>
 * <tr>
 * <td>$X360</td>
 * <td>Xbox360</td>
 * </tr>
 * </table>
 */

throws(IOException::class)
public fun Element(`is`: InputStream, c: Charset): Element {
    val __ = Element(null)
    __.vdf = VDFNode(`is`, c)
    Element.parseScheme(__.vdf!!)
    return __
}

public fun Element(): Element {
    return Element(null)
}

private fun Element(name: String): Element {
    val __ = Element(null)
    __.name = name
    return __
}

private fun Element(name: String, info: String): Element {
    val __ = Element(info)
    __.name = name
    return __
}

public class Element(private val info: String?) : ViewableData {

    public fun addNode(e: Element) {
        vdf!!.addNode(e.vdf)
    }

    private fun trim(s: String?) = when {
        s != null && "\"" in s -> {
            var tmp: String = s
            // assumes one set of quotes
            tmp = tmp.substring(1, tmp.length() - 1)
            tmp = tmp.replace("\"", "")
            tmp.trim()
        }
        else -> s
    }

    private fun parseInt(v: String): Int? {
        var vint: Int? = null
        try {
            vint = Integer.parseInt(v)
        } catch (ignored: NumberFormatException) {
        }


        if (vint == null)
            try {
                vint = java.lang.Float.parseFloat(v).toInt()
            } catch (ignored: NumberFormatException) {
            }


        return vint
    }

    public fun load() {
        for (entry in props) {
            val k = trim(entry.getKey())
            var v = trim(java.lang.String.valueOf(entry.getValue()))!!
            var vint = parseInt(v)
            val vbool = vint != null && vint == 1
            val switchArg = k!!.toLowerCase()
            if ("enabled" == switchArg) {
                if (vbool) enabled = vbool
            } else if ("visible" == switchArg) {
                if (vbool) enabled = vbool
            } else if ("xpos" == switchArg) {
                if (v.startsWith("c")) {
                    XAlignment = Alignment.Center
                    v = v.substring(1)
                } else if (v.startsWith("r")) {
                    XAlignment = Alignment.Right
                    v = v.substring(1)
                } else {
                    XAlignment = Alignment.Left
                }

                vint = parseInt(v)
                if (vint != null)
                    localX = vint.toDouble()
            } else if ("ypos" == switchArg) {
                if (v.startsWith("c")) {
                    YAlignment = VAlignment.Center
                    v = v.substring(1)
                } else if (v.startsWith("r")) {
                    YAlignment = VAlignment.Bottom
                    v = v.substring(1)
                } else {
                    YAlignment = VAlignment.Top
                }
                vint = parseInt(v)
                if (vint != null)
                    localY = vint.toDouble()
            } else if ("zpos" == switchArg) {
                if (vint != null) layer = vint
            } else if ("wide" == switchArg) {
                if (v.startsWith("f")) {
                    v = v.substring(1)
                    wideMode = DimensionMode.Mode2
                }
                vint = parseInt(v)
                if (vint != null)
                    wide = vint
            } else if ("tall" == switchArg) {
                if (v.startsWith("f")) {
                    v = v.substring(1)
                    tallMode = DimensionMode.Mode2
                }
                vint = parseInt(v)
                if (vint != null)
                    tall = vint
            } else if ("labeltext" == switchArg) {
                labelText = v
            } else if ("textalignment" == switchArg) {
                if ("center".equals(v, ignoreCase = true)) {
                    textAlignment = Alignment.Center
                } else {
                    textAlignment = if ("right".equals(v, ignoreCase = true)) Alignment.Right else Alignment.Left
                }

            } else if ("controlname" == switchArg) {
                controlName = v
            } else if ("fgcolor" == switchArg) {
                val c = v.splitBy(" ")
                try {
                    fgColor = Color(Integer.parseInt(c[0]), Integer.parseInt(c[1]), Integer.parseInt(c[2]), Integer.parseInt(c[3]))
                } catch (ignored: NumberFormatException) {
                    // It's a variable
                }


            } else if ("font" == switchArg) {
                if (!fonts.containsKey(v)) continue
                val f = fonts[v]?.font
                if (f != null) font = f
            } else if ("image" == switchArg || "icon" == switchArg) {
                image = VGUIRenderer.locateImage(v)
            } else {
                LOG.log(Level.WARNING, { "Unknown property: ${k}" })
            }
        }

        if (controlName != null) {
            val control = Controls[controlName!!]
        } else {
            if ("hudlayout".equals(file ?: "", ignoreCase = true)) {
                areas.put(name!!, this)
            }
        }
    }

    public fun save(): String {
        val sb = StringBuilder()
        // preceding header
        for (p in props) {
            if (java.lang.String.valueOf(p.getValue()).isEmpty()) {
                if ("\\n" == p.getKey()) {
                    sb.append("\n")
                }

                if ("//" == p.getKey()) {
                    sb.append("//").append(p.info).append("\n")
                }

            }

        }

        sb.append(name).append("\n")
        sb.append("{\n")
        for (p in props) {
            if (!java.lang.String.valueOf(p.getValue()).isEmpty()) {
                sb.append(if ("\\n" == p.getKey())
                    "\t    \n"
                else {
                    "\t    ${p.getKey()}\t    ${p.getValue()}${' ' + p.info}\n"
                })
            }

        }

        sb.append("}\n")
        return sb.toString()
    }

    public val size: Int get() = wide * tall

    override fun toString(): String {
        return name + (when {
            info != null -> " ~ $info"
            else -> ""
        }) // elements cannot have a value, only info
    }

    public fun validateDisplay() {
        for (entry in props) {
            val k = entry.getKey()
            if (k == null) continue
            try {
                if ("enabled".equals(k, ignoreCase = true)) {
                    entry.setValue(if (enabled) 1 else 0)
                } else if ("visible".equals(k, ignoreCase = true)) {
                    entry.setValue(if (visible) 1 else 0)
                } else if ("xpos".equals(k, ignoreCase = true)) {
                    val e = XAlignment
                    entry.setValue("${e.name().substring(0, 1).toLowerCase().replaceFirstLiteral("l", "")}$localX")
                } else if ("ypos".equals(k, ignoreCase = true)) {
                    val e = Alignment.values()[YAlignment.ordinal()]
                    entry.setValue("${e.name().substring(0, 1).toLowerCase().replaceFirstLiteral("l", "")}$localY")
                } else if ("zpos".equals(k, ignoreCase = true)) {
                    entry.setValue(layer)
                } else if ("wide".equals(k, ignoreCase = true)) {
                    entry.setValue("${if ((wideMode == DimensionMode.Mode2)) "f" else ""}$wide")
                } else if ("tall".equals(k, ignoreCase = true)) {
                    entry.setValue("${if ((tallMode == DimensionMode.Mode2)) "f" else ""}$tall")
                } else if ("labelText".equals(k, ignoreCase = true)) {
                    entry.setValue(labelText)
                } else if ("ControlName".equals(k, ignoreCase = true)) {
                    entry.setValue(controlName)
                }

                //            else if("font".equalsIgnoreCase(k)) {
                //                entry.setValue(this.getFont()) // TODO
                //            }
            } catch (e: PropertyVetoException) {
                LOG.log(Level.SEVERE, { null }, e)
            }


        }

    }

    public val localXi: Int get() = Math.round(localX).toInt()

    public val localYi: Int get() = Math.round(localY).toInt()

    override fun getIcon(): Icon? {
        return UIManager.getIcon("FileChooser.listViewIcon")
    }

    public fun isVisible(): Boolean = visible

    public fun isEnabled(): Boolean = enabled

    public val properties: List<VDFNode.VDFProperty> get() = props

    public fun setProperties(properties: List<VDFNode.VDFProperty>) {
        this.props = properties
    }

    public var name: String? = null
    public var parent: Element? = null
    public var localX: Double = 0.toDouble()
    public var localY: Double = 0.toDouble()
    /**
     * > 0 = out of screen
     */
    public var layer: Int = 0
    public var wide: Int = 0
    public var wideMode: DimensionMode = DimensionMode.Mode1
    public var tall: Int = 0
    public var tallMode: DimensionMode = DimensionMode.Mode1
    public var visible: Boolean = false
    public var enabled: Boolean = false
    public var font: Font? = null
    public var fgColor: Color? = null
    public var props: List<VDFNode.VDFProperty> = LinkedList()
        private set
    public var controlName: String? = null
    public var XAlignment: Alignment = Alignment.Left
    public var YAlignment: VAlignment = VAlignment.Top
    public var labelText: String? = null
    public var textAlignment: Alignment = Alignment.Left
    public var image: Image? = null
    var vdf: VDFNode? = null
    public var file: String? = null

    public enum class Alignment {
        Left,
        Center,
        Right
    }

    public enum class VAlignment {
        Top,
        Center,
        Bottom
    }

    public enum class DimensionMode {
        Mode1,
        Mode2
    }

    companion object {

        private val LOG = Logger()

        /**
         * TODO
         */
        fun parseScheme(props: VDFNode) {
            val root = props["Scheme", "Fonts"] ?: return

            LOG.info { "Found scheme" }
            for (fontNode in root.getNodes()) {
                for (detailNode in fontNode.getNodes()) {
                    // val fontKey = fontNode.getCustom().toString()
                    val fontName = detailNode.getValue("name").toString()
                    fonts.put(fontName, HudFont(fontName, detailNode))
                    LOG.info({ "TODO: Load font ${fontName}" })
                    break// XXX: hardcoded detail level (the first one)
                }

                LOG.info { "Loaded scheme" }
            }

        }

        public fun importVdf(vdf: VDFNode): Element {
            val e = Element()
            e.vdf = vdf
            e.setProperties(vdf.getProperties())
            //, file: vdf.file)
            e.load()
            return e
        }

        public val fonts: MutableMap<String, HudFont> = LinkedHashMap()
        public val areas: MutableMap<String, Element> = LinkedHashMap()
    }
}
