package com.timepath.hl2.io.util

import com.timepath.Diff
import com.timepath.Node as NodeStore
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
public class Element extends NodeStore<VDFProperty, Element> implements ViewableData {

    public static final Map<String, HudFont> fonts = [:]
    public static final Map<String, Element> areas = [:]
    private final Dimension screen = [640, 480]
    private final Dimension internal = [640, 480]
    private final double scale = 1
    String name
    private String info
    private Element parent
    // Properties
    private double localX
    private double localY
    // > 0 = out of screen
    int layer
    private int wide
    private DimensionMode _wideMode = DimensionMode.Mode1
    private int tall
    private DimensionMode _tallMode = DimensionMode.Mode1
    private boolean visible
    private boolean enabled
    private Font font
    private Color fgColor
    private List<VDFProperty> ps = new LinkedList<>()
    private String controlName
    Alignment XAlignment = Alignment.Left
    @SuppressWarnings("SuspiciousNameCombination")
    Alignment YAlignment = Alignment.Left
    String labelText
    Alignment textAlignment = Alignment.Left
    Image image
    VDFNode vdf

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

    public String getFile() { "" } // TODO

    private Element() {
    }

    private Element(String name) {
        this.name = name
    }

    private Element(String name, String info) {
        this.name = name
        this.info = info
    }

    public static Element importVdf(VDFNode vdf) {
        new Element().with {
            ps = vdf.properties
            //        setFile(vdf.getFile())
            validateLoad()
            return it
        }
    }

