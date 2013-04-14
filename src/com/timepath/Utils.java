package com.timepath;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class Utils {

    private static final Logger LOG = Logger.getLogger(Utils.class.getName());

    private Utils() {
    }

    public static String normalisePath(String str) {
        LOG.log(Level.INFO, "Normalising {0}", str);
//        try {
//            return new URI(str).normalize().getPath();
//        } catch(URISyntaxException ex) {
//            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
//        }
//        if(str.indexOf('\\') != -1) {
//            str = str.replaceAll("\\\\", File.separator);
//        }
//        if(str.indexOf('/') != -1) {
//            str = str.replaceAll("/", File.separator); // slash consistency
//        }
        while(str.indexOf(File.separator + File.separator) != -1) {
            str = str.replaceAll(File.separator + File.separator, File.separator);
        }
        if(!str.endsWith(File.separator)) {
            str += File.separator;
        }
        return str;
    }

    public static File currentFile(Class<?> c) {
        String encoded = c.getProtectionDomain().getCodeSource().getLocation().getPath();
        try {
            return new File(URLDecoder.decode(encoded, "UTF-8"));
        } catch(UnsupportedEncodingException ex) {
            Logger.getLogger(Utils.class.getName()).log(Level.SEVERE, null, ex);
        }
        String ans = System.getProperty("user.dir") + File.separator;
        String cmd = System.getProperty("sun.java.command");
        int idx = cmd.lastIndexOf(File.separator);
        if(idx != -1) {
            cmd = cmd.substring(0, idx + 1);
        } else {
            cmd = "";
        }
        ans += cmd;
//        ans = normalisePath(ans);
        return new File(ans);
    }

    public static String workingDirectory(Class<?> c) {
        return currentFile(c).getParentFile().getAbsolutePath();
    }

    public static boolean isMD5(String str) {
        return str.matches("[a-fA-F0-9]{32}");
    }

    public static String takeMD5(byte[] bytes) throws NoSuchAlgorithmException {
        String md5 = "";
        MessageDigest md = MessageDigest.getInstance("MD5");
        md.update(bytes);
        byte[] b = md.digest();
        for(int i = 0; i < b.length; i++) {
            md5 += Integer.toString((b[i] & 0xFF) + 256, 16).substring(1);
        }
        return md5;
    }

    private static String selfCheck(Class<?> c) {
        String md5 = null;
        String runPath = currentFile(c).getName();
        if(runPath.endsWith(".jar")) {
            try {
                md5 = Utils.takeMD5(Utils.loadFile(new File(runPath)));
            } catch(Exception ex) {
                LOG.log(Level.SEVERE, null, ex);
            }
        }
        return md5;
    }

    public static byte[] loadFile(File f) throws FileNotFoundException, IOException {
        InputStream fis = new FileInputStream(f);
        byte[] buff = new byte[fis.available()];
        int numRead;
        int size = 0;
        for(;;) {
            numRead = fis.read(buff);
            if(numRead == -1) {
                break;
            } else {
                size += numRead;
            }
        }
        fis.close();
        byte[] ret = new byte[size];
        System.arraycopy(buff, 0, ret, 0, ret.length);
        return ret;
    }

    public static Comparator<File> ALPHA_COMPARATOR = new Comparator<File>() {
        /**
         * Alphabetically sorts directories before files ignoring case.
         */
        public int compare(File a, File b) {
            if(a.isDirectory() && !b.isDirectory()) {
                return -1;
            } else if(!a.isDirectory() && b.isDirectory()) {
                return 1;
            } else {
                return a.getName().compareToIgnoreCase(b.getName());
            }
        }
    };

}