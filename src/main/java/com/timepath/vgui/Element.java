package com.timepath.vgui;

import com.timepath.io.utils.ViewableData;
import com.timepath.steam.io.VDFNode;
import java.awt.Color;
import java.awt.Font;
import java.awt.Image;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.UIManager;

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
 *
 * @author TimePath
 */
public class Element implements ViewableData {

    private static final Logger LOG = Logger.getLogger(Element.class.getName());

    public void addNode(Element e) {
        vdf.addNode(e.vdf);
    }

    public Element(InputStream is, Charset c) throws IOException {
        vdf = new VDFNode(is, c);
        parseScheme(vdf);
    }

    /**
     * TODO
     */
    private static void parseScheme(VDFNode props) {
        VDFNode root = props.get("Scheme", "Fonts");
        if (root == null) return;

        LOG.info("Found scheme");
        for (VDFNode fontNode : root.getNodes()) {
            for (VDFNode detailNode : fontNode.getNodes()) {
                String fontKey = String.valueOf(fontNode.getCustom());
                String fontName = String.valueOf(detailNode.getValue("name"));
                fonts.put(fontName, new HudFont(fontName, detailNode));
                LOG.log(Level.INFO, "TODO: Load font {0}", fontName);
                break;// XXX: hardcoded detail level (the first one)
            }

            LOG.info("Loaded scheme");
        }

    }

    protected Element() {
    }

    private Element(String name) {
        this.name = name;
    }

    private Element(String name, String info) {
        this.name = name;
        this.info = info;
    }

    public static Element importVdf(VDFNode vdf) {
        Element e = new Element();
        e.setVdf(vdf);
        e.setProperties(vdf.getProperties());
        //, file: vdf.file)
        e.load();
        return e;
    }

    private String trim(String s) {
        if (s != null && s.contains("\"")) {// assumes one set of quotes
            s = s.substring(1, (int) s.length() - 1);
            s = s.replaceAll("\"", "").trim();
        }

        return s;
    }

    private Integer parseInt(String v) {
        Integer vint = null;
        try {
            vint = Integer.parseInt(v);
        } catch (NumberFormatException ignored) {
        }

        if (vint == null) try {
            vint = (int) Float.parseFloat(v);
        } catch (NumberFormatException ignored) {
        }

        return vint;
    }

    public void load() {
        for (VDFNode.VDFProperty entry : properties) {
            String k = trim(entry.getKey());
            String v = trim(String.valueOf(entry.getValue()));
            Integer vint = parseInt(v);
            boolean vbool = vint != null && vint == 1;
            final String switchArg = k.toLowerCase();
            if ("enabled".equals(switchArg)) {
                if (vbool) enabled = vbool;
            } else if ("visible".equals(switchArg)) {
                if (vbool) enabled = vbool;
            } else if ("xpos".equals(switchArg)) {
                if (v.startsWith("c")) {
                    XAlignment = Alignment.Center;
                    v = v.substring(1);
                } else if (v.startsWith("r")) {
                    XAlignment = Alignment.Right;
                    v = v.substring(1);
                } else {
                    XAlignment = Alignment.Left;
                }

                vint = parseInt(v);
                if (vint != null)
                    localX = vint;
            } else if ("ypos".equals(switchArg)) {
                if (v.startsWith("c")) {
                    YAlignment = VAlignment.Center;
                    v = v.substring(1);
                } else if (v.startsWith("r")) {
                    YAlignment = VAlignment.Bottom;
                    v = v.substring(1);
                } else {
                    YAlignment = VAlignment.Top;
                }
                vint = parseInt(v);
                if (vint != null)
                    localY = vint;
            } else if ("zpos".equals(switchArg)) {
                if (vint != null) layer = vint;
            } else if ("wide".equals(switchArg)) {
                if (v.startsWith("f")) {
                    v = v.substring(1);
                    wideMode = DimensionMode.Mode2;
                }
                vint = parseInt(v);
                if (vint != null)
                    wide = vint;
            } else if ("tall".equals(switchArg)) {
                if (v.startsWith("f")) {
                    v = v.substring(1);
                    tallMode = DimensionMode.Mode2;
                }
                vint = parseInt(v);
                if (vint != null)
                    tall = vint;
            } else if ("labeltext".equals(switchArg)) {
                labelText = v;
            } else if ("textalignment".equals(switchArg)) {
                if ("center".equalsIgnoreCase(v)) {
                    textAlignment = Alignment.Center;
                } else {
                    textAlignment = "right".equalsIgnoreCase(v) ? Alignment.Right : Alignment.Left;
                }

            } else if ("controlname".equals(switchArg)) {
                controlName = v;
            } else if ("fgcolor".equals(switchArg)) {
                String[] c = v.split(" ");
                try {
                    fgColor = new Color(Integer.parseInt(c[0]), Integer.parseInt(c[1]), Integer.parseInt(c[2]), Integer.parseInt(c[3]));
                } catch (NumberFormatException ignored) {
                    // It's a variable
                }

            } else if ("font".equals(switchArg)) {
                if (!fonts.containsKey(v)) continue;
                Font f = fonts.get(v).getFont();
                if (f != null) font = f;
            } else if ("image".equals(switchArg) || "icon".equals(switchArg)) {
                image = VGUIRenderer.locateImage(v);
            } else {
                LOG.log(Level.WARNING, "Unknown property: {0}", k);
            }
        }

        if (controlName != null) {
            Class<? extends Control> control = (Class<? extends Control>) Controls.getDefaults().get(controlName);
            if (control == null)
                LOG.log(Level.WARNING, "Unknown control: {0}", controlName);
        } else {
            if ("hudlayout".equalsIgnoreCase(file)) {
                areas.put(name, this);
            }

        }

    }

