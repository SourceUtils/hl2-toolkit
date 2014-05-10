package com.timepath.hl2.io.demo;

import com.timepath.Pair;
import com.timepath.io.BitBuffer;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * https://forums.alliedmods.net/showthread.php?t=163224
 * 
 * @author TimePath
 */
public enum UserMessage {

    Geiger(0, 1),
    Train(1, 1),
    HudText(2, -1),
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
            List<String> args = new LinkedList<String>();
            while (bb.positionBits() < endBit) {
                String arg = bb.getString();
                args.add(arg);
            }
            l.add(new Pair<Object, Object>("Args", args));
            return true;
        }

    }),
    TextMsg(5, -1),
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
    HudMsg(21, -1),
    AmmoDenied(22, 2),
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

    static boolean read(BitBuffer bb, List<Pair<Object, Object>> l, HL2DEM demo) {
        int userMsgType = (int) bb.getBits(8);
        UserMessage m = UserMessage.get(userMsgType);
        l.add(new Pair<Object, Object>("Message type", m != null ? m.name() : "Unknown: " + userMsgType));
        int length = (int) bb.getBits(11);
        l.add(new Pair<Object, Object>("Length in bits", length));
        if (m == null || m.handler == null) {
            l.add(new Pair<Object, Object>("TODO", userMsgType));
            bb.getBits(length); // TODO
            return true;
        }
        return m.handler.read(bb, l, demo, length);
    }
    private int i;
    private int size;
    private PacketHandler handler;

    UserMessage(int i, int size, PacketHandler handler) {
        this.i = i;
        this.size = size;
        this.handler = handler;
    }

    UserMessage(int id, int size) {
        this(id, size, null);
    }

    public static UserMessage get(int i) {
        return i < values().length ? values()[i] : null;
    }

}
