package com.timepath.hl2.io.util;

import com.timepath.hl2.io.RES;
import com.timepath.io.utils.ViewableData;
import com.timepath.steam.io.util.Property;
import com.timepath.hl2.io.VTF;
import com.timepath.steam.io.util.VDFNode;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.UIManager;

/**
 *
 * @author timepath
 */
public class Element extends VDFNode implements ViewableData {

    private static final Logger LOG = Logger.getLogger(Element.class.getName());

    public static Map<String, Element> areas = new HashMap<String, Element>();

    public static Element importVdf(VDFNode vdf) {
        Element e = new Element();
        e.ps = vdf.getProperties();
        e.setFile(vdf.getFile());
        e.validateLoad();
        return e;
    }

    public Element() {
    }

    public Element(String name) {
        this.name = name;
    }

    public Element(String name, String info) {
        this.name = name;
        this.info = info;
    }

    private Dimension screen = new Dimension(640, 480);

    private Dimension internal = new Dimension(640, 480);

    private double scale = 1;

    public String save() {
        String str = "";
        // preceding header
        for(Property p : this.getProps()) {
            if(p.getValue().length() == 0) {
                if(p.getKey().equals("\\n")) {
                    str += "\n";
                }
                if(p.getKey().equals("//")) {
                    str += "//" + p.getInfo() + "\n";
                }
            }
        }
        str += this.name + "\n";
        str += "{\n";
        for(Property p : this.getProps()) {
            if(p.getValue().length() != 0) {
                if(p.getKey().equals("\\n")) {
                    str += "\t    \n";
                } else {
                    str += "\t    " + p.getKey() + "\t    " + p.getValue() + (p.getInfo() != null ? " " + p.getInfo() : "") + "\n";
                }
            }
        }
        str += "}\n";
        return str;
    }

    private String name;

    public String getName() {
        return name;
    }

    private String info;

    private Element parent;

    // Extra stuff
    public int getSize() { // works well unless they are exactly the same size
        return wide * tall;
    }

    public Rectangle getBounds() {
        int minX = (int) Math.round(
                this.getX() * ((double) screen.width / (double) internal.width) * scale);
        int minY = (int) Math.round(
                this.getY() * ((double) screen.height / (double) internal.height) * scale);
        int maxX = (int) Math.round(
                this.getWidth() * ((double) screen.width / (double) internal.width) * scale);
        int maxY = (int) Math.round(
                this.getHeight() * ((double) screen.height / (double) internal.height) * scale);
        return new Rectangle(minX, minY, maxX + 1, maxY + 1);
    }

    @Override
    public String toString() {
        String displayInfo = (info != null ? (" ~ " + info) : ""); // elements cannot have a value, only info
        return name + displayInfo;
    }
    // Properties

    private double xPos;

    public int getLocalX() {
        return (int) Math.round(xPos);
    }

    public void setLocalX(double x) {
        this.xPos = x;
    }

    public int getX() {
        if(getParent() == null || getParent().name.replaceAll("\"", "").endsWith(".res")) {
            if(this.getXAlignment() == Alignment.Center) {
                return (getLocalX() + Math.round(internal.width / 2));
            } else if(this.getXAlignment() == Alignment.Right) {
                return (internal.width - getLocalX());
            } else {
                return getLocalX();
            }
        } else {
            int x;
            if(this.getXAlignment() == Alignment.Center) {
                x = (getParent().getWidth() / 2) + getLocalX();
            } else if(this.getXAlignment() == Alignment.Right) {
                x = (getParent().getWidth()) - getLocalX();
            } else {
                x = getLocalX();
            }
            return x + getParent().getX();
        }
    }

    private double yPos;

    public int getLocalY() {
        return (int) Math.round(yPos);
    }

    public void setLocalY(double y) {
        this.yPos = y;
    }

    public int getY() {
        if(getParent() == null || getParent().name.replaceAll("\"", "").endsWith(".res")) {
            if(this.getYAlignment() == Alignment.Center) {
                return (getLocalY() + Math.round(internal.height / 2));
            } else if(this.getYAlignment() == Alignment.Right) {
                return (internal.height - getLocalY());
            } else {
                return getLocalY();
            }
        }
        int y;
        if(this.getYAlignment() == Alignment.Center) {
            y = (getParent().getHeight() / 2) + getLocalY();
        } else if(this.getYAlignment() == Alignment.Right) {
            y = getParent().getHeight() - getLocalY();
        } else {
            y = getLocalY();
        }
        return y + getParent().getY();
    }

