package com.timepath.hl2.io.test;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.input.ChaseCamera;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeCanvasContext;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.util.BufferUtils;
import com.timepath.hl2.io.MDL;
import com.timepath.hl2.io.VTF;
import java.awt.Canvas;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;
import javax.swing.JFrame;

/**
 *
 * @author timepath
 */
public class MDLTest extends SimpleApplication {

    private static final Logger LOG = Logger.getLogger(MDLTest.class.getName());

    public static void main(String... args) {
        final MDLTest app = new MDLTest();
        app.setPauseOnLostFocus(false);
        app.setShowSettings(true);
        AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL_ANY);
        app.setSettings(settings);
        app.createCanvas();
        app.startCanvas(true);
        JmeCanvasContext context = (JmeCanvasContext) app.getContext();
        Canvas canvas = context.getCanvas();
        canvas.setSize(settings.getWidth(), settings.getHeight());

        JFrame frame = new JFrame("Test");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                app.stop();
            }
        });
        frame.getContentPane().add(canvas);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void registerLoaders() {
        this.assetManager.registerLoader(MDLLoader.class, "mdl");
        this.assetManager.registerLoader(VTFLoader.class, "vtf");
//        this.assetManager.registerLoader(BSPLoader.class, "bsp");
    }

    @Override
    public void simpleInitApp() {
        registerLoaders();
        this.setDisplayStatView(false);
        this.setDisplayFps(false);
        initInput();
        Texture tex = assetManager.loadTexture("tf/materials/models/weapons/v_bonesaw/v_bonesaw.vtf");
        Geometry mdl = (Geometry) assetManager.loadModel("tf/models/weapons/w_models/w_bonesaw.mdl");
        Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        mat.setColor("Color", ColorRGBA.White);
        mat.setTexture("ColorMap", tex);
        mdl.setMaterial(mat);
        rootNode.attachChild(mdl);
    }

    private void initInput() {
        flyCam.setDragToRotate(true);
        flyCam.setRotationSpeed(-1);
        flyCam.setEnabled(false);

        ChaseCamera chaseCam = new ChaseCamera(cam, rootNode, inputManager);
        chaseCam.setInvertHorizontalAxis(false);
        chaseCam.setInvertVerticalAxis(true);
        chaseCam.setSmoothMotion(false);
        chaseCam.setRotationSpeed(3);
        chaseCam.setMinVerticalRotation(-179);
        chaseCam.setMaxVerticalRotation(179);
        chaseCam.setDefaultHorizontalRotation(-FastMath.HALF_PI);
        chaseCam.setMaxDistance(100);
    }

    public static class MDLLoader implements AssetLoader {

        private static final Logger LOG = Logger.getLogger(MDLLoader.class.getName());

        @Override
        public Object load(AssetInfo info) throws IOException {
            String name = "mdl/" + info.getKey().getName().substring(0, info.getKey().getName().length() - 4);
            System.out.println(new File(name));
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
            if (indexes != null) {
                mesh.setBuffer(VertexBuffer.Type.Index, 1, BufferUtils.createIntBuffer(indexes));
            }

            mesh.setStatic();
            mesh.updateBound();
            mesh.updateCounts();

            Geometry geom = new Geometry(name + "-geom", mesh);
            return geom;
            //</editor-fold>
        }
    }

    public static class VTFLoader implements AssetLoader {

        private static final Logger LOG = Logger.getLogger(VTFLoader.class.getName());

        @Override
        public Object load(AssetInfo info) throws IOException {
            File f = new File("mdl/" + info.getKey().getName());
            LOG.info(f.toString());
            VTF v = VTF.load(f);
            BufferedImage t = (BufferedImage) v.getThumbImage();
            t = (BufferedImage) v.getImage(0);

            byte[] rawData = new byte[t.getWidth() * t.getHeight() * 4];

            int idx = 0;
            for (int x = 0; x < t.getWidth(); x++) {
                for (int y = 0; y < t.getHeight(); y++) {
                    int d = t.getRGB(x, y);
                    rawData[idx++] = (byte) ((d >> 16) & 0xFF);
                    rawData[idx++] = (byte) ((d >> 8) & 0xFF);
                    rawData[idx++] = (byte) ((d >> 0) & 0xFF);
                }
            }

            ByteBuffer scratch = BufferUtils.createByteBuffer(rawData.length);
            scratch.clear();
            scratch.put(rawData);
            scratch.rewind();
            // Create the Image object
            Image textureImage = new Image();
            textureImage.setFormat(Image.Format.RGB8);
            textureImage.setWidth(t.getWidth());
            textureImage.setHeight(t.getHeight());
            textureImage.setData(scratch);
            return textureImage;
        }
    }
}
