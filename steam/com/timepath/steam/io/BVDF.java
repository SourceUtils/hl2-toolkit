package com.timepath.steam.io;

import com.timepath.DataUtils;
import java.io.ByteArrayOutputStream;
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
 * http://webcache.googleusercontent.com/search?q=cache:pKubZAM3J3QJ:cs.rin.ru/forum/viewtopic.php%3Ff%3D20%26t%3D62438+&cd=1&hl=en&ct=clnk&gl=au
 *
 * @author timepath
 */
public class BVDF {

    public static void analyze(File f, DefaultMutableTreeNode root) throws IOException {
        DataNode dn = new DataNode("root");
        ByteBuffer buf = DataUtils.mapFile(f);
        parse(buf, dn);
        root.add(dn);
//        @SuppressWarnings("unchecked")
//        Enumeration<MutableTreeNode> e = dn.children();
//        while(e.hasMoreElements()) {
//            root.add(e.nextElement());
//        }
    }

    private static void parse(ByteBuffer buf, DataNode dn) throws IOException {
        byte[] magic = new byte[4];
        buf.get(magic);
        if(magic[2] != 0x56) {
            LOG.log(Level.WARNING, "Invalid header : {0}, offending byte: {2}", new Object[]{new String(magic), magic[2]});
            return;
        }
        int universe = buf.getInt();
        dn.add(new DataNode("universe", universe));

        //<editor-fold defaultstate="collapsed" desc="AppInfo">
        if(magic[1] == 0x44) {
            for(;;) {
                int appID = buf.getInt();
                if(appID == 0x0000 || appID == 0) {
                    break;
                }
                DataNode c = new DataNode("#" + appID);
                dn.add(c);
                int size = buf.getInt(); // skip this many bytes to reach the next entry
                c.add(new DataNode("size", size));
                parseEntry(DataUtils.getSlice(buf, size), c);
            }
            //</editor-fold>
            //<editor-fold defaultstate="collapsed" desc="PackageInfo">
        } else if(magic[1] == 0x55) {
            for(;;) {
                int appID = buf.getInt();
                if(appID == 0xFFFFFFFF || appID == -1) {
                    break;
                }
                byte[] unknown1 = new byte[20];
                buf.get(unknown1);
                int unknown2 = buf.getInt();

                parse(buf, dn, 0, -1);
            }
            //</editor-fold>
        } else {
//            throw new Exception("Unknown file type!");
        }
    }

    private static void parseEntry(ByteBuffer buf, DataNode c) {
        int start = buf.position();
        int appInfoState = buf.getInt();
        c.add(new DataNode("state", appInfoState));
        long lastUpdated = buf.getInt();
        DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        String formattedDate = df.format(new Date(lastUpdated * 1000));
        DataNode lu = new DataNode("lastUpdated", lastUpdated);
        lu.add(new DataNode("formatted", formattedDate));
        c.add(lu);
        //<editor-fold defaultstate="collapsed" desc="Padding">
        long dummy1 = buf.getLong();
        c.add(new DataNode("dummy1", dummy1));
        byte[] dummy2 = new byte[20];
        buf.get(dummy2);
        c.add(new DataNode("dummy2", Arrays.toString(dummy2)));
        //</editor-fold>
        long changeNumber = (buf.getInt()); // perhaps this isn't part of the header, but child nodes?
        c.add(new DataNode("changeNumber", changeNumber));

        DataNode sections = new DataNode("Sections");
        c.add(sections);
        for(;;) {
            byte section = buf.get();
            if(section == 0x00) {
                break;
            }
            DataNode sec = new DataNode("#" + section);
            sections.add(sec);
            byte[] section_data; // section data as written by KeyValues::WriteAsBinary() (see KeyValues.cpp in Source SDK)
        }
    }

    private static String parse(ByteBuffer buf, DataNode dn, int end, int timesEndSeen) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        while(buf.remaining() > 0) {
            String p = Long.toHexString(buf.position());
            byte b = buf.get();

            if(b == ControlCharacter.HEADING_START.ID()) {
                String heading = parse(buf, dn, ControlCharacter.NULL.ID(), 2);
            } else if(b == ControlCharacter.TEXT_START.ID()) {
                String text = parse(buf, dn, ControlCharacter.TERMINATOR.ID(), 2);
                LOG.log(Level.FINE, "{0}" + "\n" + "={1}\n", new Object[]{p, text});
            } else if(b == ControlCharacter.EXTENDED.ID()) {
                String text = parse(buf, dn, ControlCharacter.NULL.ID(), 2);
                LOG.log(Level.FINE, "{0}" + "\n" + ">{1}\n", new Object[]{p, text});
            } else if(b == ControlCharacter.DEPOTS.ID()) {
                String text = parse(buf, dn, ControlCharacter.NULL.ID(), 2);
                LOG.log(Level.FINE, "{0}" + "\n" + ">{1}\n", new Object[]{p, text});
            } else {
                baos.write(b);
                if(b == end) {
                    timesEndSeen--;
                }
                if(timesEndSeen == 0) {
                    break;
                }
            }
        }
        byte[] bytes = baos.toByteArray();
        String str = new String(bytes);
        if(bytes.length > 1) {
            if(bytes[bytes.length - 1] == 0) {
                str = str.substring(0, str.length() - 1);
            }
            if(bytes[0] == 0) {
                str = str.substring(1, str.length());
            }
        }
        return str;
    }

    private static class DataNode extends DefaultMutableTreeNode {

        DataNode(Object obj) {
            this.setUserObject(obj);
        }

        DataNode(String name, Object obj) {
            this.name = name;
            this.setUserObject(obj);
        }

        private String name;

        @Override
        public String toString() {
            Object o = this.getUserObject();
            if(o == null) {
                return "unnamed";
            } else if(o instanceof String) {
                return (String) o;
            } else {
                return (name != null ? name : o.getClass().getSimpleName()) + ": " + o;
            }
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

    //</editor-fold>
    /**
     * http://cdr.xpaw.ru/app/5/#section_info
     * TODO: reverse KeyValues::WriteAsBinary()
     *
     * @param rf
     * @param data
     * @param end
     * @param timesEndSeen
     *
     * @return
     *
     * @throws IOException
     */
    private static final Logger LOG = Logger.getLogger(BVDF.class.getName());

    private BVDF() {
    }
}