    // > 0 = out of screen
    private int zPos;

    public int getLayer() {
        return zPos;
    }

    public void setLayer(int z) {
        this.zPos = z;
    }

    private int wide;

    public int getLocalWidth() {
        return wide;
    }

    public void setLocalWidth(int wide) {
        this.wide = wide;
    }

    public int getWidth() {
        if(this.getWidthMode() == DimensionMode.Mode2) {
//            if(this.getParent().!hudRes= null) {
//                return this.getParent().getWidth() - wide;
//            } else {
            return internal.width - wide;
//            }
        } else {
            return wide;
        }
    }

    private DimensionMode _wideMode = DimensionMode.Mode1;

    public DimensionMode getWidthMode() {
        return _wideMode;
    }

    public void setWidthMode(DimensionMode mode) {
        this._wideMode = mode;
    }

    private int tall;

    public int getLocalHeight() {
        return tall;
    }

    public void setLocalHeight(int tall) {
        this.tall = tall;
    }

    public int getHeight() {
        return (this.getHeightMode() == DimensionMode.Mode2 ? (this.getParent() != null ? this.getParent().getHeight() - tall : internal.height - tall) : tall);
    }

    private DimensionMode _tallMode = DimensionMode.Mode1;

    public DimensionMode getHeightMode() {
        return _tallMode;
    }

    public void setHeightMode(DimensionMode mode) {
        this._tallMode = mode;
    }

    private boolean visible;

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    private boolean enabled;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private Font font;

