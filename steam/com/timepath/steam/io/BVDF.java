package com.timepath.steam.io;

import com.timepath.DataUtils;
import com.timepath.swing.TreeUtils;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;

/**
 * See also:
 * https://github.com/harvimt/steam_launcher/blob/master/binvdf.py
 * https://github.com/barneygale/bvdf/blob/master/bvdf.py
 * https://github.com/DHager/hl2parse
 * http://cs.rin.ru/forum/viewtopic.php?f=20&t=61506&hilit=appinfo
 * http://cs.rin.ru/forum/viewtopic.php?f=20&t=62438&hilit=packageinfo
 * http://media.steampowered.com/steamcommunity/public/images/apps/[appID]/[sha].[ext]
 * http://cdr.xpaw.ru/app/5/#section_info
 *
 * @author timepath
 */
public class BVDF {

    public static void analyze(File f, DefaultMutableTreeNode root) throws IOException {
        DataNode dn = new DataNode("root");
        ByteBuffer buf = DataUtils.mapFile(f);
        parse(buf, dn);
//        root.add(dn);
        TreeUtils.moveChildren(dn, root);
    }

    private static void parse(ByteBuffer buf, DataNode dn) throws IOException {
        int magic = buf.getInt();

        if(magic == 0x07564426) {
            //<editor-fold defaultstate="collapsed" desc="AppInfo">
            int universe = buf.getInt();
            dn.add(new DataNode("universe", universe));
            for(;;) {
                int appID = buf.getInt();
                if(appID == 0x0000) {
                    break;
                }
                DataNode c = new DataNode("#" + appID);
                dn.add(c);

                int size = buf.getInt(); // skip this many bytes to reach the next entry
                c.add(new DataNode("size", size));

                parseAppEntry(DataUtils.getSlice(buf, size), c);
            }
            //</editor-fold>
        } else if(magic == 0x06565527) {
            //<editor-fold defaultstate="collapsed" desc="PackageInfo">
            int universe = buf.getInt();
            dn.add(new DataNode("universe", universe));
            for(;;) {
                int appID = buf.getInt();
                if(appID == 0xFFFFFFFF || appID == -1) { // same thing
                    break;
                }
                DataNode c = new DataNode("#", appID);
                dn.add(c);

                byte[] sha = new byte[20];
                buf.get(sha);
                c.add(new DataNode("sha", Arrays.toString(sha)));

                int changeNumber = buf.getInt();
                c.add(new DataNode("changeNumber", changeNumber));

                DataNode bin = new DataNode();
                bin.name = "Binary Data";
                c.add(bin);
                TreeUtils.moveChildren(parseBinaryData(buf), bin);
            }
            //</editor-fold>
        } else {
            buf.position(0);
            TreeUtils.moveChildren(parseBinaryData(buf), dn);
        }
    }

    private static void parseAppEntry(ByteBuffer buf, DataNode c) {
        int appInfoState = buf.getInt();
        c.add(new DataNode("state", appInfoState));

        long lastUpdated = buf.getInt();
        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        String formattedDate = df.format(new Date(lastUpdated * 1000));
        DataNode lu = new DataNode("lastUpdated", lastUpdated);
        lu.add(new DataNode("formatted", formattedDate));
        c.add(lu);

        long token = buf.getLong();
        c.add(new DataNode("token", token));

        byte[] sha = new byte[20];
        buf.get(sha);
        c.add(new DataNode("sha", Arrays.toString(sha)));

        int changeNumber = buf.getInt();
        c.add(new DataNode("changeNumber", changeNumber));

        DataNode sections = new DataNode("Sections");
        c.add(sections);
        for(;;) {
            byte section = buf.get();
            if(section == 0x00) {
                break;
            }
            DataNode sectionNode = new DataNode("#", section);
            sections.add(sectionNode);
            
            TreeUtils.moveChildren(parseBinaryData(buf), sections);
        }
    }

    private static int KEYVALUES_TOKEN_SIZE = 1024;

