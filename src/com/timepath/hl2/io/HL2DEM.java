package com.timepath.hl2.io;

import com.timepath.DataUtils;
import com.timepath.hl2.io.util.Vector3f;
import com.timepath.io.BitBuffer;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/common/proto_version.h
 * https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/public/demofile/demoformat.h
 * https://github.com/ValveSoftware/source-sdk-2013/blob/f56bb35301836e56582a575a75864392a0177875/mp/src/public/networkstringtabledefs.h
 * https://github.com/ValveSoftware/source-sdk-2013/blob/f56bb35301836e56582a575a75864392a0177875/mp/src/public/keyvaluescompiler.h
 * <p/>
 * https://github.com/jpcy/coldemoplayer
 * https://github.com/stgn/netdecode
 * https://github.com/tritao/netdecode
 * <p/>
 * https://github.com/jpcy/coldemoplayer/blob/ce21973bf7b4e4ae7a981ab76dff659ce2511ece/compLexity%20Demo%20Player/demo/SourceDemo.cs
 * https://github.com/jpcy/coldemoplayer/blob/ce21973bf7b4e4ae7a981ab76dff659ce2511ece/compLexity%20Demo%20Player/demo%20parser/SourceDemoParser.cs
 * <p/>
 * https://github.com/tritao/netdecode/blob/master/DemoFile.cs
 * https://github.com/tritao/netdecode/blob/master/Packet.cs
 * <p/>
 * https://forums.alliedmods.net/showthread.php?t=232925
 * <p>
 * @author TimePath
 */
public class HL2DEM {

    private static final int DEMO_PROTOCOL = 3;

    private static final String HEADER = "HL2DEMO\0";

    private static final Logger LOG = Logger.getLogger(HL2DEM.class.getName());

    private static final int MAX_EDICT_BITS = 11;

    private static final int MAX_GAME_EVENTS = (int) Math.pow(2, 9) - 1;

    /**
     * TF2 specific, need enough space for OBJ_LAST items from tf_shareddefs.h
     */
    private static final int WEAPON_SUBTYPE_BITS = 6;

    private static String[] gameEvents;

    public static HL2DEM load(File f) throws IOException {
        LOG.log(Level.INFO, "Parsing {0}", f);
        ByteBuffer buffer = DataUtils.mapFile(f);
        return new HL2DEM(buffer);
    }

    private List<Message> frames = new LinkedList<Message>();

    private DemoHeader header;

