package com.timepath.hl2.io;

import com.timepath.DataUtils;
import com.timepath.nio.BitBuffer;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * https://code.google.com/p/coldemoplayer
 * https://code.google.com/p/coldemoplayer/source/browse/trunk/compLexity%20Demo%20Player/demo/SourceDemo.cs
 * https://code.google.com/p/coldemoplayer/source/browse/trunk/compLexity%20Demo%20Player/demo%20parser/SourceDemoParser.cs
 * 
 * https://github.com/stgn/netdecode
 * https://github.com/stgn/netdecode/issues/1
 * https://github.com/stgn/netdecode/blob/master/DemoFile.cs
 * https://github.com/stgn/netdecode/blob/master/Packet.cs
 * 
 * http://hg.alliedmods.net/hl2sdks/hl2sdk-css/file/1901d5b74430/public/demofile/demoformat.h
 *
 * @author timepath
 */
public class HL2DEM {

    /**
     * hl2sdk-ob-valve/public/inputsystem/ButtonCode.h
     */
    enum Buttons {

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
        // 123
        // 146
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

    private static final Logger LOG = Logger.getLogger(HL2DEM.class.getName());

    private static final String HEADER = "HL2DEMO\0";

    private static final int DEMO_PROTOCOL = 3;

    public static HL2DEM load(File f) throws IOException {
        ByteBuffer buffer = DataUtils.mapFile(f);
        HL2DEM dem = new HL2DEM();

        //<editor-fold defaultstate="collapsed" desc="Header">
        ByteBuffer header = DataUtils.getSlice(buffer, 1072);

        String head = DataUtils.getText(DataUtils.getSlice(header, 8));
        if(!head.equals(HEADER)) {
            LOG.log(Level.WARNING, "Unexpected header");
            return null;
        }
        int demoProtocol = header.getInt();
        if(demoProtocol != DEMO_PROTOCOL) {
            LOG.log(Level.WARNING, "Unknown demo version {0}", demoProtocol);
            return null;
        }
        int networkProtocol = header.getInt();
        LOG.log(Level.INFO, "Network protocol: {0}", networkProtocol);

        ByteBuffer serverNameBuffer = DataUtils.getSlice(header, 260);
        String serverName = DataUtils.getText(serverNameBuffer).trim();
        LOG.log(Level.INFO, "Server: {0}", serverName);
        ByteBuffer clientNameBuffer = DataUtils.getSlice(header, 260);
        String clientName = DataUtils.getText(clientNameBuffer).trim();
        LOG.log(Level.INFO, "Client: {0}", clientName);
        ByteBuffer mapNameBuffer = DataUtils.getSlice(header, 260);
        String mapName = DataUtils.getText(mapNameBuffer).trim();
        LOG.log(Level.INFO, "Map: {0}", mapName);
        ByteBuffer gameDirectoryBuffer = DataUtils.getSlice(header, 260);
        String gameDirectory = DataUtils.getText(gameDirectoryBuffer).trim();
        LOG.log(Level.INFO, "Game: {0}", gameDirectory);

        float playbackTime = header.getFloat();
        LOG.log(Level.INFO, "Playback time: {0}", playbackTime);
        int ticks = header.getInt();
        LOG.log(Level.INFO, "Ticks: {0}", ticks);
        int frames = header.getInt();
        LOG.log(Level.INFO, "Frames: {0}", frames);
        int signonLength = header.getInt();
        LOG.log(Level.INFO, "Signon length: {0}", signonLength);
        //</editor-fold>

        while(true) {
            // frame header
            Message msg = new Message();
            msg.type = MessageType.get(buffer.get());
            if(msg.type == null) {
                throw new IOException("Unknown demo message type encountered.");
            }
            if(msg.type == MessageType.Stop) {
                LOG.log(Level.INFO, "Stopping at {0}, {1} remaining bytes", new Object[]{buffer.position(), buffer.remaining()});
                break;
            }
            msg.tick = buffer.getInt();

            switch(msg.type) {
                case Signon:
                case Packet:
                case Console:
                case UserCmd:
                case DataTables:
                case StringTables:
                    switch(msg.type) {
                        case Packet:
                        case Signon:
                            buffer.get(new byte[84]); // command/sequence info
                            break;
                        case UserCmd:
                            buffer.get(new byte[4]); // unknown
                            break;
                    }
                    msg.data = buffer.get(new byte[buffer.getInt()]);
                    if(buffer.position() < 2000000) {
                        continue;
                    }
                    switch(msg.type) {
                        case UserCmd:
                            LOG.log(Level.INFO, "UserCommand at {0} ({1})", new Object[]{msg.tick, buffer.position()});
                            BitBuffer bb = new BitBuffer((ByteBuffer) msg.data);
                            Level l = Level.INFO;
                            if(bb.ReadBool()) {
                                LOG.log(l, "Command number: {0}", bb.ReadBits(32));
                            }
                            if(bb.ReadBool()) {
                                LOG.log(l, "Tick count: {0}", bb.ReadBits(32));
                            }
                            if(bb.ReadBool()) {
                                LOG.log(l, "Viewangle pitch: {0}", bb.ReadFloat());
                            }
                            if(bb.ReadBool()) {
                                LOG.log(l, "Viewangle yaw: {0}", bb.ReadFloat());
                            }
                            if(bb.ReadBool()) {
                                LOG.log(l, "Viewangle roll: {0}", bb.ReadFloat());
                            }
                            if(bb.ReadBool()) {
                                LOG.log(l, "Foward move: {0}", bb.ReadFloat());
                            }
                            if(bb.ReadBool()) {
                                LOG.log(l, "Side move: {0}", bb.ReadFloat());
                            }
                            if(bb.ReadBool()) {
                                LOG.log(l, "Up move: {0}", bb.ReadFloat());
                            }
                            if(bb.ReadBool()) {
                                LOG.log(l, "Buttons: 0x{0}", Integer.toOctalString(bb.ReadBits(32)));
                            }
                            if(bb.ReadBool()) {
                                LOG.log(l, "Impulse: {0}", bb.ReadBits(8));
                            }
                            break;
                    }
                    break;
                case Synctick:
                    break;
            }

        }
        return dem;
    }

    //<editor-fold defaultstate="collapsed" desc="Messages">
    static class Message {
        
        MessageType type;
        
        int tick;
        
        Object data;
        
        Message() {
        }
    }
    
    static enum MessageType {
        
        Signon(1),
        Packet(2),
        Synctick(3),
        Console(4),
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
    //</editor-fold>
    
    private HL2DEM() {
    }
}