    public List<VDFNode.VDFProperty> getProps() {
        return properties;
    }

    public String getControlName() {
        return controlName;
    }

    public void setControlName(String controlName) {
        this.controlName = controlName;
    }

    public String save() {
        StringBuilder sb = new StringBuilder();
        // preceding header
        for (VDFNode.VDFProperty p : properties) {
            if (String.valueOf(p.getValue()).isEmpty()) {
                if ("\\n".equals(p.getKey())) {
                    sb.append("\n");
                }

                if ("//".equals(p.getKey())) {
                    sb.append("//").append(p.getInfo()).append("\n");
                }

            }

        }

        sb.append(name).append("\n");
        sb.append("{\n");
        for (VDFNode.VDFProperty p : properties) {
            if (!String.valueOf(p.getValue()).isEmpty()) {
                sb.append("\\n".equals(p.getKey()) ?
                        "\t    \n" :
                        "\t    " + p.getKey() + "\t    " + p.getValue() + ((' ' + p.getInfo())) + "\n");
            }

        }

        sb.append("}\n");
        return sb.toString();
    }

    public int getSize() {
        return wide * tall;
    }

    @Override
    public String toString() {
        return name + ((info != null) ? (" ~ " + info) : ""); // elements cannot have a value, only info
    }

    public void validateDisplay() {
        for (VDFNode.VDFProperty entry : properties) {
            String k = entry.getKey();
            if (k == null) continue;
            try {
                if ("enabled".equalsIgnoreCase(k)) {
                    entry.setValue(enabled ? 1 : 0);
                } else if ("visible".equalsIgnoreCase(k)) {
                    entry.setValue(visible ? 1 : 0);
                } else if ("xpos".equalsIgnoreCase(k)) {
                    Alignment e = XAlignment;
                    entry.setValue(e.name().substring(0, 1).toLowerCase().replaceFirst("l", "") + getLocalX());
                } else if ("ypos".equalsIgnoreCase(k)) {
                    Alignment e = Alignment.values()[YAlignment.ordinal()];
                    entry.setValue(e.name().substring(0, 1).toLowerCase().replaceFirst("l", "") + getLocalY());
                } else if ("zpos".equalsIgnoreCase(k)) {
                    entry.setValue(layer);
                } else if ("wide".equalsIgnoreCase(k)) {
                    entry.setValue(((wideMode.equals(DimensionMode.Mode2)) ? "f" : "") + wide);
                } else if ("tall".equalsIgnoreCase(k)) {
                    entry.setValue(((tallMode.equals(DimensionMode.Mode2)) ? "f" : "") + tall);
                } else if ("labelText".equalsIgnoreCase(k)) {
                    entry.setValue(labelText);
                } else if ("ControlName".equalsIgnoreCase(k)) {
                    entry.setValue(controlName);
                }

                //            else if("font".equalsIgnoreCase(k)) {
                //                entry.setValue(this.getFont()) // TODO
                //            }
            } catch (PropertyVetoException e) {
                LOG.log(Level.SEVERE, null, e);
            }

        }

    }

