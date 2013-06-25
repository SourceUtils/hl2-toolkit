package com.timepath.hl2.io;

import com.timepath.io.utils.Savable;
import com.timepath.steam.io.VDF;
import com.timepath.steam.io.util.VDFNode;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class VMT extends VDF implements Savable {

    @Override
    public void readExternal(InputStream in, String encoding) {
        super.readExternal(in, encoding);
        analyze(root);
    }

    private static final Logger LOG = Logger.getLogger(VMT.class.getName());
    
    public VMT() {
        
    }

    private static void analyze(VDFNode data) {
        String shader = data.getKey();
        LOG.log(Level.INFO, "Shader: {0}", shader);
    }
    
    public VTF getTexture() throws IOException {
        return VTF.load(this.root.get(0).get("$basetexture").getValue());
    }
    
}
