package com.timepath.vgui

import com.timepath.hl2.io.image.VTF
import com.timepath.io.utils.ViewableData
import com.timepath.steam.io.VDFNode
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.util.logging.Log

import javax.swing.*
import java.awt.*
import java.beans.PropertyVetoException
import java.nio.charset.Charset
import java.util.List
import java.util.logging.Level

import static com.timepath.steam.io.VDFNode.VDFProperty
/**
 * @author TimePath
 */
@CompileStatic
@TypeChecked
@Log('LOG')
public class Element implements ViewableData {

    public static final Map<String, HudFont> fonts = [:]
    public static final Map<String, Element> areas = [:]
    private final Dimension screen = [640, 480]
    private final Dimension internal = [640, 480]
    private final double scale = 1
    String name
    private String info
    Element parent

    // Properties
    double localX
    double localY
    /** > 0 = out of screen */
    int layer
    int wide
    private DimensionMode _wideMode = DimensionMode.Mode1
    int tall
    private DimensionMode _tallMode = DimensionMode.Mode1
    boolean visible
    boolean enabled
    Font font
    Color fgColor
    List<VDFProperty> properties = new LinkedList<>()
    String controlName
    Alignment XAlignment = Alignment.Left
    VAlignment YAlignment = VAlignment.Top
    String labelText
    Alignment textAlignment = Alignment.Left
    Image image

    @Delegate(excludes = ['addNode'])
    VDFNode vdf

    void addNode(Element e) {
        vdf.addNode(e.vdf)
    }

    static enum Alignment {
        Left, Center, Right
    }

    static enum VAlignment {
        Top, Center, Bottom
    }

    static enum DimensionMode {
        Mode1, Mode2
    }

    public Element(InputStream is, Charset c) throws IOException {
        vdf = new VDFNode(is, c)
        parseScheme(vdf)
    }

    /**
     * TODO
     *
     * @param props
     */
    private static void parseScheme(VDFNode props) {
        VDFNode root = props.get("Scheme", "Fonts")
        if (root == null) return
        LOG.info("Found scheme")
        for (VDFNode fontNode : root.nodes) {
            for (VDFNode detailNode : fontNode.nodes) {
                String fontKey = String.valueOf(fontNode.custom)
                String fontName = String.valueOf(detailNode.getValue("name"))
                fonts.put(fontName, new HudFont(fontName, detailNode))
                LOG.log(Level.INFO, "TODO: Load font {0}", fontName)
                break // XXX: hardcoded detail level (the first one)
            }
            LOG.info("Loaded scheme")
        }
    }

    String getFile() { "" } // TODO

    private Element() {
    }

    private Element(String name) {
        this.name = name
    }

    private Element(String name, String info) {
        this.name = name
        this.info = info
    }

    static Element importVdf(VDFNode vdf) {
        Element e = new Element(properties: vdf.properties)//, file: vdf.file)
        e.load()
        return e
    }

    private String trim(String s) {
        if (s != null && s.contains("\"")) { // assumes one set of quotes
            s = s.substring(1, s.length() - 1)
            s = s.replaceAll("\"", "").trim()
        }
        return s;
    }

    // TODO: remove duplicate keys (only keep the latest, or highlight duplicates)
    void load() {
        for (entry in properties) {
            def k = trim(entry.key)
            def v = trim(String.valueOf(entry.value))
            Integer vint = null;
            try {
                vint = Integer.parseInt(v);
            } catch (NumberFormatException ignored) {
            }
            boolean vbool = vint == 1;
            switch (k.toLowerCase()) {
                case "enabled": enabled = vbool; break
                case "visible": enabled = vbool; break
                case "xpos":
                    if (v.startsWith("c")) {
                        XAlignment = Alignment.Center
                        v = v.substring(1)
                    } else if (v.startsWith("r")) {
                        XAlignment = Alignment.Right
                        v = v.substring(1)
                    } else {
                        XAlignment = Alignment.Left
                    }
                    localX = vint
                    break
                case "ypos":
                    if (v.startsWith("c")) {
                        YAlignment = VAlignment.Center
                        v = v.substring(1)
                    } else if (v.startsWith("r")) {
                        YAlignment = VAlignment.Bottom
                        v = v.substring(1)
                    } else {
                        YAlignment = VAlignment.Top
                    }
                    localY = vint
                    break
                case "zpos": layer = vint; break
                case "wide":
                    if (v.startsWith("f")) {
                        v = v.substring(1)
                        _wideMode = DimensionMode.Mode2
                    }
                    wide = vint
                    break
                case "tall":
                    if (v.startsWith("f")) {
                        v = v.substring(1)
                        _tallMode = DimensionMode.Mode2
                    }
                    tall = vint
                    break
                case "labeltext": labelText = v; break
                case "textalignment":
                    if ("center".equalsIgnoreCase(v)) {
                        textAlignment = Alignment.Center
                    } else {
                        textAlignment = "right".equalsIgnoreCase(v) ? Alignment.Right : Alignment.Left
                    }
                    break
                case "controlname": controlName = v; break // Others are areas
                case "fgcolor":
                    String[] c = v.split(" ")
                    fgColor = new Color(Integer.parseInt(c[0]),
                            Integer.parseInt(c[1]),
                            Integer.parseInt(c[2]),
                            Integer.parseInt(c[3]))
                    break
                case "font":
                    if (!fonts.containsKey(v)) {
                        continue
                    }
                    Font f = fonts[v].font
                    if (f) font = f
                    break
                case "image":
                case "icon":
                    v = v.replaceAll("\"", "")
                    if ((v != null) && v.empty) {
                        continue
                    }
                    try {
                        VTF vtf = VTF.load(v + ".vtf")
                        if (vtf == null) {
                            continue
                        }
                        Image img = vtf.getImage(0)
                        if (img == null) {
                            continue
                        }
                        image = img
                    } catch (IOException ex) {
                        LOG.log(Level.SEVERE, null, ex)
                    }
                    break
                default: LOG.log(Level.WARNING, "Unknown property: {0}", k); break
            }
        }
        if (controlName != null) {
            def control = Controls.defaults[controlName];
            if (!control) LOG.log(Level.WARNING, "Unknown control: {0}", controlName)
        } else {
            if ("hudlayout".equalsIgnoreCase(file)) {
                areas.put(name, this)
            }
        }
    }