    public int getLocalXi() {
        return (int) Math.round(localX);
    }

    public int getLocalYi() {
        return (int) Math.round(localY);
    }

    @Override
    public Icon getIcon() {
        return UIManager.getIcon("FileChooser.listViewIcon");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Element getParent() {
        return parent;
    }

    public void setParent(Element parent) {
        this.parent = parent;
    }

    public double getLocalX() {
        return localX;
    }

    public void setLocalX(double localX) {
        this.localX = localX;
    }

    public double getLocalY() {
        return localY;
    }

    public void setLocalY(double localY) {
        this.localY = localY;
    }

    public int getLayer() {
        return layer;
    }

    public void setLayer(int layer) {
        this.layer = layer;
    }

    public int getWide() {
        return wide;
    }

    public void setWide(int wide) {
        this.wide = wide;
    }

    public DimensionMode getWideMode() {
        return wideMode;
    }

    public void setWideMode(DimensionMode wideMode) {
        this.wideMode = wideMode;
    }

    public int getTall() {
        return tall;
    }

    public void setTall(int tall) {
        this.tall = tall;
    }

    public DimensionMode getTallMode() {
        return tallMode;
    }

    public void setTallMode(DimensionMode tallMode) {
        this.tallMode = tallMode;
    }

    public boolean getVisible() {
        return visible;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean getEnabled() {
        return enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public Color getFgColor() {
        return fgColor;
    }

    public void setFgColor(Color fgColor) {
        this.fgColor = fgColor;
    }

    public List<VDFNode.VDFProperty> getProperties() {
        return properties;
    }

    public void setProperties(List<VDFNode.VDFProperty> properties) {
        this.properties = properties;
    }

    public Alignment getXAlignment() {
        return XAlignment;
    }

    public void setXAlignment(Alignment XAlignment) {
        this.XAlignment = XAlignment;
    }

    public VAlignment getYAlignment() {
        return YAlignment;
    }

    public void setYAlignment(VAlignment YAlignment) {
        this.YAlignment = YAlignment;
    }

    public String getLabelText() {
        return labelText;
    }

    public void setLabelText(String labelText) {
        this.labelText = labelText;
    }

    public Alignment getTextAlignment() {
        return textAlignment;
    }

    public void setTextAlignment(Alignment textAlignment) {
        this.textAlignment = textAlignment;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public VDFNode getVdf() {
        return vdf;
    }

    public void setVdf(VDFNode vdf) {
        this.vdf = vdf;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    public static final Map<String, HudFont> fonts = new LinkedHashMap<String, HudFont>();
    public static final Map<String, Element> areas = new LinkedHashMap<String, Element>();
    private String name;
    private String info;
    private Element parent;
    private double localX;
    private double localY;
    /**
     * > 0 = out of screen
     */
    private int layer;
    private int wide;
    private DimensionMode wideMode = DimensionMode.Mode1;
    private int tall;
    private DimensionMode tallMode = DimensionMode.Mode1;
    private boolean visible;
    private boolean enabled;
    private Font font;
    private Color fgColor;
    private List<VDFNode.VDFProperty> properties = new LinkedList<VDFNode.VDFProperty>();
    private String controlName;
    private Alignment XAlignment = Alignment.Left;
    private VAlignment YAlignment = VAlignment.Top;
    private String labelText;
    private Alignment textAlignment = Alignment.Left;
    private Image image;
    private VDFNode vdf;
    private String file;

    public static enum Alignment {
        Left, Center, Right;
    }

    public static enum VAlignment {
        Top, Center, Bottom;
    }

    public static enum DimensionMode {
        Mode1, Mode2;
    }
}
