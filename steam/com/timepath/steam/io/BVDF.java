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
public class BVDF {
    
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
    
    private ArrayList<String> stuff = new ArrayList<String>();
    
    public static void analyze(File f, DefaultMutableTreeNode root) {
        try {
            BVDF b = new BVDF(f.getPath());
            for(int i = 0; i < b.stuff.size(); i++) {
                root.add(new DefaultMutableTreeNode(b.stuff.get(i)));
            }
        } catch (FileNotFoundException ex) {
            Logger.getLogger(BVDF.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(BVDF.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public BVDF(String location) throws FileNotFoundException, IOException {
        RandomAccessFile rf = new RandomAccessFile(location, "r");
        byte[] magic = DataUtils.readBytes(rf, 4);
        if(magic[2] != 0x56) {
            LOG.log(Level.WARNING, "Invalid header : {0}, offending byte: {2}", new Object[]{new String(magic), magic[2]});
            return;
        }
        int universe = DataUtils.readULEInt(rf);
        stuff.add("Info:");
        stuff.add("  Universe: " + Universe.get(universe));
        //<editor-fold defaultstate="collapsed" desc="AppInfo">
        if(magic[1] == 0x44) {
            for(;;) {
                int appID = DataUtils.readULEInt(rf);
                if(appID == 0x0000 || appID == 0) {
                    break;
                }
                int size = DataUtils.readULEInt(rf); // skip this many bytes to reach the next entry
                long start = rf.getFilePointer();
                int appInfoState = DataUtils.readULEInt(rf);
                if(appInfoState != AppInfoState.AVAILABLE.ID()) {
                    LOG.log(Level.INFO, "{0} {1}", new Object[]{appID, AppInfoState.get(appInfoState)});
                }
                long lastUpdated = DataUtils.readULEInt(rf);
                DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
                df.setTimeZone(TimeZone.getTimeZone("GMT"));
                String formattedDate = df.format(new Date(lastUpdated * 1000));
                //<editor-fold defaultstate="collapsed" desc="Padding">
                long dummy1 = (DataUtils.readULEInt(rf) << 32) + DataUtils.readULEInt(rf);
                if(dummy1 != 0) {
                    LOG.log(Level.INFO, "Dummy2 ({0}) not 0 for {1}", new Object[]{dummy1, appID});
                }
                byte[] dummy2 = DataUtils.readBytes(rf, 20);
                //</editor-fold>
                long changeNumber = (DataUtils.readULEInt(rf)); // perhaps this isn't part of the header, but child nodes?
                
                stuff.add("" + appID);
                stuff.add("  Start: " + Long.toString(start - 8).toUpperCase());
                stuff.add("    Length: " + (size + 8));
                stuff.add("  AppInfoState: " + AppInfoState.get(appInfoState));
                stuff.add("  Updated: " + formattedDate);
                stuff.add("  ChangeNumber: " + changeNumber);
                stuff.add("  Data:");
                
                parse(rf, stuff, 0, -1);
                
//                for(;;) {
//                    byte section = DataUtils.readByte(rf);
//                    stuff.add("      Section: " + Section.get(section));
////                    if(section == 0x00) {
//                        break;
////                    }
////                    byte[] section_data; // section data as written by KeyValues::WriteAsBinary() (see KeyValues.cpp in Source SDK)
////                    for(;;) {
////                        if(rf.readByte() == ControlCharacter.TERMINATOR.ID()) {
////                            break;
////                        }
////                    }
//                }
                long end = rf.getFilePointer();
                if(end != (start + size)) {
                    LOG.log(Level.FINE, "Read: {0}, not {1}", new Object[]{(end - start), size});
                    rf.seek(start + size);
                }
            }
            //</editor-fold>
        //<editor-fold defaultstate="collapsed" desc="PackageInfo">
        } else if(magic[1] == 0x55) {
            for(;;) {
                int appID = DataUtils.readULEInt(rf);
                if(appID == 0xFFFFFFFF || appID == -1) {
                    break;
                }
                byte[] unknown1 = DataUtils.readBytes(rf, 20);
                int unknown2 = DataUtils.readULEInt(rf);
                
                parse(rf, stuff, 0, -1);
            }
            //</editor-fold>
        } else {
//            throw new Exception("Unknown file type!");
        }
    }

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
    private String parse(RandomAccessFile rf, ArrayList<String> data, int end, int timesEndSeen) throws IOException {
//        if(true) {
//            return "";
//        }
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        while(rf.getFilePointer() < rf.length()) {
            String p = Long.toHexString(rf.getFilePointer());
            byte b = rf.readByte();
            
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
                LOG.log(Level.FINE,"{0}" + "\n" + "={1}\n", new Object[]{p, text});
                data.add(text);
            } else if(b == ControlCharacter.EXTENDED.ID()) {
                String text = parse(rf, data, ControlCharacter.NULL.ID(), 2);
                LOG.log(Level.FINE,"{0}" + "\n" + ">{1}\n", new Object[]{p, text});
                data.add(text);
            } else if(b == ControlCharacter.DEPOTS.ID()) {
                String text = parse(rf, data, ControlCharacter.NULL.ID(), 2);
                LOG.log(Level.FINE,"{0}" + "\n" + ">{1}\n", new Object[]{p, text});
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

    private static final Logger LOG = Logger.getLogger(BVDF.class.getName());
}