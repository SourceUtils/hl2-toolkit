package com.timepath;

import java.io.File;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class FileUtils {

    private static final Logger LOG = Logger.getLogger(FileUtils.class.getName());

    private FileUtils() {
    }

    public static void chmod777(File file) {
        file.setReadable(true, false);
        file.setWritable(true, false);
        file.setExecutable(true, false);
    }

    /*
     * Get the extension of a file.
     */
    public static String extension(String s) {
        String ext = null;
        int i = s.lastIndexOf('.');

        if(i > 0 && i < s.length() - 1) {
            ext = s.substring(i + 1).toLowerCase();
        }
        return ext;
    }

    public static String extension(File f) {
        return extension(f.getName());
    }
}
