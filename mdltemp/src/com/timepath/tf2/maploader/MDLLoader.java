package com.timepath.tf2.maploader;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;
import com.timepath.hl2.io.MDL;
import java.io.IOException;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class MDLLoader implements AssetLoader {

    private static final Logger LOG = Logger.getLogger(MDLLoader.class.getName());

    @Override
    public Object load(AssetInfo info) throws IOException {
        String name = "src/" + info.getKey().getName().substring(0, info.getKey().getName().length() - 4);
        MDL m = MDL.load(name);

        float[] vertices = m.getVertices();
        float[] normals = m.getNormals();
        float[] tangents = m.getTangents();
        float[] uv = m.getTextureCoordinates();
        int[] indexes = m.getIndices();

        //<editor-fold defaultstate="collapsed" desc="Mesh creation">
        Mesh mesh = new Mesh();
//        mesh.setMode(Mesh.Mode.Points);
        mesh.setPointSize(4);
        
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(vertices));
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
        mesh.setBuffer(VertexBuffer.Type.Tangent, 4, BufferUtils.createFloatBuffer(tangents));
        mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, BufferUtils.createFloatBuffer(uv));
        mesh.setBuffer(VertexBuffer.Type.Index, 1, BufferUtils.createIntBuffer(indexes));

        mesh.setStatic();
        mesh.updateBound();
        mesh.updateCounts();

        Geometry geom = new Geometry(name + "-geom", mesh);
        return geom;
        //</editor-fold>
    }
}