    /**
     *
     * http://hlssmod.net/he_code/public/tier1/KeyValues.h
     * http://hpmod.googlecode.com/svn/trunk/tier1/KeyValues.cpp
     *
     * @param buffer
     * @param dn
     * @param end
     * @param timesEndSeen
     *
     * @return
     */
    private static DataNode parseBinaryData(ByteBuffer buffer) {
        DataNode parent = new DataNode();
        parent.name = "<joiner>";
        for(;;) {
            DataNode dat = new DataNode();
            ValueType type = ValueType.get(buffer.get());
            LOG.log(Level.FINE, "Type : {0}", type);
            if(type == null) {
                LOG.log(Level.SEVERE, "Something went horribly wrong in my code");
                return parent;
            }

            if(type == ValueType.TYPE_NUMTYPES) {
                LOG.log(Level.FINE, "No more peers");
                break;
            }
            dat.type = type;
            int originalPosition = buffer.position();
            String token = DataUtils.getText(DataUtils.getSafeSlice(buffer, KEYVALUES_TOKEN_SIZE - 1), true);
            buffer.position(originalPosition + token.length() + 1);
            LOG.log(Level.FINE, "Token: {0}", Arrays.toString(token.getBytes()));
            dat.name = token;

            switch(type) {
                case TYPE_NONE:
                    LOG.log(Level.FINE, "Node has children");
                    TreeUtils.moveChildren(parseBinaryData(buffer), dat);
                    break;

                case TYPE_STRING:
                    int originalPosition2 = buffer.position();
                    String stringValue = DataUtils.getText(DataUtils.getSafeSlice(buffer, KEYVALUES_TOKEN_SIZE - 1), true);
                    buffer.position(originalPosition2 + stringValue.length() + 1);
                    dat.value = (stringValue);
                    LOG.log(Level.FINE, "String value: {0}", Arrays.toString(stringValue.getBytes()));
                    break;

                case TYPE_WSTRING:
                    LOG.log(Level.SEVERE, "Detected {0}, this should never happen", type);
                    break;

                case TYPE_INT:
                    int intValue = buffer.getInt(); // is this 2 or 4 bytes?
                    dat.value = (intValue);
                    LOG.log(Level.FINE, "Int value: {0}", intValue);
                    break;

                case TYPE_FLOAT:
                    float floatValue = buffer.getFloat();
                    dat.value = (floatValue);
                    LOG.log(Level.FINE, "Float value: {0}", floatValue);
                    break;

                case TYPE_COLOR:
                    Color colorValue = new Color(buffer.get(), buffer.get(), buffer.get(), buffer.get());
                    dat.value = (colorValue);
                    LOG.log(Level.FINE, "Color value: {0}", colorValue);
                    break;

                case TYPE_PTR:
                    long pointerValue = buffer.getInt();
                    dat.value = (pointerValue);
                    LOG.log(Level.FINE, "Pointer value: {0}", pointerValue);
                    break;

                default:
                    LOG.log(Level.SEVERE, "Unhandled data type {0}", type);
                    break;
            }
            
            parent.add(dat);
        }
        return parent;
    }

    private static class DataNode extends DefaultMutableTreeNode {

        DataNode(Object obj) {
            this.value = obj;
        }

        DataNode(String name, Object obj) {
            this.name = name;
            this.value = obj;
        }

        private String name;
        
        private Object value;

        private ValueType type;

        @Override
        public String toString() {
            String splitComp = "";
            if(name != null && value != null) {
                splitComp = ": ";
            }
            return (name == null ? "" : name) + splitComp + (value == null ? "" : value + " ["+value.getClass().getSimpleName()+"]");
        }

        DataNode() {
        }
    }

    //<editor-fold defaultstate="collapsed" desc="Enums">
    private enum ControlCharacter {

        NULL(0),
        HEADING_START(1),
        TEXT_START(2),
        EXTENDED(3),
        DEPOTS(7),
        TERMINATOR(8);

        ControlCharacter(int i) {
            this.i = i;
        }

        private int i;

        public int ID() {
            return i;
        }

        public static int get(ControlCharacter c) {
            return c.ID();
        }

        public static String get(int i) {
            ControlCharacter[] search = ControlCharacter.values();
            for(int s = 0; s < search.length; s++) {
                if(search[s].ID() == i) {
                    return search[s].name();
                }
            }
            return "UNKNOWN (" + i + ")";
        }
    }

    private enum Universe {

        INVALID(0),
        PUBLIC(1),
        BETA(2),
        INTERNAL(3),
        DEV(4);

        Universe(int i) {
            this.id = i;
        }

        private int id;

        public int ID() {
            return id;
        }

