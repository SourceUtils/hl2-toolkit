package com.timepath.hl2.io.demo;

import com.timepath.Pair;
import com.timepath.io.BitBuffer;

import java.awt.*;
import java.util.LinkedList;
import java.util.List;

/**
 * @author TimePath
 * @see <a>https://forums.alliedmods.net/showthread.php?t=163224</a>
 * @see <a>https://github.com/LestaD/SourceEngine2007/blob/master/src_main/game/server/util.cpp#L1115</a>
 * @see <a>https://github.com/LestaD/SourceEngine2007/blob/master/se2007/game/shared/hl2/hl2_usermessages.cpp</a>
 * HookMessage, HOOK_HUD_MESSAGE, MsgFunc_<Message>
 */
public enum UserMessage {
    Geiger(0, 1, new PacketHandler() {
        @Override
        boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo, int lengthBits) {
            l.add(new Pair<Object, Object>("Range", (bb.getByte() & 0xFF) * 2));
            return true;
        }
    }),
    Train(1, 1, new PacketHandler() {
        @Override
        boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo, int lengthBits) {
            l.add(new Pair<Object, Object>("Pos", bb.getByte()));
            return true;
        }
    }),
    HudText(2, -1, new PacketHandler() {
        @Override
        boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo, int lengthBits) {
            l.add(new Pair<Object, Object>("Text", bb.getString()));
            return true;
        }
    }),
    SayText(3, -1),
    SayText2(4, -1, new PacketHandler() {
        @Override
        boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo, int lengthBits) {
            int endBit = bb.positionBits() + lengthBits;
            long client = bb.getBits(8);
            l.add(new Pair<Object, Object>("Client", client));
            // 0 - raw text, 1 - sets CHAT_FILTER_PUBLICCHAT
            boolean isRaw = bb.getBits(8) != 0;
            l.add(new Pair<Object, Object>("Raw", isRaw));
            // \x03 in the message for the team color of the specified clientid
            String kind = bb.getString();
            l.add(new Pair<Object, Object>("Kind", kind));
            String from = bb.getString();
            l.add(new Pair<Object, Object>("From", from));
            String msg = bb.getString();
            l.add(new Pair<Object, Object>("Text", msg));
            // This message can have two optional string parameters.
            List<String> args = new LinkedList<>();
            while (bb.positionBits() < endBit) {
                String arg = bb.getString();
                args.add(arg);
            }
            l.add(new Pair<Object, Object>("Args", args));
            return true;
        }
    }),
    TextMsg(5, -1, new PacketHandler() {
        @Override
        boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo, int lengthBits) {
            String[] destination = {
                    "HUD_PRINTCONSOLE", "HUD_PRINTNOTIFY", "HUD_PRINTTALK", "HUD_PRINTCENTER"
            };
            byte msgDest = bb.getByte();
            l.add(new Pair<Object, Object>("Destination", destination[msgDest]));
            l.add(new Pair<Object, Object>("Message", bb.getString()));
            // These seem to be disabled in TF2
//            l.add(new Pair<Object, Object>("args[0]", bb.getString()));
//            l.add(new Pair<Object, Object>("args[1]", bb.getString()));
//            l.add(new Pair<Object, Object>("args[2]", bb.getString()));
//            l.add(new Pair<Object, Object>("args[3]", bb.getString()));
            return true;
        }
    }),
    ResetHUD(6, 1),
    GameTitle(7, 0),
    ItemPickup(8, -1),
    ShowMenu(9, -1),
    Shake(10, 13),
    Fade(11, 10),
    VGUIMenu(12, -1),
    Rumble(13, 3),
    CloseCaption(14, -1),
    SendAudio(15, -1),
    VoiceMask(16, 17),
    RequestState(17, 0),
    Damage(18, -1),
    HintText(19, -1),
    KeyHintText(20, -1),
    /**
     * Position command $position x y
     * x & y are from 0 to 1 to be screen resolution independent
     * -1 means center in each dimension
     * Effect command $effect <effect number>
     * effect 0 is fade in/fade out
     * effect 1 is flickery credits
     * effect 2 is write out (training room)
     * Text color r g b command $color
     * Text color r g b command $color2
     * fadein time fadeout time / hold time
     * $fadein (message fade in time - per character in effect 2)
     * $fadeout (message fade out time)
     * $holdtime (stay on the screen for this long)
     */
    HudMsg(21, -1, new PacketHandler() {
        private static final int MAX_NETMESSAGE = 6;

        @Override
        boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo, int lengthBits) {
            Point pos = new Point(bb.getByte(), bb.getByte());
            Color color = new Color(bb.getByte(), bb.getByte(), bb.getByte(), bb.getByte());
            Color color2 = new Color(bb.getByte(), bb.getByte(), bb.getByte(), bb.getByte());
            float effect = bb.getByte();
            float fadein = bb.getFloat();
            float fadeout = bb.getFloat();
            float holdtime = bb.getFloat();
            float fxtime = bb.getFloat();
            l.add(new Pair<Object, Object>("Text", bb.getString()));
            return true;
        }
    }),
    AmmoDenied(22, 2, new PacketHandler() {
        @Override
        boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo, int lengthBits) {
            l.add(new Pair<Object, Object>("Ammo", bb.getShort() & 0xFFFF));
            return true;
        }
    }),
    AchievementEvent(23, -1),
    UpdateRadar(24, -1),
    VoiceSubtitle(25, 3),
    HudNotify(26, 1),
    HudNotifyCustom(27, -1),
    PlayerStatsUpdate(28, -1),
    PlayerIgnited(29, 3),
    PlayerIgnitedInv(30, 3),
    HudArenaNotify(31, 2),
    UpdateAchievement(32, -1),
    TrainingMsg(33, -1),
    TrainingObjective(34, -1),
    DamageDodged(35, -1),
    PlayerJarated(36, 2),
    PlayerExtinguished(37, 2),
    PlayerJaratedFade(38, 2),
    PlayerShieldBlocked(39, 2),
    BreakModel(40, -1),
    CheapBreakModel(41, -1),
    BreakModel_Pumpkin(42, -1),
    BreakModelRocketDud(43, -1),
    CallVoteFailed(44, -1),
    VoteStart(45, -1),
    VotePass(46, -1),
    VoteFailed(47, 2),
    VoteSetup(48, -1),
    PlayerBonusPoints(49, 3),
    SpawnFlyingBird(50, -1),
    PlayerGodRayEffect(51, -1),
    SPHapWeapEvent(52, 4),
    HapDmg(53, -1),
    HapPunch(54, -1),
    HapSetDrag(55, -1),
    HapSetConst(56, -1),
    HapMeleeContact(57, 0);
    private final int i;
    private final int size;
    private final PacketHandler handler;

    UserMessage(int id, int size) {
        this(id, size, null);
    }

    UserMessage(int i, int size, PacketHandler handler) {
        this.i = i;
        this.size = size;
        this.handler = handler;
    }

    static boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
        int msgType = (int) bb.getByte();
        UserMessage m = get(msgType);
        l.add(new Pair<Object, Object>("Message type", (m != null) ? m.name() : ("Unknown: " + msgType)));
        int length = (int) bb.getBits(11);
        l.add(new Pair<Object, Object>("Length in bits", length));
        l.add(new Pair<Object, Object>("Start bit", bb.positionBits()));
        l.add(new Pair<Object, Object>("End bit", bb.positionBits() + length));
        if ((m == null) || (m.handler == null)) {
            l.add(new Pair<Object, Object>("TODO", msgType));
            bb.getBits(length); // Skip
            return true;
        }
        return m.handler.read(bb, l, demo, length);
    }

    private static UserMessage get(int i) {
        return (i < values().length) ? values()[i] : null;
    }
}