    // TODO: remove duplicate keys (only keep the latest, or highlight duplicates)
    public void validateLoad() {
        for (VDFProperty entry : ps) {
            String k = entry.key.toLowerCase()
            //            if(k != null && k.contains("\"")) { // assumes one set of quotes
            //                k = k.substring(1, k.length() - 1)
            //                k = k.replaceAll("\"", "").trim()
            //            }
            String v = String.valueOf(entry.value)
            //            if(v != null && v.contains("\"")) {
            //                v = v.substring(1, v.length() - 1)
            //                v = v.replaceAll("\"", "").trim()
            //            }
            //            String i = entry.getInfo()
            //            if(i != null && i.contains("\"")) {
            //                i = i.substring(1, i.length() - 1)
            //                i = i.replaceAll("\"", "").trim()
            //            }
            try {
                if ("enabled".equalsIgnoreCase(k)) {
                    enabled = Integer.parseInt(v) == 1
                } else if ("visible".equalsIgnoreCase(k)) {
                    visible = Integer.parseInt(v) == 1
                } else if ("xpos".equalsIgnoreCase(k)) {
                    if (v.startsWith("c")) {
                        XAlignment = Alignment.Center
                        v = v.substring(1)
                    } else if (v.startsWith("r")) {
                        XAlignment = Alignment.Right
                        v = v.substring(1)
                    } else {
                        XAlignment = Alignment.Left
                    }
                    localX = Integer.parseInt(v)
                } else if ("ypos".equalsIgnoreCase(k)) {
                    if (v.startsWith("c")) {
                        YAlignment = Alignment.Center
                        v = v.substring(1)
                    } else if (v.startsWith("r")) {
                        //noinspection SuspiciousNameCombination
                        YAlignment = Alignment.Right
                        v = v.substring(1)
                    } else {
                        //noinspection SuspiciousNameCombination
                        YAlignment = Alignment.Left
                    }
                    localY = Integer.parseInt(v)
                } else if ("zpos".equalsIgnoreCase(k)) {
                    layer = Integer.parseInt(v)
                } else if ("wide".equalsIgnoreCase(k)) {
                    if (v.startsWith("f")) {
                        v = v.substring(1)
                        _wideMode = DimensionMode.Mode2
                    }
                    wide = Integer.parseInt(v)
                } else if ("tall".equalsIgnoreCase(k)) {
                    if (v.startsWith("f")) {
                        v = v.substring(1)
                        _tallMode = DimensionMode.Mode2
                    }
                    tall = Integer.parseInt(v)
                } else if ("labelText".equalsIgnoreCase(k)) {
                    labelText = v
                } else if ("textAlignment".equalsIgnoreCase(k)) {
                    if ("center".equalsIgnoreCase(v)) {
                        textAlignment = Alignment.Center
                    } else {
                        textAlignment = "right".equalsIgnoreCase(v) ? Alignment.Right : Alignment.Left
                    }
                } else if ("ControlName".equalsIgnoreCase(k)) { // others are areas
                    controlName = v
                } else if ("fgcolor".equalsIgnoreCase(k)) {
                    String[] c = v.split(" ")
                    fgColor = new Color(Integer.parseInt(c[0]),
                            Integer.parseInt(c[1]),
                            Integer.parseInt(c[2]),
                            Integer.parseInt(c[3]))
                } else if ("font".equalsIgnoreCase(k)) {
                    if (!fonts.containsKey(v)) {
                        continue
                    }
                    HudFont a = fonts[v]
                    Font f = a.font
                    if (f != null) {
                        font = f
                    }
                } else if ("image".equalsIgnoreCase(k) || "icon".equalsIgnoreCase(k)) {
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
                } else {
                    LOG.log(Level.WARNING, "Unknown property: {0}", k)
                }
            } catch (NumberFormatException ignored) {
            }
        }
        if (controlName != null) { // temp
            switch (controlName) {
                case "CExLabel":
                    break
                case "CIconPanel":
                    break
                case "CTeamMenu":
                    break
                case "CTFClassInfoPanel":
                    break
                case "CTFArrowPanel":
                    break
                case "CTFImagePanel":
                    break
                case "CEmbeddedItemModelPanel":
                    break // ooh, fancy
                case "CTFHudTimeStatus":
                    break
                case "CWaveStatusPanel":
                    break // MvM?
                case "CControlPointCountdown":
                    break // control points
                case "CWaveCompleteSummaryPanel":
                    break // MvM?
                case "CTFTextWindow":
                    break // text motd?
                case "CTFRichText":
                    break
                case "CTankStatusPanel":
                    break
                case "CExImageButton":
                    break
                case "CEngyDestroyMenuItem":
                    break
                case "CCurrencyStatusPanel":
                    break
                case "CTFClassImage":
                    break
                case "CInWorldCurrencyStatus":
                    break // MvM in world currency
                case "CTFClientScoreBoardDialog":
                    break // scoreboard
                case "CTFHudEscort":
                    break // payload?
                case "CWarningSwoop":
                    break
                case "CItemModelPanel":
                    break
                case "CStoreItemControlsPanel":
                    break
                case "CExButton":
                    break
                case "CStorePreviewItemPanel":
                    break
                case "CStorePricePanel":
                    break
                case "CRichTextWithScrollbarBorders":
                    break
                case "CTFPlayerModelPanel":
                    break
                case "CArmoryPanel":
                    break
                case "CNotificationsPresentPanel":
                    break
                case "CEconItemDetailsRichText":
                    break
                case "CTFMapStampsInfoDialog":
                    break
                case "CStorePreviewItemIcon":
                    break
                case "CMouseMessageForwardingPanel":
                    break
                case "CGenericNotificationToast":
                    break
                case "CStorePreviewClassIcon":
                    break
                case "CNavigationPanel":
                    break
                case "CAvatarImagePanel":
                    break
                case "CNotificationQueuePanel":
                    break
                case "CPreviewRotButton":
                    break
                case "CNotificationToastControl":
                    break
                case "CItemMaterialCustomizationIconPanel":
                    break
                case "CImagePanel":
                    break
                case "CExplanationPopup":
                    break
                case "CRGBAImagePanel":
                    break
                case "CBackpackPanel":
                    break
                case "CModePanel":
                    break
                case "CTrainingDialog":
                    break
                case "CAchievementsDialog":
                    break
                case "CClassMenu":
                    break
                case "CBitmapPanel":
                    break
                case "CModeSelectionPanel":
                    break
                case "CCustomTextureImagePanel":
                    break
                case "CTFClassTipsItemPanel":
                    break
                case "CBasicTraining_ClassSelectionPanel":
                    break
                case "CBasicTraining_ClassDetailsPanel":
                    break
                case "COfflinePractice_ModeSelectionPanel":
                    break
                case "COfflinePractice_MapSelectionPanel":
                    break
                case "CLoadoutPresetPanel":
                    break
                case "CClassLoadoutPanel":
                    break
                case "CBuildingHealthBar":
                    break
                case "CBuildingStatusAlertTray":
                    break
                case "CTFFreezePanelHealth":
                    break
                case "CTFTeamButton":
                    break
                case "CModelPanel":
                    break
                case "CTFFooter":
                    break
                case "CMvMBombCarrierProgress":
                    break
                case "CTFProgressBar":
                    break
                case "CVictorySplash":
                    break
                case "CMvMVictoryPanelContainer":
                    break
                case "CMvMWaveLossPanel":
                    break
                case "CExRichText":
                    break
                case "CTFIntroMenu":
                    break
                case "CTFVideoPanel":
                    break
                case "CTFLayeredMapItemPanel":
                    break
                case "CTFClassTipsPanel":
                    break
                case "CBaseModelPanel":
                    break
                case "CCreditDisplayPanel":
                    break
                case "CCreditSpendPanel":
                    break
                case "CVictoryPanel":
                    break
                case "CMvMVictoryMannUpPanel":
                    break
                case "CMvMVictoryMannUpEntry":
                    break
                case "CTFHudEscortProgressBar":
                    break
                case "CPublishFileDialog":
                    break
                case "CPublishedFileBrowserDialog":
                    break
                case "CQuickPlayBusyDialog":
                    break
                case "CQuickplayDialog":
                    break
                case "CMainMenuNotificationsControl":
                    break
                case "CSteamWorkshopDialog":
                    break
                case "CSteamWorkshopItemPanel":
                    break
                case "CTankProgressBar":
                    break
                case "CPanelListPanel":
                    break
                case "CTrainingItemPanel":
                    break
                case "CTFTrainingComplete":
                    break
                case "CImageButton":
                    break
                case "CCommentaryExplanationDialog":
                    break
                case "CCommentaryItemPanel":
                    break
                case "CTGAImagePanel":
                    break
                case "COfflinePracticeServerPanel":
                    break
                case "CLoadGameDialog":
                    break
                case "CNewGameDialog":
                    break
                case "COptionsSubMultiplayer":
                    break
                case "CPlayerListDialog":
                    break
                case "CVoteSetupDialog":
                    break
                case "CCvarSlider":
                    break
                case "CControllerMap":
                    break
                case "CScenarioInfoPanel":
                    break
                case "CTFButton":
                    break
                case "CTFImageButton":
                    break
                case "CTFFlagStatus":
                    break
                case "CTFHudMannVsMachineScoreboard":
                    break
                case "CReplayReminderPanel":
                    break
                case "CircularProgressBar":
                    break // what the hell is this?
                case "PanelListPanel":
                    break
                case "ImageButton":
                    break
                case "RichText":
                    break
                case "SectionedListPanel":
                    break
                case "ListPanel":
                    break
                case "RoundInfoOverlay":
                    break
                case "ProgressBar":
                    break
                case "Slider":
                    break
                case "Divider":
                    break
                case "AnalogBar":
                    break
                case "FooterPanel":
                    break
                case "AnimatingImagePanel":
                    break
                case "RotatingProgressBar":
                    break
                case "MaterialButton":
                    break
                case "CustomTextureStencilGradientMapWidget":
                    break
                case "RadioButton":
                    break
                case "ScrollableEditablePanel":
                    break
                case "CheckButton":
                    break
                case "ComboBox":
                    break
                case "ScrollBar":
                    break
                case "Button":
                    break
                case "Panel":
                    break
                case "ImagePanel":
                    break
                case "ContinuousProgressBar":
                    break
                case "Menu":
                    break
                case "EditablePanel":
                    break
                case "Frame":
                    break
                case "ScalableImagePanel":
                    break
                case "Label":
                    break
                case "HTML":
                    break
                case "TextEntry":
                    break
                default:
                    LOG.log(Level.WARNING, "Unknown control: {0}", controlName)
                    break
            }
        } else {
            if ((file != null) && "hudlayout".equalsIgnoreCase(file)) {
                areas.put(name, this)
                //            System.out.println("adding " + this.name + " to areas")
            }
        }
    }