    public Font getFont() {
        return font;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    private Color fgColor;

    public Color getFgColor() {
        return fgColor;
    }

    public void setFgColor(Color fgColor) {
        this.fgColor = fgColor;
    }

    // TODO: remove duplicate keys (only keep the latest, or highlight duplicates)
    public void validateLoad() {
        for(int n = 0; n < this.getProps().size(); n++) {
            Property entry = this.getProps().get(n);
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
                this.setEnabled(Integer.parseInt(v) == 1);
            } else if("visible".equalsIgnoreCase(k)) {
                this.setVisible(Integer.parseInt(v) == 1);
            } else if("xpos".equalsIgnoreCase(k)) {
                if(v.startsWith("c")) {
                    this.setXAlignment(Alignment.Center);
                    v = v.substring(1);
                } else if(v.startsWith("r")) {
                    this.setXAlignment(Alignment.Right);
                    v = v.substring(1);
                } else {
                    this.setXAlignment(Alignment.Left);
                }
                this.setLocalX(Integer.parseInt(v));
            } else if("ypos".equalsIgnoreCase(k)) {
                if(v.startsWith("c")) {
                    this.setYAlignment(Alignment.Center);
                    v = v.substring(1);
                } else if(v.startsWith("r")) {
                    this.setYAlignment(Alignment.Right);
                    v = v.substring(1);
                } else {
                    this.setYAlignment(Alignment.Left);
                }
                this.setLocalY(Integer.parseInt(v));
            } else if("zpos".equalsIgnoreCase(k)) {
                this.setLayer(Integer.parseInt(v));
            } else if("wide".equalsIgnoreCase(k)) {
                if(v.startsWith("f")) {
                    v = v.substring(1);
                    this.setWidthMode(DimensionMode.Mode2);
                }
                this.setLocalWidth(Integer.parseInt(v));
            } else if("tall".equalsIgnoreCase(k)) {
                if(v.startsWith("f")) {
                    v = v.substring(1);
                    this.setHeightMode(DimensionMode.Mode2);
                }
                this.setLocalHeight(Integer.parseInt(v));
            } else if("labelText".equalsIgnoreCase(k)) {
                this.setLabelText(v);
            } else if("textAlignment".equalsIgnoreCase(k)) {
                if(v.equalsIgnoreCase("center")) {
                    this.setTextAlignment(Alignment.Center);
                } else if(v.equalsIgnoreCase("right")) {
                    this.setTextAlignment(Alignment.Right);
                } else {
                    this.setTextAlignment(Alignment.Left);
                }
            } else if("ControlName".equalsIgnoreCase(k)) { // others are areas
                this.setControlName(v);
            } else if("fgcolor".equalsIgnoreCase(k)) {
                String[] c = v.split(" ");
                try {
                    this.setFgColor(new Color(Integer.parseInt(c[0]), Integer.parseInt(c[1]),
                                              Integer.parseInt(c[2]), Integer.parseInt(c[3])));
                } catch(NumberFormatException e) {
                }
            } else if("font".equalsIgnoreCase(k)) {
                if(!RES.fonts.containsKey(v)) {
                    continue;
                }
                HudFont a = RES.fonts.get(v);
                Font f = a.getFont();
                if(f != null) {
                    this.setFont(f);
                }
            } else if("image".equalsIgnoreCase(k) || "icon".equalsIgnoreCase(k)) {
                v = v.replaceAll("\"", "");
                if("".equals(v)) {
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
                    this.setImage(img);
                } catch(IOException ex) {
                    LOG.log(Level.SEVERE, null, ex);
                }
            } else {
//                System.out.println("Other property: " + k);
            }
        }

        if(this.getControlName() != null) { // temp
            if("CExLabel".equalsIgnoreCase(controlName)) {
            } else if("CIconPanel".equalsIgnoreCase(controlName)) {
            } else if("CTeamMenu".equalsIgnoreCase(controlName)) {
            } else if("CTFClassInfoPanel".equalsIgnoreCase(controlName)) {
            } else if("CTFArrowPanel".equalsIgnoreCase(controlName)) {
            } else if("CTFImagePanel".equalsIgnoreCase(controlName)) {
            } else if("CEmbeddedItemModelPanel".equalsIgnoreCase(controlName)) { // ooh, fancy
            } else if("CTFHudTimeStatus".equalsIgnoreCase(controlName)) {
            } else if("CWaveStatusPanel".equalsIgnoreCase(controlName)) { // MvM?
            } else if("CControlPointCountdown".equalsIgnoreCase(controlName)) { // control points
            } else if("CWaveCompleteSummaryPanel".equalsIgnoreCase(controlName)) { // MvM?
            } else if("CTFTextWindow".equalsIgnoreCase(controlName)) { // text motd?
            } else if("CTFRichText".equalsIgnoreCase(controlName)) {
            } else if("CTankStatusPanel".equalsIgnoreCase(controlName)) {
            } else if("CExImageButton".equalsIgnoreCase(controlName)) {
            } else if("CEngyDestroyMenuItem".equalsIgnoreCase(controlName)) {
            } else if("CCurrencyStatusPanel".equalsIgnoreCase(controlName)) {
            } else if("CTFClassImage".equalsIgnoreCase(controlName)) {
            } else if("CInWorldCurrencyStatus".equalsIgnoreCase(controlName)) { // MvM in world currency
            } else if("CTFClientScoreBoardDialog".equalsIgnoreCase(controlName)) { // scoreboard
            } else if("CTFHudEscort".equalsIgnoreCase(controlName)) { // payload?
            } else if("CWarningSwoop".equalsIgnoreCase(controlName)) {
            } else if("CItemModelPanel".equalsIgnoreCase(controlName)) {
            } else if("CStoreItemControlsPanel".equalsIgnoreCase(controlName)) {
            } else if("CExButton".equalsIgnoreCase(controlName)) {
            } else if("CStorePreviewItemPanel".equalsIgnoreCase(controlName)) {
            } else if("CStorePricePanel".equalsIgnoreCase(controlName)) {
            } else if("CRichTextWithScrollbarBorders".equalsIgnoreCase(controlName)) {
            } else if("CTFPlayerModelPanel".equalsIgnoreCase(controlName)) {
            } else if("CArmoryPanel".equalsIgnoreCase(controlName)) {
            } else if("CNotificationsPresentPanel".equalsIgnoreCase(controlName)) {
            } else if("CEconItemDetailsRichText".equalsIgnoreCase(controlName)) {
            } else if("CTFMapStampsInfoDialog".equalsIgnoreCase(controlName)) {
            } else if("CStorePreviewItemIcon".equalsIgnoreCase(controlName)) {
            } else if("CMouseMessageForwardingPanel".equalsIgnoreCase(controlName)) {
            } else if("CGenericNotificationToast".equalsIgnoreCase(controlName)) {
            } else if("CStorePreviewClassIcon".equalsIgnoreCase(controlName)) {
            } else if("CNavigationPanel".equalsIgnoreCase(controlName)) {
            } else if("CAvatarImagePanel".equalsIgnoreCase(controlName)) {
            } else if("CNotificationQueuePanel".equalsIgnoreCase(controlName)) {
            } else if("CPreviewRotButton".equalsIgnoreCase(controlName)) {
            } else if("CNotificationToastControl".equalsIgnoreCase(controlName)) {
            } else if("CItemMaterialCustomizationIconPanel".equalsIgnoreCase(controlName)) {
            } else if("CImagePanel".equalsIgnoreCase(controlName)) {
            } else if("CExplanationPopup".equalsIgnoreCase(controlName)) {
            } else if("CRGBAImagePanel".equalsIgnoreCase(controlName)) {
            } else if("CBackpackPanel".equalsIgnoreCase(controlName)) {
            } else if("CModePanel".equalsIgnoreCase(controlName)) {
            } else if("CTrainingDialog".equalsIgnoreCase(controlName)) {
            } else if("CAchievementsDialog".equalsIgnoreCase(controlName)) {
            } else if("CClassMenu".equalsIgnoreCase(controlName)) {
            } else if("CBitmapPanel".equalsIgnoreCase(controlName)) {
            } else if("CModeSelectionPanel".equalsIgnoreCase(controlName)) {
            } else if("CCustomTextureImagePanel".equalsIgnoreCase(controlName)) {
            } else if("CTFClassTipsItemPanel".equalsIgnoreCase(controlName)) {
            } else if("CBasicTraining_ClassSelectionPanel".equalsIgnoreCase(controlName)) {
            } else if("CBasicTraining_ClassDetailsPanel".equalsIgnoreCase(controlName)) {
            } else if("COfflinePractice_ModeSelectionPanel".equalsIgnoreCase(controlName)) {
            } else if("COfflinePractice_MapSelectionPanel".equalsIgnoreCase(controlName)) {
            } else if("CLoadoutPresetPanel".equalsIgnoreCase(controlName)) {
            } else if("CClassLoadoutPanel".equalsIgnoreCase(controlName)) {
            } else if("CBuildingHealthBar".equalsIgnoreCase(controlName)) {
            } else if("CBuildingStatusAlertTray".equalsIgnoreCase(controlName)) {
            } else if("CTFFreezePanelHealth".equalsIgnoreCase(controlName)) {
            } else if("CTFTeamButton".equalsIgnoreCase(controlName)) {
            } else if("CModelPanel".equalsIgnoreCase(controlName)) {
            } else if("CTFFooter".equalsIgnoreCase(controlName)) {
            } else if("CMvMBombCarrierProgress".equalsIgnoreCase(controlName)) {
            } else if("CTFProgressBar".equalsIgnoreCase(controlName)) {
            } else if("CVictorySplash".equalsIgnoreCase(controlName)) {
            } else if("CMvMVictoryPanelContainer".equalsIgnoreCase(controlName)) {
            } else if("CMvMWaveLossPanel".equalsIgnoreCase(controlName)) {
            } else if("CExRichText".equalsIgnoreCase(controlName)) {
            } else if("CTFIntroMenu".equalsIgnoreCase(controlName)) {
            } else if("CTFVideoPanel".equalsIgnoreCase(controlName)) {
            } else if("CTFLayeredMapItemPanel".equalsIgnoreCase(controlName)) {
            } else if("CTFClassTipsPanel".equalsIgnoreCase(controlName)) {
            } else if("CBaseModelPanel".equalsIgnoreCase(controlName)) {
            } else if("CCreditDisplayPanel".equalsIgnoreCase(controlName)) {
            } else if("CCreditSpendPanel".equalsIgnoreCase(controlName)) {
            } else if("CVictoryPanel".equalsIgnoreCase(controlName)) {
            } else if("CMvMVictoryMannUpPanel".equalsIgnoreCase(controlName)) {
            } else if("CMvMVictoryMannUpEntry".equalsIgnoreCase(controlName)) {
            } else if("CTFHudEscortProgressBar".equalsIgnoreCase(controlName)) {
            } else if("CPublishFileDialog".equalsIgnoreCase(controlName)) {
            } else if("CPublishedFileBrowserDialog".equalsIgnoreCase(controlName)) {
            } else if("CQuickPlayBusyDialog".equalsIgnoreCase(controlName)) {
            } else if("CQuickplayDialog".equalsIgnoreCase(controlName)) {
            } else if("CMainMenuNotificationsControl".equalsIgnoreCase(controlName)) {
            } else if("CSteamWorkshopDialog".equalsIgnoreCase(controlName)) {
            } else if("CSteamWorkshopItemPanel".equalsIgnoreCase(controlName)) {
            } else if("CTankProgressBar".equalsIgnoreCase(controlName)) {
            } else if("CPanelListPanel".equalsIgnoreCase(controlName)) {
            } else if("CTrainingItemPanel".equalsIgnoreCase(controlName)) {
            } else if("CTFTrainingComplete".equalsIgnoreCase(controlName)) {
            } else if("CImageButton".equalsIgnoreCase(controlName)) {
            } else if("CCommentaryExplanationDialog".equalsIgnoreCase(controlName)) {
            } else if("CCommentaryItemPanel".equalsIgnoreCase(controlName)) {
            } else if("CTGAImagePanel".equalsIgnoreCase(controlName)) {
            } else if("COfflinePracticeServerPanel".equalsIgnoreCase(controlName)) {
            } else if("CLoadGameDialog".equalsIgnoreCase(controlName)) {
            } else if("CNewGameDialog".equalsIgnoreCase(controlName)) {
            } else if("COptionsSubMultiplayer".equalsIgnoreCase(controlName)) {
            } else if("CPlayerListDialog".equalsIgnoreCase(controlName)) {
            } else if("CVoteSetupDialog".equalsIgnoreCase(controlName)) {
            } else if("CCvarSlider".equalsIgnoreCase(controlName)) {
            } else if("CControllerMap".equalsIgnoreCase(controlName)) {
            } else if("CScenarioInfoPanel".equalsIgnoreCase(controlName)) {
            } else if("CTFButton".equalsIgnoreCase(controlName)) {
            } else if("CTFImageButton".equalsIgnoreCase(controlName)) {
            } else if("CTFFlagStatus".equalsIgnoreCase(controlName)) {
            } else if("CTFHudMannVsMachineScoreboard".equalsIgnoreCase(controlName)) {
            } else if("CReplayReminderPanel".equalsIgnoreCase(controlName)) {
            } else if("CircularProgressBar".equalsIgnoreCase(controlName)) { // what the hell is this?
            } else if("PanelListPanel".equalsIgnoreCase(controlName)) {
            } else if("ImageButton".equalsIgnoreCase(controlName)) {
            } else if("RichText".equalsIgnoreCase(controlName)) {
            } else if("SectionedListPanel".equalsIgnoreCase(controlName)) {
            } else if("ListPanel".equalsIgnoreCase(controlName)) {
            } else if("RoundInfoOverlay".equalsIgnoreCase(controlName)) {
            } else if("ProgressBar".equalsIgnoreCase(controlName)) {
            } else if("Slider".equalsIgnoreCase(controlName)) {
            } else if("Divider".equalsIgnoreCase(controlName)) {
            } else if("AnalogBar".equalsIgnoreCase(controlName)) {
            } else if("FooterPanel".equalsIgnoreCase(controlName)) {
            } else if("AnimatingImagePanel".equalsIgnoreCase(controlName)) {
            } else if("RotatingProgressBar".equalsIgnoreCase(controlName)) {
            } else if("MaterialButton".equalsIgnoreCase(controlName)) {
            } else if("CustomTextureStencilGradientMapWidget".equalsIgnoreCase(controlName)) {
            } else if("RadioButton".equalsIgnoreCase(controlName)) {
            } else if("ScrollableEditablePanel".equalsIgnoreCase(controlName)) {
            } else if("CheckButton".equalsIgnoreCase(controlName)) {
            } else if("ComboBox".equalsIgnoreCase(controlName)) {
            } else if("ScrollBar".equalsIgnoreCase(controlName)) {
            } else if("Button".equalsIgnoreCase(controlName)) {
            } else if("Panel".equalsIgnoreCase(controlName)) {
            } else if("ImagePanel".equalsIgnoreCase(controlName)) {
            } else if("ContinuousProgressBar".equalsIgnoreCase(controlName)) {
            } else if("Menu".equalsIgnoreCase(controlName)) {
            } else if("EditablePanel".equalsIgnoreCase(controlName)) {
            } else if("Frame".equalsIgnoreCase(controlName)) {
            } else if("ScalableImagePanel".equalsIgnoreCase(controlName)) {
            } else if("Label".equalsIgnoreCase(controlName)) {
            } else if("HTML".equalsIgnoreCase(controlName)) {
            } else if("TextEntry".equalsIgnoreCase(controlName)) {
            } else {
                LOG.log(Level.WARNING, "Unknown control: {0}", controlName);
            }
        } else if(this.getFile().equalsIgnoreCase("hudlayout")) {
            areas.put(this.name, this);
//            System.out.println("adding " + this.name + " to areas");
        }
    }

