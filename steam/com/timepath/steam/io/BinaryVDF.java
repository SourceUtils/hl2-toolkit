package com.timepath.steam.io;

import com.timepath.DataUtils;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.TimeZone;
import java.util.logging.Logger;

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

    private static final byte nul = 0;

    private static final byte headingStart = 1;

    private static final byte textStart = 2;

    private static final byte extended = 3;

    private static final byte depots = 7;

    private static final byte terminator = 8;

    public BinaryVDF(String location) throws FileNotFoundException, IOException {
        RandomAccessFile rf = new RandomAccessFile(location, "r");
        byte[] magic = new byte[4];
        rf.read(magic);
        ArrayList<String> stuff = new ArrayList<String>();
//        System.out.println(magic[2]);
        if(magic[2] == 0x56) {
            /**
             * 0 invalid
             * 1 public
             * 2 beta
             * 3 internal
             * 4 dev
             */
            int universe = DataUtils.readULEInt(rf);
            System.out.println("Universe: " + universe);
            if(magic[1] == 0x44) {
                /**
                 * sections
                 * 0 unknown
                 * 1 all
                 * 2 common
                 * 3 extended
                 * 4 config
                 * 5 stats
                 * 6 install
                 * 7 depots
                 * 8 vac
                 * 9 drm
                 * 10 ufs
                 * 11 ogg
                 * 12 items
                 * 13 policies
                 * 14 sysreqs
                 * 15 community
                 *
                 */
//                ty = AppInfoFile
                for(;;) {
                    int appID = DataUtils.readULEInt(rf);
                    if(appID == 0) {
                        break;
                    }
                    int size = DataUtils.readULEInt(rf);
                    int dummy1 = DataUtils.readULEInt(rf);
                    long lastUpdated = DataUtils.readULEInt(rf);
                    DateFormat df = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
                    df.setTimeZone(TimeZone.getTimeZone("GMT"));
                    System.out.println(appID + " : " + df.format(new Date(lastUpdated * 1000)) + " : " + lastUpdated);
                    long dummy2 = (DataUtils.readULEInt(rf) << 32) + DataUtils.readULEInt(rf);
                    byte[] dummy3 = DataUtils.readBytes(rf, 20);
                    long changeNumber = (DataUtils.readULEInt(rf) << 32) + DataUtils.readULEInt(rf);
                    for(;;) {
                        byte section_number = DataUtils.readByte(rf); // enum EAppInfoSection
                        if(section_number == 0x00) // end of section data
                        {
                            break;
                        }
                        byte[] section_data; // section data as written by KeyValues::WriteAsBinary() (see KeyValues.cpp in Source SDK)
                        for(;;) {
                            if(rf.readByte() == 8) {
                                break;
                            }
                        }
                    }
                }
            } else if(magic[1] == 0x55) {
//                ty = PackageInfoFile
                for(;;) {
                    int appID = DataUtils.readULEInt(rf);
                    if(appID == 0xFFFFFFFF) { // -1
                        break;
                    }
                    byte[] unknown1 = new byte[20];
                    for(int i = 0; i < unknown1.length; i++) {
                        unknown1[i] = rf.readByte();
                    }
                    int unknown2 = DataUtils.readULEInt(rf);
//                    byte[] data;
                    parse(rf, stuff, 0, -1);
                }
            } else {
//                raise Exception("Unknown file type!")
            }
        } else {
//            ty = VDFFile
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
            if(b == headingStart) {
                ArrayList<String> d2 = new ArrayList<String>();
                String heading = parse(rf, d2, nul, 2);
//                System.out.println(heading);
//                StringBuilder sb = new StringBuilder();
//                sb.append(heading);
//                for(int i = 0; i < d2.size(); i++) {
//                    sb.append("\t").append(d2.get(i)).append("\n");
//                }
//                System.out.append(sb.toString());
            } else if(b == textStart) {
                String text = parse(rf, data, terminator, 2);
                System.out.println(p + "\n" + "=" + text + "\n");
                data.add(text);
            } else if(b == extended) {
                String text = parse(rf, data, nul, 2);
                System.out.println(p + "\n" + ">" + text + "\n");
                data.add(text);
            } else if(b == depots) {
                String text = parse(rf, data, nul, 2);
                System.out.println(p + "\n" + ">" + text + "\n");
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
//        return super.toString();
        return "";
    }

    private static final Logger LOG = Logger.getLogger(BinaryVDF.class.getName());
}