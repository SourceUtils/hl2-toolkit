package com.timepath.hl2.io.demo;

import com.timepath.DataUtils;
import com.timepath.io.struct.StructField;

import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class DemoHeader {

    private static final Logger LOG        = Logger.getLogger(DemoHeader.class.getName());
    private static final int    MAX_OSPATH = 260;
    @StructField(index = 0, limit = 8)
    public String head;
    @StructField(index = 1)
    public int    demoProtocol;
    @StructField(index = 2)
    public int    networkProtocol;
    @StructField(index = 3, limit = MAX_OSPATH)
    public String serverName;
    @StructField(index = 4, limit = MAX_OSPATH)
    public String clientName;
    @StructField(index = 5, limit = MAX_OSPATH)
    public String mapName;
    @StructField(index = 6, limit = MAX_OSPATH)
    public String gameDirectory;
    @StructField(index = 7)
    public float  playbackTime;
    @StructField(index = 8)
    public int    ticks;
    @StructField(index = 9)
    public int    frames;
    @StructField(index = 10)
    public int    signonLength;

    static DemoHeader parse(ByteBuffer slice) {
        DemoHeader h = new DemoHeader();
        h.head = DataUtils.getText(DataUtils.getSlice(slice, 8));
        if(!HL2DEM.HEADER.equals(h.head)) {
            LOG.log(Level.WARNING, "Unexpected header");
        }
        h.demoProtocol = slice.getInt();
        if(h.demoProtocol != HL2DEM.DEMO_PROTOCOL) {
            LOG.log(Level.WARNING, "Unknown demo version {0}", h.demoProtocol);
        }
        h.networkProtocol = slice.getInt();
        LOG.log(Level.INFO, "Network protocol: {0}", h.networkProtocol);
        ByteBuffer serverNameBuffer = DataUtils.getSlice(slice, MAX_OSPATH);
        h.serverName = DataUtils.getText(serverNameBuffer, true);
        LOG.log(Level.INFO, "Server: {0}", h.serverName);
        ByteBuffer clientNameBuffer = DataUtils.getSlice(slice, MAX_OSPATH);
        h.clientName = DataUtils.getText(clientNameBuffer, true);
        LOG.log(Level.INFO, "Client: {0}", h.clientName);
        ByteBuffer mapNameBuffer = DataUtils.getSlice(slice, MAX_OSPATH);
        h.mapName = DataUtils.getText(mapNameBuffer, true);
        LOG.log(Level.INFO, "Map: {0}", h.mapName);
        ByteBuffer gameDirectoryBuffer = DataUtils.getSlice(slice, MAX_OSPATH);
        h.gameDirectory = DataUtils.getText(gameDirectoryBuffer, true);
        LOG.log(Level.INFO, "Game: {0}", h.gameDirectory);
        h.playbackTime = slice.getFloat();
        LOG.log(Level.INFO, "Playback time: {0}", h.playbackTime);
        h.ticks = slice.getInt();
        LOG.log(Level.INFO, "Ticks: {0}", h.ticks);
        h.frames = slice.getInt();
        LOG.log(Level.INFO, "Frames: {0}", h.frames);
        h.signonLength = slice.getInt();
        LOG.log(Level.INFO, "Signon length: {0}", h.signonLength);
        return h;
    }
}
