package com.timepath.steam.io;

import com.timepath.DataUtils;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 * See also:
 * https://github.com/harvimt/steam_launcher/blob/master/binvdf.py
 * https://github.com/barneygale/bvdf/blob/master/bvdf.py
 * https://github.com/DHager/hl2parse
 * http://webcache.googleusercontent.com/search?q=cache:pKubZAM3J3QJ:cs.rin.ru/forum/viewtopic.php%3Ff%3D20%26t%3D62438+&cd=1&hl=en&ct=clnk&gl=au
 *
 * @author timepath
 */
public class BinaryVDF {
    
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
            return "UNKNOWN";
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
            return "UNKNOWN";
        }
        
    }
    
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
        COMMUNITY(15);
        
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
            return "UNKNOWN";
        }
    }
    
    private ArrayList<String> stuff = new ArrayList<String>();
    
    public static void analyze(File f, DefaultMutableTreeNode root) {
        try {
            BinaryVDF b = new BinaryVDF(f.getPath());
            for(int i = 0; i < b.stuff.size(); i++) {
                root.add(new DefaultMutableTreeNode(b.stuff.get(i)));
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(BinaryVDF.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BinaryVDF.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public BinaryVDF(String location) throws FileNotFoundException, IOException {
        RandomAccessFile rf = new RandomAccessFile(location, "r");
        byte[] magic = DataUtils.readBytes(rf, 4);
        if(magic[2] != 0x56) {
            LOG.log(Level.WARNING, "Invalid header : {0}, offending byte: {2}", new Object[]{new String(magic), magic[2]});
            return;
        }
        int universe = DataUtils.readULEInt(rf);
        LOG.log(Level.INFO, "Universe: {0}", Universe.get(universe));
        if(magic[1] == 0x44) { // AppInfo
            for(;;) {
                int appID = DataUtils.readULEInt(rf);
                if(appID == 0) {
                    break;
                }
                int size = DataUtils.readULEInt(rf);
                int dummy1 = DataUtils.readULEInt(rf);
                if(dummy1 != 2) {
                    LOG.log(Level.INFO, "Dummy1 ({0}) not 2 for {1}", new Object[]{dummy1, appID});
                }
                long lastUpdated = DataUtils.readULEInt(rf);
                DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
                df.setTimeZone(TimeZone.getTimeZone("GMT"));
                String fd = df.format(new Date(lastUpdated * 1000));
//                    LOG.log(Level.INFO, "{0} : {1} : {2}", new Object[]{appID, fd, lastUpdated});
                long dummy2 = (DataUtils.readULEInt(rf) << 32) + DataUtils.readULEInt(rf);
                if(dummy2 != 0) {
                    LOG.log(Level.INFO, "Dummy2 ({0}) not 0 for {1}", new Object[]{dummy2, appID});
                }
                byte[] dummy3 = DataUtils.readBytes(rf, 20);
                long changeNumber = (DataUtils.readULEInt(rf) << 32) + DataUtils.readULEInt(rf);
                long start = rf.getFilePointer();
//                for(;;) {
                    byte section = DataUtils.readByte(rf); // enum EAppInfoSection
                    LOG.log(Level.FINE, "Section #: {0}", section);
                    stuff.add("\tSection: " + section);
                    rf.seek(start + size);// - 53);
//                    if(section == 0x00) {// end of section data
//                        break;
//                    }
//                    break;
//                    byte[] section_data; // section data as written by KeyValues::WriteAsBinary() (see KeyValues.cpp in Source SDK)
//                    for(;;) {
//                        if(rf.readByte() == ControlCharacter.TERMINATOR.ID()) {
//                            break;
//                        }
//                    }
//                }
                long end = rf.getFilePointer();
                if((end - start) != size) {
                    LOG.log(Level.INFO, "Read: {0}, not {1}", new Object[]{(end - start), size});
                }
                stuff.add(appID + ", " + fd + ", " + new String(dummy3) + ", " + changeNumber);
            }
        } else if(magic[1] == 0x55) { // PackageInfo
            for(;;) {
                int appID = DataUtils.readULEInt(rf);
                if(appID == 0xFFFFFFFF) { // -1
                    break;
                }
                byte[] unknown1 = DataUtils.readBytes(rf, 20);
                int unknown2 = DataUtils.readULEInt(rf);
//                    byte[] data;
                parse(rf, stuff, 0, -1);
            }
        } else {
//                raise Exception("Unknown file type!")
        }
    }

    /**
     * http://cdr.xpaw.ru/app/5/#section_info
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
    private String parse(RandomAccessFile rf, ArrayList<String> data, int end, int timesEndSeen) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        while(rf.getFilePointer() < rf.length()) {
            byte b = rf.readByte();
            String p = Long.toHexString(rf.getFilePointer());
            if(b == ControlCharacter.HEADING_START.ID()) {
                ArrayList<String> d2 = new ArrayList<String>();
                String heading = parse(rf, d2, ControlCharacter.NULL.ID(), 2);
//                System.out.println(heading);
//                StringBuilder sb = new StringBuilder();
//                sb.append(heading);
//                for(int i = 0; i < d2.size(); i++) {
//                    sb.append("\t").append(d2.get(i)).append("\n");
//                }
//                System.out.append(sb.toString());
            } else if(b == ControlCharacter.TEXT_START.ID()) {
                String text = parse(rf, data, ControlCharacter.TERMINATOR.ID(), 2);
                LOG.log(Level.INFO,"{0}" + "\n" + "={1}\n", new Object[]{p, text});
                data.add(text);
            } else if(b == ControlCharacter.EXTENDED.ID()) {
                String text = parse(rf, data, ControlCharacter.NULL.ID(), 2);
                LOG.log(Level.INFO,"{0}" + "\n" + ">{1}\n", new Object[]{p, text});
                data.add(text);
            } else if(b == ControlCharacter.DEPOTS.ID()) {
                String text = parse(rf, data, ControlCharacter.NULL.ID(), 2);
                LOG.log(Level.INFO,"{0}" + "\n" + ">{1}\n", new Object[]{p, text});
                data.add(text);
            } else {
                buf.write(b);
                if(b == end) {
                    timesEndSeen--;
                }
                if(timesEndSeen == 0) {
                    break;
                }
            }
        }
        byte[] bytes = buf.toByteArray();
//        System.out.println(Arrays.toString(bytes));
        String str = new String(bytes);
        if(bytes.length > 1) {
            if(bytes[bytes.length - 1] == 0) {
                str = str.substring(0, str.length() - 1);
            }
            if(bytes[0] == 0) {
                str = str.substring(1, str.length());
            }
        }
//        str = str.replaceAll("\0", " ");
        return str;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    private static final Logger LOG = Logger.getLogger(BinaryVDF.class.getName());
}