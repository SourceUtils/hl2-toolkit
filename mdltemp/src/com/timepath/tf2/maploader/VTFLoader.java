package com.timepath.tf2.maploader;

import com.jme3.asset.AssetInfo;
import com.jme3.asset.AssetLoader;
import com.jme3.asset.TextureKey;
import com.jme3.texture.Image;
import com.jme3.util.BufferUtils;
import com.timepath.hl2.io.VTF;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

public final class VTFLoader implements AssetLoader {

    @Override
    public Object load(AssetInfo info) throws IOException {
        File f = new File("src/" + info.getKey().getName());
        LOG.info(f.toString());
        VTF v = VTF.load(f);
        BufferedImage t = (BufferedImage) v.getThumbImage();
        t = (BufferedImage) v.getImage(0);
        
        byte[] rawData = new byte[t.getWidth() * t.getHeight() * 4];
        
        int idx = 0;
        for(int x = 0; x < t.getWidth(); x++) {
            for(int y = 0; y < t.getHeight(); y++) {
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
    private static final Logger LOG = Logger.getLogger(VTFLoader.class.getName());
}