    private ArrayList<Property> ps = new ArrayList<Property>();

    public ArrayList<Property> getProps() {
        return ps;
    }

    // TODO: remove duplicate keys (only keep the latest, or highlight duplicates)
    public void validateDisplay() {
        for(Property entry : this.getProps()) {
            String k = entry.getKey();
            if(k == null) {
                continue;
            }

            if("enabled".equalsIgnoreCase(k)) {
                entry.setValue(this.isEnabled() ? 1 : 0);
            } else if("visible".equalsIgnoreCase(k)) {
                entry.setValue(this.isVisible() ? 1 : 0);
            } else if("xpos".equalsIgnoreCase(k)) {
                entry.setValue(
                        this.getXAlignment().name().substring(0, 1).toLowerCase().replaceFirst("l",
                                                                                               "") + this.getLocalX());
            } else if("ypos".equalsIgnoreCase(k)) {
                entry.setValue(
                        this.getYAlignment().name().substring(0, 1).toLowerCase().replaceFirst("l",
                                                                                               "") + this.getLocalY());
            } else if("zpos".equalsIgnoreCase(k)) {
                entry.setValue(this.getLayer());
            } else if("wide".equalsIgnoreCase(k)) {
                entry.setValue(
                        (this.getWidthMode() == DimensionMode.Mode2 ? "f" : "") + this.getLocalWidth());
            } else if("tall".equalsIgnoreCase(k)) {
                entry.setValue(
                        (this.getHeightMode() == DimensionMode.Mode2 ? "f" : "") + this.getLocalHeight());
            } else if("labelText".equalsIgnoreCase(k)) {
                entry.setValue(this.getLabelText());
            } else if("ControlName".equalsIgnoreCase(k)) {
                entry.setValue(this.getControlName());
            } else if("font".equalsIgnoreCase(k)) {
//                entry.setValue(this.getFont()); // TODO
            }
        }
    }

