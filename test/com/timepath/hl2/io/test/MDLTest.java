package com.timepath.hl2.io.test;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.input.ChaseCamera;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.scene.*;
import com.jme3.scene.shape.Box;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeCanvasContext;
import com.jme3.texture.*;
import com.jme3.util.BufferUtils;
import com.timepath.hl2.io.VTF;
import com.timepath.hl2.io.studiomodel.StudioModel;
import com.timepath.plaf.x.filechooser.NativeFileChooser;
import java.awt.Canvas;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.*;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;

/**
 *
 * @author TimePath
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
        Logger.getLogger("com.jme3").setLevel(Level.INFO);
        
        final JFrame frame = new JFrame("Model test");
        JMenuBar mb = new JMenuBar();
        frame.setJMenuBar(mb);
        JMenu fileMenu = new JMenu("File");
        mb.add(fileMenu);
        JMenuItem open = new JMenuItem("Open");
        open.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                try {
                    File[] f = new NativeFileChooser().setParent(frame).setTitle("Select model").choose();
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
    }

    @Override
    public void simpleInitApp() {
        registerLoaders();
        this.setDisplayStatView(false);
        this.setDisplayFps(false);
        initInput();
    }

    private void initInput() {
        flyCam.setDragToRotate(true);
        flyCam.setEnabled(false);

        ChaseCamera chaseCam = new ChaseCamera(cam, rootNode, inputManager);
        chaseCam.setSmoothMotion(false);
        chaseCam.setRotationSpeed(3);
        chaseCam.setInvertHorizontalAxis(false);
        chaseCam.setInvertVerticalAxis(true);
        chaseCam.setMinVerticalRotation(-FastMath.HALF_PI + FastMath.ZERO_TOLERANCE);
        chaseCam.setDefaultVerticalRotation(0);
        chaseCam.setMaxVerticalRotation(FastMath.HALF_PI);
        chaseCam.setDefaultHorizontalRotation(FastMath.HALF_PI);        
        chaseCam.setDefaultDistance(10);
        chaseCam.setMaxDistance(100);
    }

    private void loadModel(final File f) {
        try {
            String stripped = f.getPath().substring(0, f.getPath().lastIndexOf('.'));
            Texture tex = (Texture) new VTFLoader().load(stripped + ".vtf");
            float s = 10;
            Geometry[] mdls = {
                (Geometry) new MDLLoader().load(stripped),
                new Geometry("Box", new Box(0.5f * s, 0.5f * s, 0.5f * s))
            };
            int i = 0;
            for(final Geometry mdl : mdls) {
                mdl.setLocalTranslation(0, 0, -20 * i++);
                Material mat = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
                if(new File(stripped + ".vtf").exists()) {
                    mat.setTexture("ColorMap", tex);
                } else {
                    mat.setColor("Color", ColorRGBA.randomColor());
                }
//                mat.getAdditionalRenderState().setFaceCullMode(FaceCullMode.Front);
//                mat.getAdditionalRenderState().setWireframe(true); 
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

    private void registerLoaders() {
//        this.assetManager.registerLocator("/", FileLocator.class);
//        this.assetManager.registerLoader(MDLLoader.class, "mdl");
//        this.assetManager.registerLoader(VTFLoader.class, "vtf");
//        this.assetManager.registerLoader(BSPLoader.class, "bsp");
    }

    private static class MDLLoader implements AssetLoader {

        private static final Logger LOG = Logger.getLogger(MDLLoader.class.getName());

        MDLLoader() {
        }

        @Override
        public Object load(AssetInfo info) throws IOException {
            String name = "mdl/" + info.getKey().getName().substring(0, info.getKey().getName().length() - 4);
            LOG.log(Level.INFO, "Loading {0}", name);
            return load(name);
        }

        public Object load(String name) throws IOException {
            StudioModel m = StudioModel.load(name);

            Mesh mesh = new Mesh();
//            mesh.setMode(Mesh.Mode.Lines);
            mesh.setPointSize(4);

            FloatBuffer posBuf = m.getVertices();
            if(posBuf != null) {
                mesh.setBuffer(VertexBuffer.Type.Position, 3, posBuf);
            }
            FloatBuffer normBuf = m.getNormals();
            if(normBuf != null) {
                mesh.setBuffer(VertexBuffer.Type.Normal, 3, normBuf);
            }
            FloatBuffer texBuf = m.getTextureCoordinates();
            if(texBuf != null) {
                mesh.setBuffer(VertexBuffer.Type.TexCoord, 2, texBuf);
            }
            FloatBuffer tanBuf = m.getTangents();
            if(tanBuf != null) {
                mesh.setBuffer(VertexBuffer.Type.Tangent, 4, tanBuf);
            }
            IntBuffer idxBuf = m.getIndices();
            if(idxBuf != null) {
                mesh.setBuffer(VertexBuffer.Type.Index, 1, idxBuf);
            }

            mesh.setStatic();
            mesh.updateBound();
            mesh.updateCounts();

            Geometry geom = new Geometry(name + "-geom", mesh);
            return geom;
        }

    }

    private static class VTFLoader implements AssetLoader {

        private static final Logger LOG = Logger.getLogger(VTFLoader.class.getName());

        VTFLoader() {
        }

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

}
