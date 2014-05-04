package com.timepath.hl2.io.demo;

import com.timepath.DataUtils;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/common/proto_version.h
 * https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/public/demofile/demoformat.h
 * https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/public/networkstringtabledefs.h
 * https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/public/keyvaluescompiler.h
 * <p/>
 * https://github.com/LestaD/SourceEngine2007/blob/master/src_main/common/netmessages.cpp
 * <p/>
 * https://github.com/jpcy/coldemoplayer
 * https://github.com/stgn/netdecode
 * https://github.com/tritao/netdecode
 * <p/>
 * https://github.com/jpcy/coldemoplayer/blob/master/compLexity%20Demo%20Player/demo/SourceDemo.cs
 * https://github.com/jpcy/coldemoplayer/blob/master/compLexity%20Demo%20Player/demo%20parser/SourceDemoParser.cs
 * <p/>
 * https://github.com/tritao/netdecode/blob/master/DemoFile.cs
 * https://github.com/tritao/netdecode/blob/master/Packet.cs
 * <p/>
 * https://forums.alliedmods.net/showthread.php?t=232925
 * <p/>
 * @author TimePath
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

    public static HL2DEM load(File f) throws IOException {
        LOG.log(Level.INFO, "Parsing {0}", f);
        ByteBuffer buffer = DataUtils.mapFile(f);
        return new HL2DEM(buffer);
    }

    private List<Message> frames = new LinkedList<Message>();

    GameEvent[] gameEvents;

    DemoHeader header;

    public HL2DEM(ByteBuffer buffer) {
        header = new DemoHeader(DataUtils.getSlice(buffer, 1072));

        while(true) {
            Message frame = new Message(buffer, this);
            frames.add(frame);
            if(frame.type == MessageType.Stop) {
                break;
            }
            if(frame.type == MessageType.Synctick) {
                continue;
            }
            switch(frame.type) {
                case Packet:
                case Signon:
                    buffer.get(new byte[21 * 4]); // TODO: Command / sequence info
                    break;
                case UserCmd:
                    buffer.get(new byte[4]); // TODO: Outgoing sequence number
                    break;
                default:
                    break;
            }

            int size = buffer.getInt();
            if(size == 0) {
                continue;
            }

            byte[] data = new byte[size];
            buffer.get(data);
            frame.data = ByteBuffer.wrap(data);
            frame.data.order(ByteOrder.LITTLE_ENDIAN);

            frame.parse();
        }
    }

    /**
     * @return the frames
     */
    public List<Message> getFrames() {
        return Collections.unmodifiableList(frames);
    }

}