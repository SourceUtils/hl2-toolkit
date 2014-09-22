package com.timepath.hl2.io.demo;

import com.timepath.Pair;
import com.timepath.hl2.io.util.Vector3f;
import com.timepath.io.BitBuffer;

import java.math.BigInteger;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;

/**
 * @author TimePath
 */
public class Packet {

    public final List<Pair<Object, Object>> list = new LinkedList<>();
    public final Type type;
    public final int offset;

    public Packet(Type type, int offset) {
        this.type = type;
        this.offset = offset;
    }

    private static int log2(int i) {
        return (int) (Math.log(i) / Math.log(2));
    }

    @Override
    public String toString() {
        return MessageFormat.format("{0}, offset {1}", type, offset);
    }

    /**
     * https://github.com/LestaD/SourceEngine2007/blob/master/src_main/common/netmessages.h
     */
    public static enum Type {
        net_NOP(0, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                return true;
            }
        }),
        net_Disconnect(1, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                l.add(new Pair<Object, Object>("Reason", bb.getString()));
                return true;
            }
        }),
        net_File(2, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                l.add(new Pair<Object, Object>("Transfer ID", bb.getInt()));
                l.add(new Pair<Object, Object>("Filename", bb.getString()));
                l.add(new Pair<Object, Object>("Requested", bb.getBoolean()));
                return true;
            }
        }),
        net_Tick(3, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                l.add(new Pair<Object, Object>("Tick", bb.getInt()));
                l.add(new Pair<Object, Object>("Host frametime", bb.getShort()));
                l.add(new Pair<Object, Object>("Host frametime StdDev", bb.getShort()));
                return true;
            }
        }),
        net_StringCmd(4, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                l.add(new Pair<Object, Object>("Command", bb.getString()));
                return true;
            }
        }),
        net_SetConVar(5, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                short n = bb.getByte();
                for (int i = 0; i < n; i++) {
                    l.add(new Pair<Object, Object>(bb.getString(), bb.getString()));
                }
                return true;
            }
        }),
        net_SignonState(6, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                int state = bb.getByte() & 0xFF;
                SignonState[] signon = SignonState.values();
                l.add(new Pair<Object, Object>("Signon state", (state < signon.length) ? signon[state] : state));
                l.add(new Pair<Object, Object>("Spawn count", (int) bb.getBits(32)));
                return true;
            }
        }),
        /**
         * 16 in newer protocols
         */
        svc_Print(7, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                l.add(new Pair<Object, Object>("Value", bb.getString()));
                return true;
            }
        }),
        svc_ServerInfo(8, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                short version = (short) bb.getBits(16);
                l.add(new Pair<Object, Object>("Version", version));
                l.add(new Pair<Object, Object>("Server count", (int) bb.getBits(32)));
                l.add(new Pair<Object, Object>("SourceTV", bb.getBoolean()));
                l.add(new Pair<Object, Object>("Dedicated", bb.getBoolean()));
                l.add(new Pair<Object, Object>("Server client CRC", "0x" + Integer.toHexString(bb.getInt())));
                l.add(new Pair<Object, Object>("Max classes", bb.getBits(16)));
                if (version >= 18) {
                    byte[] md5 = new byte[16];
                    bb.get(md5);
                    l.add(new Pair<Object, Object>("Server map MD5",
                            String.format("%0" + (md5.length * 2) + "x",
                                    new BigInteger(1, md5))));
                } else {
                    l.add(new Pair<Object, Object>("Server map CRC", "0x" + Integer.toHexString(bb.getInt())));
                }
                l.add(new Pair<Object, Object>("Current player count", bb.getBits(8)));
                l.add(new Pair<Object, Object>("Max player count", bb.getBits(8)));
                l.add(new Pair<Object, Object>("Interval per tick", bb.getFloat()));
                l.add(new Pair<Object, Object>("Platform", (char) bb.getBits(8)));
                l.add(new Pair<Object, Object>("Game directory", bb.getString()));
                l.add(new Pair<Object, Object>("Map name", bb.getString()));
                l.add(new Pair<Object, Object>("Skybox name", bb.getString()));
                l.add(new Pair<Object, Object>("Hostname", bb.getString()));
                l.add(new Pair<Object, Object>("Has replay", bb.getBoolean())); // ???: protocol version
                return true;
            }
        }),
        /**
         * TODO
         */
        svc_SendTable(9, new PacketHandler() {
        }),
        svc_ClassInfo(10, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                int n = bb.getShort();
                l.add(new Pair<Object, Object>("Number of server classes", n));
                boolean cc = bb.getBoolean();
                l.add(new Pair<Object, Object>("Create classes on client", cc));
                demo.serverClassBits = log2(n) + 1;
                l.add(new Pair<Object, Object>("serverClassBits", demo.serverClassBits));
                if (!cc) {
                    for (int i = 0; i < n; i++) {
                        l.add(new Pair<Object, Object>("Class ID", bb.getBits(demo.serverClassBits)));
                        l.add(new Pair<Object, Object>("Class name", bb.getString()));
                        l.add(new Pair<Object, Object>("Datatable name", bb.getString()));
                    }
                }
                return true;
            }
        }),
        svc_SetPause(11, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                l.add(new Pair<Object, Object>("Paused", bb.getBoolean()));
                return true;
            }
        }),
        /**
         * TODO
         * https://github.com/LestaD/SourceEngine2007/blob/master/src_main/common/netmessages.cpp#L898
         */
        svc_CreateStringTable(12, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                String tableName = bb.getString();
                l.add(new Pair<Object, Object>("Table name", tableName));
                int maxEntries = bb.getShort();
                l.add(new Pair<Object, Object>("Max entries", maxEntries));
                int entryBits = log2(maxEntries);
                long numEntries = bb.getBits(entryBits + 1);
                l.add(new Pair<Object, Object>("Number of entries", numEntries));
                int length = (int) bb.getBits(HL2DEM.NET_MAX_PALYLOAD_BITS + 3);
                l.add(new Pair<Object, Object>("Length in bits", length));
                boolean userDataFixedSize = bb.getBoolean();
                l.add(new Pair<Object, Object>("Userdata fixed size", userDataFixedSize));
                int userDataSize = -1, userDataSizeBits = -1;
                if (userDataFixedSize) {
                    userDataSize = (int) bb.getBits(12);
                    l.add(new Pair<Object, Object>("Userdata size", userDataSize));
                    userDataSizeBits = (int) bb.getBits(4);
                    l.add(new Pair<Object, Object>("Userdata bits", userDataSizeBits));
                }
                StringTable.create(tableName, maxEntries, entryBits, userDataFixedSize, userDataSize, userDataSizeBits)
                //                .parse(bb, l)
                ;
                bb.getBits(length); // Skip
                return true;
            }
        }),
        /**
         * TODO
         * https://github.com/LestaD/SourceEngine2007/blob/master/src_main/common/netmessages.cpp#L837
         */
        svc_UpdateStringTable(13, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                int tableID = (int) bb.getBits(log2(StringTable.MAX_TABLES)); // 5 bits
                l.add(new Pair<Object, Object>("Table ID", tableID));
                long changedEntries = bb.getBoolean() ? bb.getBits(16) : 1;
                l.add(new Pair<Object, Object>("Changed entries", changedEntries));
                int length = (int) bb.getBits(20);
                l.add(new Pair<Object, Object>("Length in bits", length));
                StringTable.get(tableID)
                //                .parse(bb, l)
                ;
                bb.getBits(length); // Skip
                return true;
            }
        }),
        svc_VoiceInit(14, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                l.add(new Pair<Object, Object>("Codec", bb.getString()));
                l.add(new Pair<Object, Object>("Quality", bb.getByte()));
                return true;
            }
        }),
        svc_VoiceData(15, new VoiceDataHandler()),
        /**
         * TODO: One of these
         * svc_HLTV: HLTV control messages
         * svc_Print: split screen style message
         */
        svc_Unknown16(16, new PacketHandler() {
        }),
        /**
         * TODO
         */
        svc_Sounds(17, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                boolean reliable = bb.getBoolean();
                l.add(new Pair<Object, Object>("Reliable", reliable));
                int count = reliable ? 1 : bb.getByte();
                l.add(new Pair<Object, Object>("Number of sounds", count));
                int length = reliable ? bb.getByte() : bb.getShort();
                l.add(new Pair<Object, Object>("Length in bits", length));
                bb.getBits(length); // Skip
                return true;
            }
        }),
        svc_SetView(18, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                l.add(new Pair<Object, Object>("Entity index", bb.getBits(11)));
                return true;
            }
        }),
        svc_FixAngle(19, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                l.add(new Pair<Object, Object>("Relative", bb.getBoolean()));
                Vector3f v = new Vector3f(readBitAngle(bb, 16), readBitAngle(bb, 16), readBitAngle(bb, 16));
                l.add(new Pair<Object, Object>("Vector", v));
                return true;
            }

            float readBitAngle(BitBuffer bb, int numbits) {
                return bb.getBits(numbits) * (360.0f / (1 << numbits));
            }
        }),
        svc_CrosshairAngle(20, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                Vector3f v = new Vector3f(readBitAngle(bb, 16), readBitAngle(bb, 16), readBitAngle(bb, 16));
                l.add(new Pair<Object, Object>("Vector", v));
                return true;
            }

            float readBitAngle(BitBuffer bb, int numbits) {
                return bb.getBits(numbits) * (360.0f / (1 << numbits));
            }
        }),
        svc_BSPDecal(21, new PacketHandler() {
            float getCoord(BitBuffer bb) {
                boolean hasint = bb.getBoolean();
                boolean hasfract = bb.getBoolean();
                float value = 0;
                if (hasint || hasfract) {
                    boolean sign = bb.getBoolean();
                    if (hasint) {
                        value += bb.getBits(14) + 1;
                    }
                    if (hasfract) {
                        value += bb.getBits(5) * (1 / 32f);
                    }
                    if (sign) {
                        value = -value;
                    }
                }
                return value;
            }

            Vector3f getVecCoord(BitBuffer bb) {
                boolean hasx = bb.getBoolean();
                boolean hasy = bb.getBoolean();
                boolean hasz = bb.getBoolean();
                return new Vector3f(hasx ? getCoord(bb) : 0, hasy ? getCoord(bb) : 0, hasz ? getCoord(bb) : 0);
            }

            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                l.add(new Pair<Object, Object>("Position", getVecCoord(bb)));
                l.add(new Pair<Object, Object>("Decal texture index", bb.getBits(HL2DEM.MAX_DECAL_INDEX_BITS)));
                if (bb.getBoolean()) {
                    l.add(new Pair<Object, Object>("Entity index", bb.getBits(HL2DEM.MAX_EDICT_BITS)));
                    int bits = HL2DEM.SP_MODEL_INDEX_BITS;
                    if (demo.header.demoProtocol <= 21) {
                        bits--;
                    }
                    l.add(new Pair<Object, Object>("Model index", bb.getBits(bits)));
                }
                l.add(new Pair<Object, Object>("Low priority", bb.getBoolean()));
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
        /**
         * TODO
         */
        svc_UserMessage(23, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                return UserMessage.read(bb, l, demo);
            }
        }),
        /**
         * TODO
         */
        svc_EntityMessage(24, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                l.add(new Pair<Object, Object>("Entity index", bb.getBits(11)));
                l.add(new Pair<Object, Object>("Class ID", bb.getBits(9)));
                int length = (int) bb.getBits(11);
                l.add(new Pair<Object, Object>("Length in bits", length));
                bb.getBits(length); // Skip
                return true;
            }
        }),
        svc_GameEvent(25, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                int length = (int) bb.getBits(11);
                l.add(new Pair<Object, Object>("Length in bits", length));
                int gameEventId = (int) bb.getBits(9);
                GameEvent gameEvent = demo.gameEvents[gameEventId];
                if (gameEvent != null) {
                    l.add(new Pair<Object, Object>(gameEvent.name, gameEvent.parse(bb).entrySet()));
                } else {
                    l.add(new Pair<Object, Object>("Unknown event", gameEventId));
                    bb.getBits(length - 9); // Skip
                }
                return true;
            }
        }),
        /**
         * Non-delta compressed entities.
         * TODO
         *
         * @see <a>https://github.com/LestaD/SourceEngine2007/blob/master/src_main/engine/baseclientstate.cpp#L1245<a/>
         * @see <a>https://code.google.com/p/coldemoplayer/source/browse/branches/2.0/compLexity+Demo+Player/CDP
         * .HalfLifeDemo/Messages/SvcPacketEntities.cs?r=59#43<a/>
         * @see <a>https://github.com/LestaD/SourceEngine2007/blob/master/src_main/engine/packed_entity.h#L52<a/>
         * @see <a>https://github.com/LestaD/SourceEngine2007/blob/master/src_main/engine/sv_ents_write.cpp#L862<a/>
         */
        svc_PacketEntities(26, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                int MAX_EDICT_BITS = 11;
                int DELTASIZE_BITS = 20;
                long maxEntries = bb.getBits(MAX_EDICT_BITS);
                l.add(new Pair<Object, Object>("Max entries", maxEntries));
                boolean isDelta = bb.getBoolean();
                l.add(new Pair<Object, Object>("Is delta", isDelta));
                long deltaFrom = -1;
                if (isDelta) {
                    deltaFrom = bb.getBits(32);
                    l.add(new Pair<Object, Object>("Delta from", deltaFrom));
                }
                boolean baseline = bb.getBoolean();
                l.add(new Pair<Object, Object>("Baseline", baseline));
                long updatedEntries = bb.getBits(MAX_EDICT_BITS);
                l.add(new Pair<Object, Object>("Updated entries", updatedEntries));
                int length = (int) bb.getBits(DELTASIZE_BITS);
                l.add(new Pair<Object, Object>("Length in bits", length));
                boolean updateBaseline = bb.getBoolean();
                l.add(new Pair<Object, Object>("Update baseline", updateBaseline));
                bb.getBits(length); // Skip
                return true;
            }
        }),
        /**
         * TODO
         * https://github.com/LestaD/SourceEngine2007/blob/master/src_main/engine/servermsghandler.cpp#L738
         */
        svc_TempEntities(27, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                long numEntries = bb.getBits(HL2DEM.EVENT_INDEX_BITS);
                l.add(new Pair<Object, Object>("Number of entries", numEntries));
                int length = (int) bb.getBits(HL2DEM.NET_MAX_PALYLOAD_BITS);
                l.add(new Pair<Object, Object>("Length in bits", length));
                // FIXME: underflows, but is usually last
                bb.getBits(length); // Skip
                return true;
                /*
                if(numEntries == 0) {
                    boolean reliable = true;
                    numEntries = 1;
                }
                int classID = -1;
                l.add(new Pair<Object, Object>("serverClassBits", demo.serverClassBits));
                for(int i = 0; i < numEntries; i++) {
                    float delay = 0;
                    if(bb.getBoolean()) {
                        delay = bb.getBits(8) / 100.0f;
                    }
                    l.add(new Pair<Object, Object>("ent[" + i + "].delay", delay));
                    if(bb.getBoolean()) { // Full update
                        classID = (int) bb.getBits(demo.serverClassBits);
                    }
                    l.add(new Pair<Object, Object>("ent[" + i + "].classID", classID));
                }
                return false;
                */
            }
        }),
        svc_Prefetch(28, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                int bits = demo.header.networkProtocol >= 23 ? HL2DEM.MAX_SOUND_INDEX_BITS : 13;
                l.add(new Pair<Object, Object>("Sound index", bb.getBits(bits)));
                return true;
            }
        }),
        /**
         * TODO
         */
        svc_Menu(29, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                l.add(new Pair<Object, Object>("Menu type", bb.getBits(16)));
                int length = (int) bb.getBits(16);
                l.add(new Pair<Object, Object>("Length in bytes", length));
                bb.getBits(length * 8); // Skip
                return true;
            }
        }),
        svc_GameEventList(30, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                int numGameEvents = (int) bb.getBits(9);
                demo.gameEvents = new GameEvent[HL2DEM.MAX_GAME_EVENTS];
                l.add(new Pair<Object, Object>("Number of events", numGameEvents));
                int length = (int) bb.getBits(20);
                l.add(new Pair<Object, Object>("Length in bits", length));
                for (int i = 0; i < numGameEvents; i++) {
                    int id = (int) bb.getBits(9);
                    demo.gameEvents[id] = new GameEvent(bb);
                    l.add(new Pair<Object, Object>("gameEvents[" + id + "] = " + demo.gameEvents[id].name,
                            demo.gameEvents[id].declarations.entrySet()));
                }
                return true;
            }
        }),
        svc_GetCvarValue(31, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                l.add(new Pair<Object, Object>("Cookie", "0x" + Integer.toHexString(bb.getInt())));
                l.add(new Pair<Object, Object>("value", bb.getString()));
                return true;
            }
        }),
        /**
         * TODO
         */
        svc_CmdKeyValues(32, new PacketHandler() {
            @Override
            boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
                int length = bb.getInt();
                l.add(new Pair<Object, Object>("Length in bits: ", length));
                bb.getBits(length); // Skip
                return true;
            }
        });
        /**
         * The handler associated with this packet
         */
        final PacketHandler handler;
        /**
         * The opcode for this packet
         */
        private final int[] id;

        Type(int id, PacketHandler handler) {
            this(new int[]{id}, handler);
        }

        Type(int[] id, PacketHandler handler) {
            this.id = id;
            this.handler = handler;
        }

        public static Type get(int i) {
            for (Type t : Type.values()) {
                for (int j : t.id) {
                    if (j == i) {
                        return t;
                    }
                }
            }
            return null;
        }
    }
}
