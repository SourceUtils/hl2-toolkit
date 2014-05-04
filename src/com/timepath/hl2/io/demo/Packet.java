package com.timepath.hl2.io.demo;

import com.timepath.hl2.io.util.Vector3f;
import com.timepath.io.BitBuffer;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author TimePath
 */
public enum Packet {

    net_NOP(0, new PacketHandler() {
        @Override
        public boolean read(BitBuffer bb, List<Object> l) {
            return true;
        }
    }),
    net_Disconnect(1, new PacketHandler() {
        @Override
        boolean read(BitBuffer bb, List<Object> l) {
            l.add("Reason: " + bb.getString());
            return true;
        }
    }),
    net_File(2, new PacketHandler() {
        @Override
        boolean read(BitBuffer bb, List<Object> l) {
            l.add("Transfer ID: " + bb.getInt());
            l.add("Filename: " + bb.getString());
            l.add("Requested: " + bb.getBoolean());
            return true;
        }
    }),
    net_Tick(3, new PacketHandler() {
        @Override
        public boolean read(BitBuffer bb, List<Object> l) {
            l.add("Tick: " + bb.getInt());
            l.add("Host frametime: " + bb.getShort());
            l.add("Host frametime StdDev: " + bb.getShort());
            return true;
        }
    }),
    net_StringCmd(4, new PacketHandler() {
        @Override
        boolean read(BitBuffer bb, List<Object> l) {
            l.add("Command: " + bb.getString());
            return true;
        }
    }),
    net_SetConVar(5, new PacketHandler() {
        @Override
        public boolean read(BitBuffer bb, List<Object> l) {
            short n = bb.getByte();
            while(n-- > 0) {
                l.add(bb.getString() + ": " + bb.getString());
            }
            return true;
        }
    }),
    net_SignonState(6, new PacketHandler() {
        @Override
        public boolean read(BitBuffer bb, List<Object> l) {
            l.add("Signon state: " + SignonState.values()[bb.getByte() & 0xFF]);
            l.add("Spawn count: " + ((long) bb.getInt()));
            return true;
        }
    }),
    /**
     * 16 in newer protocols
     */
    svc_Print(7, new PacketHandler() {
        @Override
        public boolean read(BitBuffer bb, List<Object> l) {
            l.add(bb.getString());
            return true;
        }
    }),
    svc_ServerInfo(8, new PacketHandler() {
        @Override
        public boolean read(BitBuffer bb, List<Object> l) {
            short version = (short) bb.getBits(16);
            l.add("Version: " + version);
            l.add("Server count: " + (int) bb.getBits(32));
            l.add("SourceTV: " + bb.getBoolean());
            l.add("Dedicated: " + bb.getBoolean());
            l.add("Server client CRC: 0x" + Integer.toHexString(bb.getInt()));
            l.add("Max classes: " + bb.getBits(16));
            if(version < 18) {
                l.add("Server map CRC: 0x" + Integer.toHexString(bb.getInt()));
            } else {
                bb.getBits(128); // TODO: display out map md5 hash
            }
            l.add("Current player count: " + bb.getBits(8));
            l.add("Max player count: " + bb.getBits(8));
            l.add("Interval per tick: " + bb.getFloat());
            l.add("Platform: " + (char) bb.getBits(8));
            l.add("Game directory: " + bb.getString());
            l.add("Map name: " + bb.getString());
            l.add("Skybox name: " + bb.getString());
            l.add("Hostname: " + bb.getString());
            l.add("Has replay: " + bb.getBoolean()); // ???: protocol version
            return true;
        }
    }),
    // TODO
    svc_SendTable(9, new PacketHandler() {
    }),
    svc_ClassInfo(10, new PacketHandler() {
        @Override
        public boolean read(BitBuffer bb, List<Object> l) {
            int n = bb.getShort();
            l.add("Number of server classes: " + n);
            boolean cc = bb.getBoolean();
            l.add("Create classes on client: " + cc);
            if(!cc) {
                return false;
//                while(n-- > 0) {
//                    l.add("Class ID: " + bb.getBits(Math.log(n, 2) + 1));
//                    l.add("Class name: " + bb.getString());
//                    l.add("Datatable name: " + bb.getString());
//                }
            }
            return true;
        }
    }),
    // TODO
    svc_SetPause(11, new PacketHandler() {
    }),
    svc_CreateStringTable(12, new PacketHandler() {
        @Override
        public boolean read(BitBuffer bb, List<Object> l) {
            l.add("Table name: " + bb.getString());
            int m = bb.getShort();
            l.add("Max entries: " + m);
            int encodeBits = (int) bb.getBits((int) ((Math.log(m) / Math.log(2)) + 1));
            l.add("Number of entries: " + encodeBits);
            long length = bb.getBits(HL2DEM.NET_MAX_PALYLOAD_BITS + 3);
            l.add("Length in bits: " + length);
            boolean f = bb.getBoolean();
            l.add("Userdata fixed size: " + f);
            if(f) {
                l.add("Userdata size: " + bb.getBits(12));
                l.add("Userdata bits: " + bb.getBits(4));
            }
//             ???: this is not in Source 2007 netmessages.h/cpp it seems. protocol version?
//            l.add("Compressed: " + bb.getBoolean());
            bb.getBits(encodeBits); // TODO
            return true;
        }
    }),
    // TODO
    svc_UpdateStringTable(13, new PacketHandler() {
    }),
    svc_VoiceInit(14, new PacketHandler() {
        @Override
        public boolean read(BitBuffer bb, List<Object> l) {
            l.add("Codec: " + bb.getString());
            l.add("Quality: " + bb.getByte());
            return true;
        }
    }),
    svc_VoiceData(15, new PacketHandler() {
        @Override
        boolean read(BitBuffer bb, List<Object> l) {
            l.add("Client: " + bb.getBits(8));
            l.add("Proximity: " + bb.getBits(8));
            int length = (int) bb.getBits(16);
            l.add("Length in bits: " + length);
            bb.getBits(length);
            return true;
        }
    }),
    /**
     * TODO: One of these
     * svc_HLTV: HLTV control messages
     * svc_Print: split screen style message
     */
    svc_Unknown16(16, new PacketHandler() {
    }),
    svc_Sounds(17, new PacketHandler() {
        @Override
        public boolean read(BitBuffer bb, List<Object> l) {
            boolean reliable = bb.getBoolean();
            l.add("Reliable: " + reliable);
            int count = reliable ? 1 : bb.getByte();
            l.add("Number of sounds: " + count);
            int length = reliable ? bb.getByte() : bb.getShort();
            l.add("Length in bits: " + length);
            bb.getBits(length); // TODO
            return true;
        }
    }),
    svc_SetView(18, new PacketHandler() {
        @Override
        boolean read(BitBuffer bb, List<Object> l) {
            l.add("Entity index: " + bb.getBits(11));
            return true;
        }
    }),
    svc_FixAngle(19, new PacketHandler() {
        @Override
        boolean read(BitBuffer bb, List<Object> l) {
            l.add("Relative: " + bb.getBoolean());
            bb.getBits(48); // TODO
            return true;
        }
    }),
    // TODO
    svc_CrosshairAngle(20, new PacketHandler() {
    }),
    svc_BSPDecal(21, new PacketHandler() {
        public float ReadCoord(BitBuffer bb) {
            boolean hasint = bb.getBoolean();
            boolean hasfract = bb.getBoolean();
            float value = 0;
            if(hasint || hasfract) {
                boolean sign = bb.getBoolean();
                if(hasint) {
                    value += bb.getBits(14) + 1;
                }
                if(hasfract) {
                    value += bb.getBits(5) * (1 / 32f);
                }
                if(sign) {
                    value = -value;
                }
            }
            return value;
        }

        public Vector3f ReadVecCoord(BitBuffer bb) {
            boolean hasx = bb.getBoolean();
            boolean hasy = bb.getBoolean();
            boolean hasz = bb.getBoolean();
            return new Vector3f(hasx ? ReadCoord(bb) : 0, hasy ? ReadCoord(bb) : 0, hasz ? ReadCoord(bb) : 0);
        }

        @Override
        boolean read(BitBuffer bb, List<Object> l, HL2DEM demo) {
            l.add("Position: " + ReadVecCoord(bb));
            l.add("Decal texture index: " + bb.getBits(HL2DEM.MAX_DECAL_INDEX_BITS));
            if(bb.getBoolean()) {
                l.add("Entity index: " + bb.getBits(HL2DEM.MAX_EDICT_BITS));
                int bits = HL2DEM.SP_MODEL_INDEX_BITS;
                if(demo.header.demoProtocol <= 21) {
                    bits--;
                }
                l.add("Model index: " + bb.getBits(bits));
            }
            l.add("Low priority: " + bb.getBoolean());
            return true;
        }
    }),
    /**
     * TODO: One of these
     * svc_TerrainMod: modification to the terrain/displacement
     * svc_SplitScreen: split screen style message
     */
    svc_unknown2(22, new PacketHandler() {
    }),
    svc_UserMessage(23, new PacketHandler() {
        @Override
        boolean read(BitBuffer bb, List<Object> l) {
            int userMsgType = (int) bb.getBits(8);
            l.add("Message type: " + userMsgType);
            int length = (int) bb.getBits(11);
            l.add("Length in bits: " + length);
            bb.getBits(length); // TODO
            return true;
        }
    }),
    // TODO
    svc_EntityMessage(24, new PacketHandler() {
    }),
    svc_GameEvent(25, new PacketHandler() {
        @Override
        boolean read(BitBuffer bb, List<Object> l, HL2DEM demo) {
            int length = (int) bb.getBits(11);
            l.add("Length in bits: " + length);
            int gameEventId = (int) bb.getBits(9);
            String gameEvent = demo.gameEvents[gameEventId];
            if(gameEvent != null) {
                l.add("Event: " + gameEvent);
            }
            bb.getBits(length - 9); // TODO
            return true;
        }
    }),
    svc_PacketEntities(26, new PacketHandler() {
        @Override
        boolean read(BitBuffer bb, List<Object> l) {
            l.add("Max entries: " + bb.getBits(11));
            boolean d = bb.getBoolean();
            l.add("Is delta: " + d);
            if(d) {
                l.add("Delta from: " + bb.getBits(32));
            }
            l.add("Baseline: " + bb.getBoolean());
            l.add("Updated entries: " + bb.getBits(11));
            int b = (int) bb.getBits(20);
            l.add("Length in bits: " + b);
            l.add("Update baseline: " + bb.getBoolean());
            bb.getBits(b); // TODO
            return true;
        }
    }),
    svc_TempEntities(27, new PacketHandler() {
        @Override
        boolean read(BitBuffer bb, List<Object> l) {
            l.add("Number of entries: " + bb.getBits(HL2DEM.EVENT_INDEX_BITS));
            int length = (int) bb.getBits(HL2DEM.NET_MAX_PALYLOAD_BITS);
            l.add("Length in bits: " + length);
            l.add("remaining bits: " + bb.remainingBits());
            bb.getBits(Math.min(length, bb.remainingBits())); // TODO: underflows
            return true;
        }
    }),
    svc_Prefetch(28, new PacketHandler() {
        @Override
        boolean read(BitBuffer bb, List<Object> l, HL2DEM demo) {
            int bits = HL2DEM.MAX_SOUND_INDEX_BITS;
            if(demo.header.networkProtocol <= 22) {
                bits = 13;
            }
            l.add("Sound index: " + bb.getBits(bits));
            return true;
        }
    }),
    svc_Menu(29, new PacketHandler() {
        @Override
        boolean read(BitBuffer bb, List<Object> l) {
            l.add("Menu type: " + bb.getBits(16));
            int length = (int) bb.getBits(16);
            l.add("Length in bytes: " + length);
            bb.getBits(length << 3); // TODO
            return true;
        }
    }),
    svc_GameEventList(30, new PacketHandler() {
        @Override
        boolean read(BitBuffer bb, List<Object> l, HL2DEM demo) {
            int numGameEvents = (int) bb.getBits(9);
            demo.gameEvents = new String[HL2DEM.MAX_GAME_EVENTS];
            l.add("Number of events: " + numGameEvents);
            int length = (int) bb.getBits(20);
            l.add("Length in bits: " + length);
            for(int i = 0; i < numGameEvents; i++) {
                int id = (int) bb.getBits(9);
                String name = bb.getString();
                l.add("event: [" + id + "] " + name);
                demo.gameEvents[id] = name;
                List<Object> sub = new LinkedList<Object>();
                while(true) {
                    int entryType = (int) bb.getBits(3);
                    if(entryType == 0) { // End of event description
                        break;
                    }
                    String entryName = bb.getString();
                    sub.add("entry: [" + GameEventMessageType.get(entryType) + "] " + entryName);
                }
                l.add(sub);
            }
            return true;
        }
    }),
    svc_GetCvarValue(31, new PacketHandler() {
        @Override
        boolean read(BitBuffer bb, List<Object> l) {
            l.add(MessageFormat.format("Cookie: 0x{0}", Integer.toHexString(bb.getInt())));
            l.add(bb.getString());
            return true;
        }
    }),
    // TODO
    svc_CmdKeyValues(32, new PacketHandler() {
    });

    final PacketHandler handler;

    private final int[] i;

    Packet(int i, PacketHandler handler) {
        this(new int[] {i}, handler);
    }

    Packet(int[] i, PacketHandler handler) {
        this.i = i;
        this.handler = handler;
    }

    public static Packet get(int i) {
        for(Packet t : Packet.values()) {
            for(int j : t.i) {
                if(j == i) {
                    return t;
                }
            }
        }
        return null;
    }

}