    public HL2DEM(ByteBuffer buffer) {
        header = new DemoHeader(DataUtils.getSlice(buffer, 1072));

        while(true) {
            Message frame = new Message(buffer);
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
                    buffer.get(new byte[21 * 4]); // command / sequence info
                    break;
                case UserCmd:
                    buffer.get(new byte[4]); // outgoing sequence number
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

    public static enum MessageType {

        Signon(1),
        Packet(2),
        Synctick(3),
        ConsoleCmd(4),
        UserCmd(5),
        DataTables(6),
        Stop(7),
        StringTables(8);

        MessageType(int i) {
            this.i = i;
        }

        int i;

        static MessageType get(int i) {
            for(MessageType m : MessageType.values()) {
                if(m.i == i) {
                    return m;
                }
            }
            return null;
        }

    }

    private static enum GameEventMessageType {

        /**
         * A zero terminated string
         */
        STRING,
        /**
         * Float, 32 bit
         */
        FLOAT,
        /**
         * Signed int, 32 bit
         */
        LONG,
        /**
         * Signed int, 16 bit
         */
        SHORT,
        /**
         * Unsigned int, 8 bit
         */
        BYTE,
        /**
         * Unsigned int, 1 bit
         */
        BOOL,
        /**
         * Any data, but not networked to clients
         */
        LOCAL;

    }

    /**
     * hl2sdk-ob-valve/public/inputsystem/ButtonCode.h
     */
    private static enum Buttons {

        KEY_NONE(0),
        KEY_0(1),
        KEY_1(2),
        KEY_2(3),
        KEY_3(4),
        KEY_4(5),
        KEY_5(6),
        KEY_6(7),
        KEY_7(8),
        KEY_8(9),
        KEY_9(10),
        KEY_A(11),
        KEY_B(12),
        KEY_C(13),
        KEY_D(14),
        KEY_E(15),
        KEY_F(16),
        KEY_G(17),
        KEY_H(18),
        KEY_I(19),
        KEY_J(20),
        KEY_K(21),
        KEY_L(22),
        KEY_M(23),
        KEY_N(24),
        KEY_O(25),
        KEY_P(26),
        KEY_Q(27),
        KEY_R(28),
        KEY_S(29),
        KEY_T(30),
        KEY_U(31),
        KEY_V(32),
        KEY_W(33),
        KEY_X(34),
        KEY_Y(35),
        KEY_Z(36),
        KEY_PAD_0(37),
        KEY_PAD_1(38),
        KEY_PAD_2(39),
        KEY_PAD_3(40),
        KEY_PAD_4(41),
        KEY_PAD_5(42),
        KEY_PAD_6(43),
        KEY_PAD_7(44),
        KEY_PAD_8(45),
        KEY_PAD_9(46),
        KEY_PAD_DIVIDE(47),
        KEY_PAD_MULTIPLY(48),
        KEY_PAD_MINUS(49),
        KEY_PAD_PLUS(50),
        KEY_PAD_ENTER(51),
        KEY_PAD_DECIMAL(52),
        KEY_LBRACKET(53),
        KEY_RBRACKET(54),
        KEY_SEMICOLON(55),
        KEY_APOSTROPHE(56),
        KEY_BACKQUOTE(57),
        KEY_COMMA(58),
        KEY_PERIOD(59),
        KEY_SLASH(60),
        KEY_BACKSLASH(61),
        KEY_MINUS(62),
        KEY_EQUAL(63),
        KEY_ENTER(64),
        KEY_SPACE(65),
        KEY_BACKSPACE(66),
        KEY_TAB(67),
        KEY_CAPSLOCK(68),
        KEY_NUMLOCK(69),
        KEY_ESCAPE(70),
        KEY_SCROLLLOCK(71),
        KEY_INSERT(72),
        KEY_DELETE(73),
        KEY_HOME(74),
        KEY_END(75),
        KEY_PAGEUP(76),
        KEY_PAGEDOWN(77),
        KEY_BREAK(78),
        KEY_LSHIFT(79),
        KEY_RSHIFT(80),
        KEY_LALT(81),
        KEY_RALT(82),
        KEY_LCONTROL(83),
        KEY_RCONTROL(84),
        KEY_LWIN(85),
        KEY_RWIN(86),
        KEY_APP(87),
        KEY_UP(88),
        KEY_LEFT(89),
        KEY_DOWN(90),
        KEY_RIGHT(91),
        KEY_F1(92),
        KEY_F2(93),
        KEY_F3(94),
        KEY_F4(95),
        KEY_F5(96),
        KEY_F6(97),
        KEY_F7(98),
        KEY_F8(99),
        KEY_F9(100),
        KEY_F10(101),
        KEY_F11(102),
        KEY_F12(103),
        KEY_CAPSLOCKTOGGLE(104),
        KEY_NUMLOCKTOGGLE(105),
        KEY_SCROLLLOCKTOGGLE(106),
        MOUSE_LEFT(107),
        MOUSE_RIGHT(108),
        MOUSE_MIDDLE(109),
        MOUSE_4(110),
        MOUSE_5(111),
        MOUSE_WHEEL_UP(112),
        MOUSE_WHEEL_DOWN(113),
        KEY_XBUTTON_A(114),
        KEY_XBUTTON_B(115),
        KEY_XBUTTON_X(116),
        KEY_XBUTTON_Y(117),
        KEY_XBUTTON_LEFT_SHOULDER(118),
        KEY_XBUTTON_RIGHT_SHOULDER(119),
        KEY_XBUTTON_BACK(120),
        KEY_XBUTTON_START(121),
        KEY_XBUTTON_STICK1(122),
        KEY_XBUTTON_STICK2(123),
        // 124 - 145
        KEY_XBUTTON_UP(146),
        KEY_XBUTTON_RIGHT(147),
        KEY_XBUTTON_DOWN(148),
        KEY_XBUTTON_LEFT(149),
        KEY_XSTICK1_RIGHT(150),
        KEY_XSTICK1_LEFT(151),
        KEY_XSTICK1_DOWN(152),
        KEY_XSTICK1_UP(153),
        KEY_XBUTTON_LTRIGGER(154),
        KEY_XBUTTON_RTRIGGER(155),
        KEY_XSTICK2_RIGHT(156),
        KEY_XSTICK2_LEFT(157),
        KEY_XSTICK2_DOWN(158),
        KEY_XSTICK2_UP(159);

        Buttons(int i) {
        }

    }

    private static enum Packet {

        net_NOP(0, new PacketHandler() {

            @Override
            public boolean read(BitBuffer bb, List<Object> l) {
                return true;
            }
        }),
        net_Disconnect(1, new PacketHandler() {
        }),
        net_File(2, new PacketHandler() {
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
                l.add("Signon state: " + (bb.getByte() & 0xFF));
                l.add("Spawn count: " + ((long) bb.getInt()));
                return true;
            }
        }),
        svc_Print(7, new PacketHandler() { // 16 in newer protocols

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
//                    while(n-- > 0) {
//                    l.add("Class ID: " + bb.getBits(Math.log(n, 2) + 1));
//                    l.add("Class name: " + bb.getString());
//                    l.add("Datatable name: " + bb.getString());
//                    }
                }
                return true;
            }
        }),
        svc_SetPause(11, new PacketHandler() {
        }),
        svc_CreateStringTable(12, new PacketHandler() {
//
            @Override
            public boolean read(BitBuffer bb, List<Object> l) {
                l.add("Table name: " + bb.getString());
                int m = bb.getShort();
                l.add("Max entries: " + m);
                int n = (int) bb.getBits((int) ((Math.log(m) / Math.log(2)) + 1));
                l.add("Number of entries: " + n);
                long length = bb.getBits(20);
                l.add("Length in bits: " + length);
                boolean f = bb.getBoolean();
                l.add("Userdata fixed size: " + f);
                if(f) {
                    l.add("Userdata size: " + bb.getBits(12));
                    l.add("Userdata bits: " + bb.getBits(4));
                }

                // ???: this is not in Source 2007 netmessages.h/cpp it seems. protocol version?
                l.add("Compressed: " + bb.getBoolean());
                bb.getBits(n);
                return true;
            }
        }),
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
        }),
        svc_Unknown16(16, new PacketHandler() { // svc_Print in newer protocols
        }),
        svc_Sounds(17, new PacketHandler() {

            @Override
            public boolean read(BitBuffer bb, List<Object> l) {
                boolean reliable = bb.getBoolean();
                l.add("Reliable: " + reliable);
                int count = (reliable ? 1 : bb.getByte());
                l.add("Number of sounds: " + count);
                int length = reliable ? bb.getByte() : bb.getShort();
                l.add("Length in bits: " + length);

                bb.getBits(length); // TODO
                return true;
            }
        }),
        svc_SetView(18, new PacketHandler() {
        }),
        svc_FixAngle(19, new PacketHandler() {
        }),
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

                return new Vector3f(
                    hasx ? ReadCoord(bb) : 0,
                    hasy ? ReadCoord(bb) : 0,
                    hasz ? ReadCoord(bb) : 0
                );
            }

            @Override
            boolean read(BitBuffer bb, List<Object> l) {
                l.add("Position: " + ReadVecCoord(bb));
                l.add("Decal texture index: " + bb.getBits(9));
                if(bb.getBoolean()) {
                    l.add("Entity index: " + bb.getBits(11));
                    l.add("Model index: " + bb.getBits(12));
                }
                l.add("Low priority: " + bb.getBoolean());
                return true;
            }

        }),
        svc_unknown2(22, new PacketHandler() { // svc_SplitScreen in newer protocols
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
        svc_EntityMessage(24, new PacketHandler() {
        }),
        svc_GameEvent(25, new PacketHandler() {

            @Override
            boolean read(BitBuffer bb, List<Object> l) {
                int length = (int) bb.getBits(11);
                l.add("Length in bits: " + length);

                int gameEventId = (int) bb.getBits(9);

                String gameEvent = gameEvents[gameEventId];
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
        }),
        svc_Prefetch(28, new PacketHandler() {
        }),
        svc_Menu(29, new PacketHandler() {
        }),
        svc_GameEventList(30, new PacketHandler() {

            @Override
            boolean read(BitBuffer bb, List<Object> l) {
                int numGameEvents = (int) bb.getBits(9);
                gameEvents = new String[MAX_GAME_EVENTS];
                l.add("Number of events: " + numGameEvents);
                int length = (int) bb.getBits(20);
                l.add("Length in bits: " + length);

                for(int i = 0; i < numGameEvents; i++) {
                    int id = (int) bb.getBits(9);
                    String name = bb.getString();

                    l.add("event: [" + id + "] " + name);
                    gameEvents[id] = name;
                    List<Object> sub = new LinkedList<Object>();
                    while(true) {
                        int entryType = (int) bb.getBits(3);

                        if(entryType == 0) { // End of event description
                            break;
                        }

                        String entryName = bb.getString();

                        sub.add("entry: [" + GameEventMessageType.values()[entryType - 1] + "] " + entryName);
                    }
                    l.add(sub);
                }

                return true;
            }

        }),
        svc_GetCvarValue(31, new PacketHandler() {
        }),
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

    private static class DemoHeader {

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
            if(!head.equals(HEADER)) {
                LOG.log(Level.WARNING, "Unexpected header");
            }
            demoProtocol = slice.getInt();
            if(demoProtocol != DEMO_PROTOCOL) {
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

    private static abstract class PacketHandler {

        boolean read(BitBuffer bb, List<Object> l) {
            return false;
        }

    }

    public class Message {

        public ByteBuffer data;

        public final List<Object> meta = new LinkedList<Object>();

        /**
         * Actually 3 bytes
         */
        public final int tick;

        public final MessageType type;

        private Message(ByteBuffer buffer) {
            int b = buffer.get();
            type = MessageType.get(b);
            if(type == null) {
                LOG.log(Level.SEVERE, "Unknown demo message type encountered: {0}", b);
            }
            tick = buffer.getShort() + (buffer.get() << 16);

            if(type != MessageType.Stop) {
                buffer.get();
            }
            LOG.log(Level.FINE, "{0} at tick {1} ({2}), {3} remaining bytes",
                    new Object[] {type, tick, buffer.position(), buffer.remaining()});
        }

        public void parse() {
            if(data == null) {
                return;
            }
            switch(type) {
                case Signon:
                case Packet: {
                    BitBuffer bb = new BitBuffer(data);
                    while(true) {
                        if(bb.remaining() < 1) {
                            break;
                        }
                        int mid = (int) bb.getBits(header.networkProtocol >= 16 ? 6 : 5);
                        Packet p = Packet.get(mid);
                        if(p == null) {
                            meta.add(MessageFormat.format("Unknown message type {0}", mid));
                            break;
                        }
                        List<Object> list = new LinkedList<Object>();
                        list.add(p);
                        boolean complete = p.handler.read(bb, list);
                        if(!complete) {
                            list.add(MessageFormat.format("Incomplete read", mid));
                        }
                        meta.add(list);
                        if(!complete) {
                            break;
                        }
                    }
                }
                break;
                case ConsoleCmd: {
                    ByteBuffer b = data;
                    Level l = Level.FINE;
                    String cmd = DataUtils.getText(b).trim();
                    meta.add(cmd);
                    if(b.remaining() > 0) {
                        LOG.log(l, "Underflow: {0}, {1}", new Object[] {b.remaining(), b.position()});
                    }
                }
                break;
                case UserCmd: { // https://github.com/LestaD/SourceEngine2007/blob/master/se2007/game/shared/usercmd.cpp#L199
                    BitBuffer bb = new BitBuffer(data);
                    if(bb.getBoolean()) {
                        meta.add(MessageFormat.format("Command number: {0}", bb.getInt()));
                    } else {
                        // Assume steady increment
                    }
                    if(bb.getBoolean()) {
                        meta.add(MessageFormat.format("Tick count: {0}", bb.getInt()));
                    } else {
                        // Assume steady increment
                    }
                    if(bb.getBoolean()) {
                        meta.add(MessageFormat.format("Viewangle pitch: {0}", bb.getFloat()));
                    }
                    if(bb.getBoolean()) {
                        meta.add(MessageFormat.format("Viewangle yaw: {0}", bb.getFloat()));
                    }
                    if(bb.getBoolean()) {
                        meta.add(MessageFormat.format("Viewangle roll: {0}", bb.getFloat()));
                    }

                    if(bb.getBoolean()) {
                        meta.add(MessageFormat.format("Foward move: {0}", bb.getFloat()));
                    }
                    if(bb.getBoolean()) {
                        meta.add(MessageFormat.format("Side move: {0}", bb.getFloat()));
                    }
                    if(bb.getBoolean()) {
                        meta.add(MessageFormat.format("Up move: {0}", bb.getFloat()));
                    }

                    if(bb.getBoolean()) {
                        meta.add(MessageFormat.format("Buttons: 0x{0}", Integer.toHexString(bb.getInt())));
                    }

                    if(bb.getBoolean()) {
                        meta.add(MessageFormat.format("Impulse: {0}", bb.getByte()));
                    }

                    if(bb.getBoolean()) {
                        meta.add(MessageFormat.format("Weapon select: {0}", bb.getBits(MAX_EDICT_BITS)));
                        if(bb.getBoolean()) {
                            meta.add(MessageFormat.format("Weapon subtype: {0}", bb.getBits(WEAPON_SUBTYPE_BITS)));
                        }
                    }

                    if(bb.getBoolean()) {
                        meta.add(MessageFormat.format("Mouse Dx: {0}", bb.getShort()));
                    }
                    if(bb.getBoolean()) {
                        meta.add(MessageFormat.format("Mouse Dy: {0}", bb.getShort()));
                    }

                    if(bb.remaining() > 0) {
                        meta.add(MessageFormat.format("Underflow: {0}", bb.remaining()));
                    }
                }
                break;
                // TODO
                case DataTables:
                case StringTables:
                    break;
            }
        }

        @Override
        public String toString() {
            return MessageFormat.format("{0}, tick {1}, {2} bytes", type, tick, data != null ? data.limit() : 0);
        }

    }

}