    List<VDFProperty> getProps() {
        return properties
    }

    String getControlName() {
        return controlName
    }

    void setControlName(String controlName) {
        this.controlName = controlName
    }

    String save() {
        StringBuilder sb = new StringBuilder()
        // preceding header
        for (VDFProperty p : properties) {
            if (String.valueOf(p.value).empty) {
                if ("\\n".equals(p.key)) {
                    sb.append('\n')
                }
                if ("//".equals(p.key)) {
                    sb.append("//").append(p.info).append('\n')
                }
            }
        }
        sb.append(name).append('\n')
        sb.append("{\n")
        for (VDFProperty p : properties) {
            if (!String.valueOf(p.value).empty) {
                sb.append("\\n".equals(p.key) ?
                        "\t    \n" :
                        "\t    ${p.key}\t    ${p.value}${(p.info != null) ? (' ' + p.info) : ""}\n")
            }
        }
        sb.append("}\n")
        return sb.toString()
    }

    // Extra stuff
    int getSize() { // works well unless they are exactly the same size
        return wide * tall
    }

    Rectangle getBounds() {
        int minX = (int) Math.round(x * (screen.width / internal.width) * scale)
        int minY = (int) Math.round(y * (screen.height / internal.height) * scale)
        int maxX = (int) Math.round(width * (screen.width / internal.width) * scale)
        int maxY = (int) Math.round(height * (screen.height / internal.height) * scale)
        return [minX, minY, maxX + 1, maxY + 1] as Rectangle
    }

    @Override
    String toString() {
        name + (info != null) ? (" ~ " + info) : "" // elements cannot have a value, only info
    }

    int getX() {
        if ((parent == null) || parent.name.replaceAll("\"", "").endsWith(".res")) {
            if (XAlignment == Alignment.Center) {
                return localX + (internal.width / 2)
            } else {
                return (XAlignment == Alignment.Right) ? (internal.width - localX) : localX
            }
        } else {
            int x
            if (XAlignment == Alignment.Center) {
                x = (parent.width / 2i + localX) as int
            } else {
                x = (XAlignment == Alignment.Right ? (parent.width - localX) : localX) as int
            }
            return x + parent.x
        }
    }

    int getY() {
        if ((parent == null) || parent.name.replaceAll("\"", "").endsWith(".res")) {
            if (YAlignment == Alignment.Center) {
                return localY + (internal.height / 2)
            } else {
                return (YAlignment == Alignment.Right) ? (internal.height - localY) : localY
            }
        }
        int y
        if (YAlignment == Alignment.Center) {
            y = (parent.height / 2i + localY) as int
        } else {
            y = ((YAlignment == Alignment.Right) ? (parent.height - localY) : localY) as int
        }
        return y + parent.y
    }

    int getWidth() {
        return (_wideMode == DimensionMode.Mode2) ? (internal.width - wide) : wide
    }

    int getHeight() {
        return (_tallMode == DimensionMode.Mode2) ? ((parent != null)
                ? (parent.height - tall)
                : (internal.height - tall)) : tall
    }

    // TODO: remove duplicate keys (only keep the latest, or highlight duplicates)
    void validateDisplay() {
        for (VDFProperty entry : properties) {
            String k = entry.key
            if (k == null) {
                continue
            }
            try {
                if ("enabled".equalsIgnoreCase(k)) {
                    entry.value = enabled ? 1 : 0
                } else if ("visible".equalsIgnoreCase(k)) {
                    entry.value = visible ? 1 : 0
                } else if ("xpos".equalsIgnoreCase(k)) {
                    entry.value = XAlignment.name().substring(0, 1).toLowerCase().replaceFirst("l", "") + getLocalX()
                } else if ("ypos".equalsIgnoreCase(k)) {
                    entry.value = YAlignment.name().substring(0, 1).toLowerCase().replaceFirst("l", "") + getLocalY()
                } else if ("zpos".equalsIgnoreCase(k)) {
                    entry.value = layer
                } else if ("wide".equalsIgnoreCase(k)) {
                    entry.value = ((_wideMode == DimensionMode.Mode2) ? "f" : "") + wide
                } else if ("tall".equalsIgnoreCase(k)) {
                    entry.value = ((_tallMode == DimensionMode.Mode2) ? "f" : "") + tall
                } else if ("labelText".equalsIgnoreCase(k)) {
                    entry.value = labelText
                } else if ("ControlName".equalsIgnoreCase(k)) {
                    entry.value = controlName
                }
                //            else if("font".equalsIgnoreCase(k)) {
                //                entry.setValue(this.getFont()) // TODO
                //            }
            } catch (PropertyVetoException e) {
                LOG.log(Level.SEVERE, null, e)
            }
        }
    }

    int getLocalX() { Math.round(localX) as int }

    int getLocalY() { Math.round(localY) as int }

    @Override
    Icon getIcon() { UIManager.getIcon "FileChooser.listViewIcon" }

}