        public static String get(int i) {
            Universe[] search = Universe.values();
            for(int s = 0; s < search.length; s++) {
                if(search[s].ID() == i) {
                    return search[s].name();
                }
            }
            return "UNKNOWN (" + i + ")";
        }
    }

    private enum AppInfoState {

        UNAVAILBALE(1),
        AVAILABLE(2);

        AppInfoState(int i) {
            this.id = i;
        }

        private int id;

        public int ID() {
            return id;
        }

        public static String get(int i) {
            AppInfoState[] search = AppInfoState.values();
            for(int s = 0; s < search.length; s++) {
                if(search[s].ID() == i) {
                    return search[s].name();
                }
            }
            return "UNKNOWN (" + i + ")";
        }
    }

    /**
     * Can be found in steamclient native library, EAppInfoSection
     */
    private enum Section {

        UNKNOWN(0),
        ALL(1),
        COMMON(2),
        EXTENDED(3),
        CONFIG(4),
        STATS(5),
        INSTALL(6),
        DEPOTS(7),
        VAC(8),
        DRM(9),
        UFS(10),
        OGG(11),
        ITEMS(12),
        POLICIES(13),
        SYSREQS(14),
        COMMUNITY(15),
        SERVERONLY(16),
        SERVERANDWGONLY(17);

        Section(int i) {
            this.id = i;
        }

        private int id;

        public int ID() {
            return id;
        }

        public static String get(int i) {
            Section[] search = Section.values();
            for(int s = 0; s < search.length; s++) {
                if(search[s].ID() == i) {
                    return search[s].name();
                }
            }
            return "UNKNOWN (" + i + ")";
        }
    }

    private enum SteamAppState {

        Invalid(0x00000000),
        Uninstalled(0x00000001),
        UpdateRequired(0x00000002),
        FullyInstalled(0x00000004),
        Encrypted(0x00000008),
        Locked(0x00000010),
        FilesMissing(0x00000020),
        AppRunning(0x00000040),
        FilesCorrupt(0x00000080),
        UpdateRunning(0x00000100),
        UpdatePaused(0x00000200),
        UpdateStarting(0x00000400),
        Uninstalling(0x00000800),
        Reconfiguring(0x00001000),
        Preallocating(0x00002000),
        Downloading(0x00004000),
        Staging(0x00008000),
        Comitting(0x00010000),
        Validating(0x00020000),
        UpdateStopping(0x00040000);

        SteamAppState(int i) {
            this.id = i;
        }

        private int id;

        public int ID() {
            return id;
        }

        public static String get(int i) {
            for(SteamAppState s : SteamAppState.values()) {
                if(s.ID() == i) {
                    return s.name();
                }
            }
            return "UNKNOWN (" + i + ")";
        }
    };

    private enum AppInfoSectionPropagationType {

        Invalid(0),
        Public(1),
        OwnersOnly(2),
        ServerOnly(3),
        ClientOnly(4),
        ServerAndWGOnly(5);

        AppInfoSectionPropagationType(int i) {
            this.id = i;
        }

        private int id;

        public int ID() {
            return id;
        }

        public static String get(int i) {
            for(AppInfoSectionPropagationType s : AppInfoSectionPropagationType.values()) {
                if(s.ID() == i) {
                    return s.name();
                }
            }
            return "UNKNOWN (" + i + ")";
        }
    };

    private enum ValueType {

        TYPE_NONE(0),
        TYPE_STRING(1),
        TYPE_INT(2),
        TYPE_FLOAT(3),
        TYPE_PTR(4),
        TYPE_WSTRING(5),
        TYPE_COLOR(6),
        TYPE_UINT64(7),
        TYPE_NUMTYPES(8);

        ValueType(int i) {
            this.id = i;
        }

        private int id;

        public int ID() {
            return id;
        }

        public static ValueType get(int i) {
            for(ValueType s : ValueType.values()) {
                if(s.ID() == i) {
                    return s;
                }
            }
            LOG.log(Level.WARNING, "No {0} for {1}", new Object[]{ValueType.class.getSimpleName(), i});
            return null;
        }

        public static String getName(int i) {
            for(ValueType s : ValueType.values()) {
                if(s.ID() == i) {
                    return s.name();
                }
            }
            return "UNKNOWN (" + i + ")";
        }
    };
    //</editor-fold>

    private static final Logger LOG = Logger.getLogger(BVDF.class.getName());

    private BVDF() {
    }
}