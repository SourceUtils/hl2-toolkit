package com.timepath.hl2.io.demo;

import com.timepath.DataUtils;

import java.io.File;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Format of a demo:
 * HL2DEM {
 * DemoHeader,
 * Message {
 * Packet
 * ...
 * }
 * ...
 * }
 *
 * @author TimePath
 * @see <a>https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/common/proto_version.h</a>
 * @see <a>https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/public/demofile/demoformat.h</a>
 * @see <a>https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/public/networkstringtabledefs.h</a>
 * @see <a>https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/public/keyvaluescompiler.h</a>
 * @see <a>https://github.com/LestaD/SourceEngine2007/blob/master/src_main/common/netmessages.cpp</a>
 * @see <a>https://github.com/jpcy/coldemoplayer</a>
 * @see <a>https://github.com/stgn/netdecode</a>
 * @see <a>https://github.com/tritao/netdecode</a>
 * @see <a>https://github.com/jpcy/coldemoplayer/blob/master/compLexity%20Demo%20Player/demo/SourceDemo.cs</a>
 * @see <a>https://github.com/jpcy/coldemoplayer/blob/master/compLexity%20Demo%20Player/demo%20parser/SourceDemoParser.cs</a>
 * @see <a>https://github.com/tritao/netdecode/blob/master/DemoFile.cs</a>
 * @see <a>https://github.com/tritao/netdecode/blob/master/Packet.cs</a>
 * @see <a>https://forums.alliedmods.net/showthread.php?t=232925</a>
 * @see <a>http://demos.geit.co.uk/</a>
 */
public class HL2DEM {

    public static final int DEMO_PROTOCOL = 3;
    public static final int EVENT_INDEX_BITS = 8;
    public static final String HEADER = "HL2DEMO\0";
    public static final int MAX_DECAL_INDEX_BITS = 9;
    public static final int MAX_EDICT_BITS = 11;
    public static final int MAX_GAME_EVENTS = 1 << 9;
    public static final int MAX_SOUND_INDEX_BITS = 14;
    public static final int NET_MAX_PALYLOAD_BITS = 17;
    public static final int SP_MODEL_INDEX_BITS = 12;
    /**
     * TF2 specific, need enough space for OBJ_LAST items from tf_shareddefs.h
     */
    public static final int WEAPON_SUBTYPE_BITS = 6;
    private static final Logger LOG = Logger.getLogger(HL2DEM.class.getName());
    private final List<Message> frames = new LinkedList<>();
    GameEvent[] gameEvents;
    DemoHeader header;
    int serverClassBits;

    private HL2DEM(ByteBuffer buffer, boolean eager) {
        header = DemoHeader.parse(DataUtils.getSlice(buffer, 32 + 260 * 4));
        while (true) {
            Message frame;
            try {
                frame = Message.parse(this, buffer);
            } catch (BufferUnderflowException e) {
                LOG.log(Level.WARNING, "Unexpected end of demo");
                break;
            }
            frames.add(frame);
            if (frame.type == MessageType.Stop) break;
            if (frame.size == 0) continue;
            byte[] dst = new byte[frame.size];
            try {
                buffer.get(dst);
            } catch (BufferUnderflowException e) {
                LOG.log(Level.SEVERE, "Unexpected end of message", e);
                break;
            }
            frame.data = ByteBuffer.wrap(dst);
            frame.data.order(ByteOrder.LITTLE_ENDIAN);
            if (eager) frame.parse();
        }
    }

    public static HL2DEM load(File f) throws IOException {
        return load(f, true);
    }

    public static HL2DEM load(File f, boolean eager) throws IOException {
        LOG.log(Level.INFO, "Parsing {0}", f);
        ByteBuffer buffer = DataUtils.mapFile(f);
        return new HL2DEM(buffer, eager);
    }

    /**
     * @return the frames
     */
    public List<Message> getFrames() {
        return frames;
    }

    public DemoHeader getHeader() {
        return header;
    }
}
