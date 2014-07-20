package com.timepath.hl2.io;

import com.timepath.hl2.io.image.VTF;
import com.timepath.steam.io.VDFNode;

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

    public static VMTNode load(File f) throws IOException {
        return load(new FileInputStream(f));
    }

    public static VMTNode load(InputStream is) throws IOException {
        return load(is, StandardCharsets.UTF_8);
    }

    public static VMTNode load(InputStream is, Charset c) throws IOException {
        return new VMTNode(is, c);
    }

    public static VMTNode load(File f, Charset c) throws IOException {
        return load(new FileInputStream(f), c);
    }

    public static class VMTNode extends VDFNode {

        private static final Logger LOG = Logger.getLogger(VMTNode.class.getName());
        public final VDFNode root;

        public VMTNode(final InputStream is, final Charset c) throws IOException {
            super(is, c);
            root = getNodes().get(0);
            String shader = String.valueOf(root.getCustom());
            LOG.log(Level.INFO, "Shader: {0}", shader);
        }

        public VTF getTexture() throws IOException {
            return VTF.load((String) root.getValue("$basetexture"));
        }
    }
}
