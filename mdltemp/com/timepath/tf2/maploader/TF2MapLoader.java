package com.timepath.tf2.maploader;

import com.jme3.app.SimpleApplication;
import com.jme3.input.ChaseCamera;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.scene.Geometry;
import com.jme3.system.AppSettings;
import com.jme3.system.JmeCanvasContext;
import com.jme3.texture.Texture;
import java.awt.Canvas;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.logging.Logger;
import javax.swing.JFrame;

/**
 *
 * @author timepath
 */
public class TF2MapLoader extends SimpleApplication {
    
    private static final Logger LOG = Logger.getLogger(TF2MapLoader.class.getName());
    
    public static void main(String... args) {
        final TF2MapLoader app = new TF2MapLoader();
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
        Texture tex = assetManager.loadTexture("tf/materials/models/weapons/v_sapper/v_sapper.vtf");
        Geometry mdl = (Geometry) assetManager.loadModel("tf/models/weapons/v_models/v_sapper_spy.mdl");
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
        chaseCam.setSmoothMotion(true);
        chaseCam.setRotationSpeed(3);
        chaseCam.setMinVerticalRotation(-179);
        chaseCam.setMaxVerticalRotation(179);
        chaseCam.setDefaultHorizontalRotation(-FastMath.HALF_PI);
    }
    
}