    public Element getParent() {
        return parent;
    }

    public void setParent(Element newParent) {
        this.parent = newParent;
    }

    private String controlName;

    public void setControlName(String controlName) {
        this.controlName = controlName;
    }

    public String getControlName() {
        return controlName;
    }

    private Alignment _xAlignment = Alignment.Left;

    public Alignment getXAlignment() {
        return _xAlignment;
    }

    public void setXAlignment(Alignment _xAlignment) {
        this._xAlignment = _xAlignment;
    }

    private Alignment _yAlignment = Alignment.Left;

    public Alignment getYAlignment() {
        return _yAlignment;
    }

    public void setYAlignment(Alignment _yAlignment) {
        this._yAlignment = _yAlignment;
    }

    private String labelText;

    public String getLabelText() {
        return labelText;
    }

    public void setLabelText(String labelText) {
        this.labelText = labelText;
    }

    private Alignment _textAlignment = Alignment.Left;

    public Alignment getTextAlignment() {
        return _textAlignment;
    }

    public void setTextAlignment(Alignment _yAlignment) {
        this._textAlignment = _yAlignment;
    }

    private Image image;

    public Image getImage() {
        return image;
    }

    public void setImage(Image image) {
        this.image = image;
    }

    public Icon getIcon() {
        return UIManager.getIcon("FileChooser.listViewIcon");
    }

    public enum DimensionMode {

        Mode1,
        Mode2

    }

}