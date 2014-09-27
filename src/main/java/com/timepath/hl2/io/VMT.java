package com.timepath.hl2.io;

import com.timepath.hl2.io.image.VTF;
import com.timepath.steam.io.VDFNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class VMT {

    @NotNull
    public static VMTNode load(@NotNull File f) throws IOException {
        return load(new FileInputStream(f));
    }

    @NotNull
    public static VMTNode load(@NotNull InputStream is) throws IOException {
        return load(is, StandardCharsets.UTF_8);
    }

    @NotNull
    public static VMTNode load(@NotNull InputStream is, @NotNull Charset c) throws IOException {
        return new VMTNode(is, c);
    }

    @NotNull
    public static VMTNode load(@NotNull File f, @NotNull Charset c) throws IOException {
        return load(new FileInputStream(f), c);
    }

    public static class VMTNode extends VDFNode {

        private static final Logger LOG = Logger.getLogger(VMTNode.class.getName());
        public final VDFNode root;

        public VMTNode(@NotNull final InputStream is, @NotNull final Charset c) throws IOException {
            super(is, c);
            root = getNodes().get(0);
            String shader = String.valueOf(root.getCustom());
            LOG.log(Level.INFO, "Shader: {0}", shader);
        }

        @Nullable
        public VTF getTexture() throws IOException {
            return VTF.load((String) root.getValue("$basetexture"));
        }
    }
}
