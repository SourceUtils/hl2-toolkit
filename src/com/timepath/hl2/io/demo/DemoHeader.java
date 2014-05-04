package com.timepath.hl2.io.demo;

import com.timepath.DataUtils;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author TimePath
 */
public class DemoHeader {

    private static final Logger LOG = Logger.getLogger(DemoHeader.class.getName());

    final String clientName;

    final int demoProtocol;

    final int frames;

    final String gameDirectory;

    final String head;

    final String mapName;

    final int networkProtocol;

    final float playbackTime;

    final String serverName;

    final int signonLength;

    final int ticks;

    DemoHeader(ByteBuffer slice) {
        head = DataUtils.getText(DataUtils.getSlice(slice, 8));
        if(!head.equals(HL2DEM.HEADER)) {
            LOG.log(Level.WARNING, "Unexpected header");
        }
        demoProtocol = slice.getInt();
        if(demoProtocol != HL2DEM.DEMO_PROTOCOL) {
            LOG.log(Level.WARNING, "Unknown demo version {0}", demoProtocol);
        }
        networkProtocol = slice.getInt();
        LOG.log(Level.INFO, "Network protocol: {0}", networkProtocol);
        ByteBuffer serverNameBuffer = DataUtils.getSlice(slice, 260);
        serverName = DataUtils.getText(serverNameBuffer).trim();
        LOG.log(Level.INFO, "Server: {0}", serverName);
        ByteBuffer clientNameBuffer = DataUtils.getSlice(slice, 260);
        clientName = DataUtils.getText(clientNameBuffer).trim();
        LOG.log(Level.INFO, "Client: {0}", clientName);
        ByteBuffer mapNameBuffer = DataUtils.getSlice(slice, 260);
        mapName = DataUtils.getText(mapNameBuffer).trim();
        LOG.log(Level.INFO, "Map: {0}", mapName);
        ByteBuffer gameDirectoryBuffer = DataUtils.getSlice(slice, 260);
        gameDirectory = DataUtils.getText(gameDirectoryBuffer).trim();
        LOG.log(Level.INFO, "Game: {0}", gameDirectory);
        playbackTime = slice.getFloat();
        LOG.log(Level.INFO, "Playback time: {0}", playbackTime);
        ticks = slice.getInt();
        LOG.log(Level.INFO, "Ticks: {0}", ticks);
        frames = slice.getInt();
        LOG.log(Level.INFO, "Frames: {0}", frames);
        signonLength = slice.getInt();
        LOG.log(Level.INFO, "Signon length: {0}", signonLength);
    }

}