    public List<VDFProperty> getProps() {
        return ps
    }

    String getControlName() {
        return controlName
    }

    void setControlName(String controlName) {
        this.controlName = controlName
    }

    public String save() {
        StringBuilder sb = new StringBuilder()
        // preceding header
        for (VDFProperty p : ps) {
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
        for (VDFProperty p : ps) {
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
    public int getSize() { // works well unless they are exactly the same size
        return wide * tall
    }

    public Rectangle getBounds() {
        int minX = (int) Math.round(x * (screen.width / internal.width) * scale)
        int minY = (int) Math.round(y * (screen.height / internal.height) * scale)
        int maxX = (int) Math.round(width * (screen.width / internal.width) * scale)
        int maxY = (int) Math.round(height * (screen.height / internal.height) * scale)
        return [minX, minY, maxX + 1, maxY + 1] as Rectangle
    }

    @Override
    public String toString() {
        name + (info != null) ? (" ~ " + info) : "" // elements cannot have a value, only info
    }

    @Override
    Diff<Element> rdiff(Element element) { null }

    public int getX() {
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
                x = XAlignment == Alignment.Right ? parent.width - localX : localX
            }
            return x + parent.x
        }
    }

    public int getY() {
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
            y = YAlignment == Alignment.Right ? parent.height - localY : localY
        }
        return y + parent.y
    }

    public int getWidth() {
        return (_wideMode == DimensionMode.Mode2) ? (internal.width - wide) : wide
    }

    public int getHeight() {
        return (_tallMode == DimensionMode.Mode2) ? ((parent != null)
                ? (parent.height - tall)
                : (internal.height - tall)) : tall
    }

    public Font getFont() {
        return font
    }

    public void setFont(Font font) {
        this.font = font
    }

    public Color getFgColor() {
        return fgColor
    }

    void setFgColor(Color fgColor) {
        this.fgColor = fgColor
    }

    // TODO: remove duplicate keys (only keep the latest, or highlight duplicates)
    public void validateDisplay() {
        for (VDFProperty entry : ps) {
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

    public int getLocalX() {
        return (int) Math.round(localX)
    }

    public int getLocalY() { Math.round(localY) as int }

    @Override
    public Icon getIcon() { UIManager.getIcon "FileChooser.listViewIcon" }

    public enum DimensionMode {
        Mode1,
        Mode2
    }
}
