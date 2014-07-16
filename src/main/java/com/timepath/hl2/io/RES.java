package com.timepath.hl2.io;

import com.timepath.hl2.io.util.Element;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * If there are multiple values with platform tags, all the values become the
 * last loaded value tag, but only if the variable is recognized Some tags:
 * $WINDOWS $WIN32 $X360 $POSIX $OSX $LINUX
 *
 * @author TimePath
 * @see <a>https://code.google.com/p/hl2sb-src/source/browse/#svn%2Ftrunk%2Fsrc%2Fgame%2Fclient%2Fgame_controls</a>
 */
public class RES {

    public static Element load(File f) throws IOException {
        return load(new FileInputStream(f));
    }

    public static Element load(InputStream is) throws IOException {
        return load(is, StandardCharsets.UTF_8);
    }

    public static Element load(InputStream is, Charset c) throws IOException {
        return new Element(is, c);
    }

    public static Element load(File f, Charset c) throws IOException {
        return load(new FileInputStream(f), c);
    }
}
