package com.timepath.hl2.io;

import com.timepath.steam.io.VDF;
import static com.timepath.steam.io.VDF.analyze;
import com.timepath.steam.io.util.VDFNode;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class VMT extends VDF {
    
    public static VMT loadMaterial(File f) {
        VMT mat = new VMT();
        mat.data = new VDFNode();
        VDF.analyze(f, mat.data);
        analyze(mat.data);
        return mat;
    }

    private static final Logger LOG = Logger.getLogger(VMT.class.getName());

    private static void analyze(VDFNode data) {
        String shader = data.getKey();
        LOG.log(Level.INFO, "Shader: {0}", shader);
    }

    private VDFNode data;
    
    public VTF getTexture() throws IOException {
        return VTF.load(this.data.get(0).get("$basetexture").getValue());
    }
    
}
