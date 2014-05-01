package com.timepath.hl2.io;

import com.timepath.hl2.io.image.VTF;
import com.timepath.io.utils.Savable;
import com.timepath.steam.io.VDF1;
import com.timepath.steam.io.util.VDFNode1;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author TimePath
 */
public class VMT extends VDF1 implements Savable {

    private static final Logger LOG = Logger.getLogger(VMT.class.getName());

    private static void analyze(VDFNode1 data) {
        String shader = data.getKey();
        LOG.log(Level.INFO, "Shader: {0}", shader);
    }

    public VMT() {
    }

    @Override
    public void readExternal(InputStream in, String encoding) {
        super.readExternal(in, encoding);
        analyze(root);
    }

    public VTF getTexture() throws IOException {
        return VTF.load(this.root.get(0).get("$basetexture").getValue());
    }

}
