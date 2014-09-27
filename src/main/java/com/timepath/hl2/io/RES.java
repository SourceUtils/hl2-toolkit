package com.timepath.hl2.io;

import com.timepath.vgui.Element;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * @author TimePath
 */
public class RES {

    @NotNull
    public static Element load(@NotNull File f) throws IOException {
        return load(new FileInputStream(f));
    }

    @NotNull
    public static Element load(InputStream is) throws IOException {
        return load(is, StandardCharsets.UTF_8);
    }

    @NotNull
    public static Element load(InputStream is, Charset c) throws IOException {
        return new Element(is, c);
    }

    @NotNull
    public static Element load(@NotNull File f, Charset c) throws IOException {
        return load(new FileInputStream(f), c);
    }
}
