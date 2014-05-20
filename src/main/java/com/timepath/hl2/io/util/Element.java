package com.timepath.hl2.io.util;

import com.timepath.hl2.io.RES;
import com.timepath.hl2.io.image.VTF;
import com.timepath.io.utils.ViewableData;
import com.timepath.steam.io.util.Property;
import com.timepath.steam.io.util.VDFNode1;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class Element extends VDFNode1 implements ViewableData {

    public static final  Map<String, Element> areas    = new HashMap<>(0);
    private static final Logger               LOG      = Logger.getLogger(Element.class.getName());
    private final        Dimension            screen   = new Dimension(640, 480);
    private final        Dimension            internal = new Dimension(640, 480);
    private final        double               scale    = 1;
    private String  name;
    private String  info;
    private Element parent;
    // Properties
    private double  xPos;
    private double  yPos;
    // > 0 = out of screen
    private int     zPos;
    private int     wide;
    private DimensionMode _wideMode = DimensionMode.Mode1;
    private int tall;
    private DimensionMode _tallMode = DimensionMode.Mode1;
    private boolean visible;
    private boolean enabled;
    private Font    font;
    private Color   fgColor;
    private List<Property> ps = new LinkedList<>();
    private String controlName;
    private Alignment _xAlignment = Alignment.Left;
    @SuppressWarnings("SuspiciousNameCombination")
    private Alignment _yAlignment = Alignment.Left;
    private String labelText;
    private Alignment _textAlignment = Alignment.Left;
    private Image image;

    private Element() {
    }

    private Element(String name) {
        this.name = name;
    }

    private Element(String name, String info) {
        this.name = name;
        this.info = info;
    }

    public static Element importVdf(VDFNode1 vdf) {
        Element e = new Element();
        e.ps = vdf.getProperties();
        e.setFile(vdf.getFile());
        e.validateLoad();
        return e;
    }

    // TODO: remove duplicate keys (only keep the latest, or highlight duplicates)
    public void validateLoad() {
        for(Property entry : ps) {
            String k = entry.getKey().toLowerCase();
            //            if(k != null && k.contains("\"")) { // assumes one set of quotes
            //                k = k.substring(1, k.length() - 1);
            //                k = k.replaceAll("\"", "").trim();
            //            }
            String v = entry.getValue();
            //            if(v != null && v.contains("\"")) {
            //                v = v.substring(1, v.length() - 1);
            //                v = v.replaceAll("\"", "").trim();
            //            }
            //            String i = entry.getInfo();
            //            if(i != null && i.contains("\"")) {
            //                i = i.substring(1, i.length() - 1);
            //                i = i.replaceAll("\"", "").trim();
            //            }
            if("enabled".equalsIgnoreCase(k)) {
                enabled = Integer.parseInt(v) == 1;
            } else if("visible".equalsIgnoreCase(k)) {
                visible = Integer.parseInt(v) == 1;
            } else if("xpos".equalsIgnoreCase(k)) {
                if(v.startsWith("c")) {
                    _xAlignment = Alignment.Center;
                    v = v.substring(1);
                } else if(v.startsWith("r")) {
                    _xAlignment = Alignment.Right;
                    v = v.substring(1);
                } else {
                    _xAlignment = Alignment.Left;
                }
                xPos = Integer.parseInt(v);
            } else if("ypos".equalsIgnoreCase(k)) {
                if(v.startsWith("c")) {
                    _yAlignment = Alignment.Center;
                    v = v.substring(1);
                } else if(v.startsWith("r")) {
                    //noinspection SuspiciousNameCombination
                    _yAlignment = Alignment.Right;
                    v = v.substring(1);
                } else {
                    //noinspection SuspiciousNameCombination
                    _yAlignment = Alignment.Left;
                }
                yPos = Integer.parseInt(v);
            } else if("zpos".equalsIgnoreCase(k)) {
                zPos = Integer.parseInt(v);
            } else if("wide".equalsIgnoreCase(k)) {
                if(v.startsWith("f")) {
                    v = v.substring(1);
                    _wideMode = DimensionMode.Mode2;
                }
                wide = Integer.parseInt(v);
            } else if("tall".equalsIgnoreCase(k)) {
                if(v.startsWith("f")) {
                    v = v.substring(1);
                    _tallMode = DimensionMode.Mode2;
                }
                tall = Integer.parseInt(v);
            } else if("labelText".equalsIgnoreCase(k)) {
                labelText = v;
            } else if("textAlignment".equalsIgnoreCase(k)) {
                if("center".equalsIgnoreCase(v)) {
                    _textAlignment = Alignment.Center;
                } else { _textAlignment = "right".equalsIgnoreCase(v) ? Alignment.Right : Alignment.Left; }
            } else if("ControlName".equalsIgnoreCase(k)) { // others are areas
                controlName = v;
            } else if("fgcolor".equalsIgnoreCase(k)) {
                String[] c = v.split(" ");
                try {
                    fgColor = new Color(Integer.parseInt(c[0]),
                                        Integer.parseInt(c[1]),
                                        Integer.parseInt(c[2]),
                                        Integer.parseInt(c[3]));
                } catch(NumberFormatException ignored) {
                }
            } else if("font".equalsIgnoreCase(k)) {
                if(!RES.fonts.containsKey(v)) {
                    continue;
                }
                HudFont a = RES.fonts.get(v);
                Font f = a.getFont();
                if(f != null) {
                    font = f;
                }
            } else if("image".equalsIgnoreCase(k) || "icon".equalsIgnoreCase(k)) {
                v = v.replaceAll("\"", "");
                if(( v != null ) && v.isEmpty()) {
                    continue;
                }
                try {
                    VTF vtf = VTF.load(v + ".vtf");
                    if(vtf == null) {
                        continue;
                    }
                    Image img = vtf.getImage(0);
                    if(img == null) {
                        continue;
                    }
                    image = img;
                } catch(IOException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            } else {
                LOG.log(Level.WARNING, "Unknown property: {0}", k);
            }
        }
        if(controlName != null) { // temp
            switch(controlName) {
                case "CExLabel":
                    break;
                case "CIconPanel":
                    break;
                case "CTeamMenu":
                    break;
                case "CTFClassInfoPanel":
                    break;
                case "CTFArrowPanel":
                    break;
                case "CTFImagePanel":
                    break;
                case "CEmbeddedItemModelPanel":
                    break; // ooh, fancy
                case "CTFHudTimeStatus":
                    break;
                case "CWaveStatusPanel":
                    break; // MvM?
                case "CControlPointCountdown":
                    break; // control points
                case "CWaveCompleteSummaryPanel":
                    break; // MvM?
                case "CTFTextWindow":
                    break; // text motd?
                case "CTFRichText":
                    break;
                case "CTankStatusPanel":
                    break;
                case "CExImageButton":
                    break;
                case "CEngyDestroyMenuItem":
                    break;
                case "CCurrencyStatusPanel":
                    break;
                case "CTFClassImage":
                    break;
                case "CInWorldCurrencyStatus":
                    break; // MvM in world currency
                case "CTFClientScoreBoardDialog":
                    break; // scoreboard
                case "CTFHudEscort":
                    break; // payload?
                case "CWarningSwoop":
                    break;
                case "CItemModelPanel":
                    break;
                case "CStoreItemControlsPanel":
                    break;
                case "CExButton":
                    break;
                case "CStorePreviewItemPanel":
                    break;
                case "CStorePricePanel":
                    break;
                case "CRichTextWithScrollbarBorders":
                    break;
                case "CTFPlayerModelPanel":
                    break;
                case "CArmoryPanel":
                    break;
                case "CNotificationsPresentPanel":
                    break;
                case "CEconItemDetailsRichText":
                    break;
                case "CTFMapStampsInfoDialog":
                    break;
                case "CStorePreviewItemIcon":
                    break;
                case "CMouseMessageForwardingPanel":
                    break;
                case "CGenericNotificationToast":
                    break;
                case "CStorePreviewClassIcon":
                    break;
                case "CNavigationPanel":
                    break;
                case "CAvatarImagePanel":
                    break;
                case "CNotificationQueuePanel":
                    break;
                case "CPreviewRotButton":
                    break;
                case "CNotificationToastControl":
                    break;
                case "CItemMaterialCustomizationIconPanel":
                    break;
                case "CImagePanel":
                    break;
                case "CExplanationPopup":
                    break;
                case "CRGBAImagePanel":
                    break;
                case "CBackpackPanel":
                    break;
                case "CModePanel":
                    break;
                case "CTrainingDialog":
                    break;
                case "CAchievementsDialog":
                    break;
                case "CClassMenu":
                    break;
                case "CBitmapPanel":
                    break;
                case "CModeSelectionPanel":
                    break;
                case "CCustomTextureImagePanel":
                    break;
                case "CTFClassTipsItemPanel":
                    break;
                case "CBasicTraining_ClassSelectionPanel":
                    break;
                case "CBasicTraining_ClassDetailsPanel":
                    break;
                case "COfflinePractice_ModeSelectionPanel":
                    break;
                case "COfflinePractice_MapSelectionPanel":
                    break;
                case "CLoadoutPresetPanel":
                    break;
                case "CClassLoadoutPanel":
                    break;
                case "CBuildingHealthBar":
                    break;
                case "CBuildingStatusAlertTray":
                    break;
                case "CTFFreezePanelHealth":
                    break;
                case "CTFTeamButton":
                    break;
                case "CModelPanel":
                    break;
                case "CTFFooter":
                    break;
                case "CMvMBombCarrierProgress":
                    break;
                case "CTFProgressBar":
                    break;
                case "CVictorySplash":
                    break;
                case "CMvMVictoryPanelContainer":
                    break;
                case "CMvMWaveLossPanel":
                    break;
                case "CExRichText":
                    break;
                case "CTFIntroMenu":
                    break;
                case "CTFVideoPanel":
                    break;
                case "CTFLayeredMapItemPanel":
                    break;
                case "CTFClassTipsPanel":
                    break;
                case "CBaseModelPanel":
                    break;
                case "CCreditDisplayPanel":
                    break;
                case "CCreditSpendPanel":
                    break;
                case "CVictoryPanel":
                    break;
                case "CMvMVictoryMannUpPanel":
                    break;
                case "CMvMVictoryMannUpEntry":
                    break;
                case "CTFHudEscortProgressBar":
                    break;
                case "CPublishFileDialog":
                    break;
                case "CPublishedFileBrowserDialog":
                    break;
                case "CQuickPlayBusyDialog":
                    break;
                case "CQuickplayDialog":
                    break;
                case "CMainMenuNotificationsControl":
                    break;
                case "CSteamWorkshopDialog":
                    break;
                case "CSteamWorkshopItemPanel":
                    break;
                case "CTankProgressBar":
                    break;
                case "CPanelListPanel":
                    break;
                case "CTrainingItemPanel":
                    break;
                case "CTFTrainingComplete":
                    break;
                case "CImageButton":
                    break;
                case "CCommentaryExplanationDialog":
                    break;
                case "CCommentaryItemPanel":
                    break;
                case "CTGAImagePanel":
                    break;
                case "COfflinePracticeServerPanel":
                    break;
                case "CLoadGameDialog":
                    break;
                case "CNewGameDialog":
                    break;
                case "COptionsSubMultiplayer":
                    break;
                case "CPlayerListDialog":
                    break;
                case "CVoteSetupDialog":
                    break;
                case "CCvarSlider":
                    break;
                case "CControllerMap":
                    break;
                case "CScenarioInfoPanel":
                    break;
                case "CTFButton":
                    break;
                case "CTFImageButton":
                    break;
                case "CTFFlagStatus":
                    break;
                case "CTFHudMannVsMachineScoreboard":
                    break;
                case "CReplayReminderPanel":
                    break;
                case "CircularProgressBar":
                    break; // what the hell is this?
                case "PanelListPanel":
                    break;
                case "ImageButton":
                    break;
                case "RichText":
                    break;
                case "SectionedListPanel":
                    break;
                case "ListPanel":
                    break;
                case "RoundInfoOverlay":
                    break;
                case "ProgressBar":
                    break;
                case "Slider":
                    break;
                case "Divider":
                    break;
                case "AnalogBar":
                    break;
                case "FooterPanel":
                    break;
                case "AnimatingImagePanel":
                    break;
                case "RotatingProgressBar":
                    break;
                case "MaterialButton":
                    break;
                case "CustomTextureStencilGradientMapWidget":
                    break;
                case "RadioButton":
                    break;
                case "ScrollableEditablePanel":
                    break;
                case "CheckButton":
                    break;
                case "ComboBox":
                    break;
                case "ScrollBar":
                    break;
                case "Button":
                    break;
                case "Panel":
                    break;
                case "ImagePanel":
                    break;
                case "ContinuousProgressBar":
                    break;
                case "Menu":
                    break;
                case "EditablePanel":
                    break;
                case "Frame":
                    break;
                case "ScalableImagePanel":
                    break;
                case "Label":
                    break;
                case "HTML":
                    break;
                case "TextEntry":
                    break;
                default:
                    LOG.log(Level.WARNING, "Unknown control: {0}", controlName);
                    break;
            }
        } else if(( getFile() != null ) && "hudlayout".equalsIgnoreCase(getFile())) {
            areas.put(name, this);
            //            System.out.println("adding " + this.name + " to areas");
        }
    }

    public List<Property> getProps() {
        return ps;
    }

    String getControlName() {
        return controlName;
    }

    void setControlName(String controlName) {
        this.controlName = controlName;
    }

    public String save() {
        StringBuilder sb = new StringBuilder();
        // preceding header
        for(Property p : ps) {
            if(p.getValue().isEmpty()) {
                if("\\n".equals(p.getKey())) {
                    sb.append('\n');
                }
                if("//".equals(p.getKey())) {
                    sb.append("//").append(p.getInfo()).append('\n');
                }
            }
        }
        sb.append(name).append('\n');
        sb.append("{\n");
        for(Property p : ps) {
            if(!p.getValue().isEmpty()) {
                sb.append("\\n".equals(p.getKey()) ? "\t    \n" : ( "\t    " + p.getKey() + "\t    " + p.getValue() +
                                                                    ( ( p.getInfo() != null ) ? ( ' ' + p.getInfo() ) : "" ) +
                                                                    '\n' ));
            }
        }
        sb.append("}\n");
        return sb.toString();
    }

    public String getName() {
        return name;
    }

    // Extra stuff
    public int getSize() { // works well unless they are exactly the same size
        return wide * tall;
    }

    public Rectangle getBounds() {
        int minX = (int) Math.round(getX() * ( screen.width / internal.width ) * scale);
        int minY = (int) Math.round(getY() * ( screen.height / internal.height ) * scale);
        int maxX = (int) Math.round(getWidth() * ( screen.width / internal.width ) * scale);
        int maxY = (int) Math.round(getHeight() * ( screen.height / internal.height ) * scale);
        return new Rectangle(minX, minY, maxX + 1, maxY + 1);
    }

    @Override
    public String toString() {
        String displayInfo = ( info != null ) ? ( " ~ " + info ) : ""; // elements cannot have a value, only info
        return name + displayInfo;
    }

    public int getX() {
        if(( parent == null ) || parent.name.replaceAll("\"", "").endsWith(".res")) {
            if(_xAlignment == Alignment.Center) {
                return getLocalX() + ( internal.width / 2 );
            } else { return ( _xAlignment == Alignment.Right ) ? ( internal.width - getLocalX() ) : getLocalX(); }
        } else {
            int x;
            if(_xAlignment == Alignment.Center) {
                x = parent.getWidth() / 2 + getLocalX();
            } else { x = _xAlignment == Alignment.Right ? parent.getWidth() - getLocalX() : getLocalX(); }
            return x + parent.getX();
        }
    }

    public int getY() {
        if(( parent == null ) || parent.name.replaceAll("\"", "").endsWith(".res")) {
            if(_yAlignment == Alignment.Center) {
                return getLocalY() + ( internal.height / 2 );
            } else { return ( _yAlignment == Alignment.Right ) ? ( internal.height - getLocalY() ) : getLocalY(); }
        }
        int y;
        if(_yAlignment == Alignment.Center) {
            y = parent.getHeight() / 2 + getLocalY();
        } else { y = _yAlignment == Alignment.Right ? parent.getHeight() - getLocalY() : getLocalY(); }
        return y + parent.getY();
    }

    public int getWidth() {
        return ( _wideMode == DimensionMode.Mode2 ) ? ( internal.width - wide ) : wide;
    }

    public int getHeight() {
        return ( _tallMode == DimensionMode.Mode2 ) ? ( ( parent != null )
                                                        ? ( parent.getHeight() - tall )
                                                        : ( internal.height - tall ) ) : tall;
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

    void setFgColor(Color fgColor) {
        this.fgColor = fgColor;
    }

    // TODO: remove duplicate keys (only keep the latest, or highlight duplicates)
    public void validateDisplay() {
        for(Property entry : ps) {
            String k = entry.getKey();
            if(k == null) {
                continue;
            }
            if("enabled".equalsIgnoreCase(k)) {
                entry.setValue(enabled ? 1 : 0);
            } else if("visible".equalsIgnoreCase(k)) {
                entry.setValue(visible ? 1 : 0);
            } else if("xpos".equalsIgnoreCase(k)) {
                entry.setValue(_xAlignment.name().substring(0, 1).toLowerCase().replaceFirst("l", "") + getLocalX());
            } else if("ypos".equalsIgnoreCase(k)) {
                entry.setValue(_yAlignment.name().substring(0, 1).toLowerCase().replaceFirst("l", "") + getLocalY());
            } else if("zpos".equalsIgnoreCase(k)) {
                entry.setValue(zPos);
            } else if("wide".equalsIgnoreCase(k)) {
                entry.setValue(( ( _wideMode == DimensionMode.Mode2 ) ? "f" : "" ) + wide);
            } else if("tall".equalsIgnoreCase(k)) {
                entry.setValue(( ( _tallMode == DimensionMode.Mode2 ) ? "f" : "" ) + tall);
            } else if("labelText".equalsIgnoreCase(k)) {
                entry.setValue(labelText);
            } else if("ControlName".equalsIgnoreCase(k)) {
                entry.setValue(controlName);
            }
            //            else if("font".equalsIgnoreCase(k)) {
            //                entry.setValue(this.getFont()); // TODO
            //            }
        }
    }

    public int getLocalX() {
        return (int) Math.round(xPos);
    }

    public void setLocalX(double x) {
        xPos = x;
    }

    public int getLocalY() {
        return (int) Math.round(yPos);
    }

    public void setLocalY(double y) {
        yPos = y;
    }

    public int getLayer() {
        return zPos;
    }

    void setLayer(int z) {
        zPos = z;
    }

    int getLocalWidth() {
        return wide;
    }

    void setLocalWidth(int wide) {
        this.wide = wide;
    }

    DimensionMode getWidthMode() {
        return _wideMode;
    }

    void setWidthMode(DimensionMode mode) {
        _wideMode = mode;
    }

    int getLocalHeight() {
        return tall;
    }

    void setLocalHeight(int tall) {
        this.tall = tall;
    }

    DimensionMode getHeightMode() {
        return _tallMode;
    }

    void setHeightMode(DimensionMode mode) {
        _tallMode = mode;
    }

    boolean isVisible() {
        return visible;
    }

    void setVisible(boolean visible) {
        this.visible = visible;
    }

    boolean isEnabled() {
        return enabled;
    }

    void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Alignment getXAlignment() {
        return _xAlignment;
    }

    void setXAlignment(Alignment _xAlignment) {
        this._xAlignment = _xAlignment;
    }

    public Alignment getYAlignment() {
        return _yAlignment;
    }

    void setYAlignment(Alignment _yAlignment) {
        this._yAlignment = _yAlignment;
    }

    public String getLabelText() {
        return labelText;
    }

    void setLabelText(String labelText) {
        this.labelText = labelText;
    }

    @Override
    public Element getParent() {
        return parent;
    }

    public void setParent(Element newParent) {
        parent = newParent;
    }

    public Alignment getTextAlignment() {
        return _textAlignment;
    }

    void setTextAlignment(Alignment _yAlignment) {
        _textAlignment = _yAlignment;
    }

    public Image getImage() {
        return image;
    }

    void setImage(Image image) {
        this.image = image;
    }

    @Override
    public Icon getIcon() {
        return UIManager.getIcon("FileChooser.listViewIcon");
    }

    public enum DimensionMode {
        Mode1,
        Mode2
    }
}
