package com.timepath.hl2.io.test;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.input.ChaseCamera;
import com.jme3.material.Material;
import com.jme3.math.FastMath;
import com.jme3.math.Vector2f;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeCanvasContext;
import com.jme3.texture.Image;
import com.jme3.texture.Texture;
import com.jme3.texture.Texture2D;
import com.jme3.util.BufferUtils;
import com.timepath.hl2.io.StudioModel;
import com.timepath.hl2.io.VTF;
import com.timepath.plaf.x.filechooser.NativeFileChooser;
import java.awt.Canvas;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

/**
 *
 * @author timepath
 */
public class MDLTest extends SimpleApplication {

    private static final Logger LOG = Logger.getLogger(MDLTest.class.getName());

    public static void main(String... args) {
        Logger.getLogger("com.jme3").setLevel(Level.SEVERE);
        final MDLTest app = new MDLTest();
        app.setPauseOnLostFocus(false);
        app.setShowSettings(true);
        AppSettings settings = new AppSettings(true);
        settings.setRenderer(AppSettings.LWJGL_OPENGL_ANY);
        settings.setAudioRenderer(null);
        app.setSettings(settings);
        app.createCanvas();
        app.startCanvas(true);
        JmeCanvasContext context = (JmeCanvasContext) app.getContext();
        Canvas canvas = context.getCanvas();
        canvas.setSize(settings.getWidth(), settings.getHeight());

        JFrame frame = new JFrame("Test");
        JMenuBar mb = new JMenuBar();
        frame.setJMenuBar(mb);
        JMenu fileMenu = new JMenu("File");
        mb.add(fileMenu);
        JMenuItem open = new JMenuItem("Open");
        open.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    File[] f = new NativeFileChooser().setTitle("Select model").choose();
                    if(f == null) {
                        return;
                    }
                    app.loadModel(f[0]);
                } catch(IOException ex) {
                    Logger.getLogger(MDLTest.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        fileMenu.add(open);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                app.stop(true);
            }
        });
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
        Logger.getLogger("com.jme3").setLevel(Level.INFO);
    }

    private void registerLoaders() {
//        this.assetManager.registerLocator("/", FileLocator.class);
//        this.assetManager.registerLoader(MDLLoader.class, "mdl");
//        this.assetManager.registerLoader(VTFLoader.class, "vtf");
//        this.assetManager.registerLoader(BSPLoader.class, "bsp");
    }

    @Override
    public void simpleInitApp() {
        registerLoaders();
        this.setDisplayStatView(false);
        this.setDisplayFps(false);
        initInput();
    }

    private void initInput() {      
        rootNode.rotateUpTo(Vector3f.UNIT_Z.negate());
        flyCam.setDragToRotate(true);
        flyCam.setEnabled(false);

        ChaseCamera chaseCam = new ChaseCamera(cam, rootNode, inputManager);
        chaseCam.setInvertHorizontalAxis(false);
        chaseCam.setInvertVerticalAxis(true);
        chaseCam.setSmoothMotion(false);
        chaseCam.setRotationSpeed(3);
        chaseCam.setMinVerticalRotation(-179);
        chaseCam.setMaxVerticalRotation(179);
        chaseCam.setDefaultHorizontalRotation(FastMath.HALF_PI);
        chaseCam.setDefaultVerticalRotation(0);
        chaseCam.setMaxDistance(100);
    }

    public static class MDLLoader implements AssetLoader {

        public MDLLoader() {
        }

        private static final Logger LOG = Logger.getLogger(MDLLoader.class.getName());

        @Override
        public Object load(AssetInfo info) throws IOException {
            String name = "mdl/" + info.getKey().getName().substring(0,
                                                                     info.getKey().getName().length() - 4);
            System.out.println(new File(name));

            return load(name);
        }

        public Object load(String name) throws IOException {
            StudioModel m = StudioModel.load(name);

            float[] vertices = m.getVertices();
            float[] normals = m.getNormals();
            float[] tangents = m.getTangents();
            float[] uv = m.getTextureCoordinates();
            int[] indexes = m.getIndices();

            Mesh mesh = new Mesh();
//            mesh.setMode(Mesh.Mode.Points);
//            mesh.setPointSize(4);

            FloatBuffer posBuf = BufferUtils.createFloatBuffer(vertices.length);
            for(int i = 0; i < vertices.length / 3; i++) {
                posBuf.put(vertices[i * 3 + 0]);
                posBuf.put(vertices[i * 3 + 1]);
                posBuf.put(vertices[i * 3 + 2]);
            }
            mesh.setBuffer(VertexBuffer.Type.Position, 3, posBuf);

//            FloatBuffer normBuf = BufferUtils.createFloatBuffer(normals.length);
//            for(int i = 0; i < normals.length / 3; i++) {
//                normBuf.put(normals[i * 3 + 0]);
//                normBuf.put(normals[i * 3 + 1]);
//                normBuf.put(normals[i * 3 + 2]);
//            }
//            mesh.setBuffer(VertexBuffer.Type.Normal, 3, normBuf);
            
            FloatBuffer texBuf = BufferUtils.createFloatBuffer(uv.length);
            for(int i = 0; i < uv.length / 2; i++) {
                texBuf.put(uv[i * 2 + 1]);
                texBuf.put(uv[i * 2 + 0]);
            }
            mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, texBuf);

//            mesh.scaleTextureCoordinates(Vector2f.UNIT_XY.mult(0.6f));
//            
//            FloatBuffer tanBuf = BufferUtils.createFloatBuffer(tangents.length);
//            for(int i = 0; i < tangents.length / 4; i++) {
//                tanBuf.put(tangents[i * 4 + 0]);
//                tanBuf.put(tangents[i * 4 + 1]);
//                tanBuf.put(tangents[i * 4 + 2]);
//                tanBuf.put(tangents[i * 4 + 3]);
//            }
//            mesh.setBuffer(VertexBuffer.Type.Tangent, 4, tanBuf);


            if(indexes != null) {
                IntBuffer idxBuf = BufferUtils.createIntBuffer(indexes.length);
                for(int i = 0; i < indexes.length / 3; i++) {
                    idxBuf.put(indexes[i * 3 + 2]);
                    idxBuf.put(indexes[i * 3 + 1]);
                    idxBuf.put(indexes[i * 3 + 0]);
                }
                mesh.setBuffer(VertexBuffer.Type.Index, 1, idxBuf);
            }

            mesh.setStatic();
            mesh.updateBound();
            mesh.updateCounts();

            Geometry geom = new Geometry(name + "-geom", mesh);
            return geom;
        }

    }

    public static class VTFLoader implements AssetLoader {

        public VTFLoader() {
        }

        private static final Logger LOG = Logger.getLogger(VTFLoader.class.getName());

        @Override
        public Object load(AssetInfo info) throws IOException {
            File f = new File("mdl/" + info.getKey().getName());
            LOG.info(f.toString());

            return load(f.getPath());
        }

        public Object load(String f) throws IOException {
            VTF v = VTF.load(new FileInputStream(f));
            BufferedImage src = (BufferedImage) v.getImage(v.mipCount - 1);

            AffineTransform tx = AffineTransform.getScaleInstance(-1, -1);
            tx.translate(-src.getWidth(), -src.getHeight());
            AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
            BufferedImage i = src;//op.filter(src, null);

            byte[] rawData = new byte[i.getWidth() * i.getHeight() * 4];

            int idx = 0;
            for(int x = 0; x < i.getWidth(); x++) {
                for(int y = 0; y < i.getHeight(); y++) {
                    int d = i.getRGB(x, y);
                    rawData[idx++] = (byte) ((d >> 16) & 0xFF);
                    rawData[idx++] = (byte) ((d >> 8) & 0xFF);
                    rawData[idx++] = (byte) (d & 0xFF);
                }
            }

            ByteBuffer scratch = BufferUtils.createByteBuffer(rawData.length);
            scratch.clear();
            scratch.put(rawData);
            scratch.rewind();
            // Create the Image object
            Image textureImage = new Image();
            textureImage.setFormat(Image.Format.RGB8);
            textureImage.setWidth(i.getWidth());
            textureImage.setHeight(i.getHeight());
            textureImage.setData(scratch);
            Texture2D t = new Texture2D(textureImage);
            return t;
        }

    }

    private void loadModel(final File f) {
        try {
            String stripped = f.getPath().substring(0, f.getPath().lastIndexOf('.'));
            Geometry[] mdls = {(Geometry) new MDLLoader().load(stripped),
                               new Geometry("Box", new Box(1, 1, 1))};
            int i = 0;
            for(final Geometry mdl : mdls) {
                mdl.setLocalTranslation(20 * i++, 0, 0);
                Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                if(new File(stripped + ".vtf").exists()) {
                    Texture tex = (Texture) new VTFLoader().load(stripped + ".vtf");
                    mat.setTexture("ColorMap", tex);
                }
                mdl.setMaterial(mat);
                this.enqueue(new Callable<Void>() {
                    public Void call() {
                        rootNode.attachChild(mdl);
                        return null;
                    }
                });
            }
        } catch(IOException ex) {
            Logger.getLogger(MDLTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
