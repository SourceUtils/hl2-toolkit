package com.timepath.hl2.io.demo

import com.timepath.io.BitBuffer
import com.timepath.toUnsigned
import java.awt.Color
import java.awt.Point
import java.util.LinkedList

/**
 * @see [https://forums.alliedmods.net/showthread.php?t=163224](null)
 * @see [https://github.com/LestaD/SourceEngine2007/blob/master/src_main/game/server/util.cpp.L1115](null)
 * @see [https://github.com/LestaD/SourceEngine2007/blob/master/se2007/game/shared/hl2/hl2_usermessages.cpp](null)
 * HookMessage, HOOK_HUD_MESSAGE, MsgFunc_
 */
public enum class UserMessage(private val id: Int, private val size: Int,
                              private open val handler: PacketHandler? = null) {

    Geiger(0, 1) {
        override val handler = PacketHandler { bb, l, demo, lengthBits ->
            l["Range"] = bb.getByte().toUnsigned() * 2
            true
        }
    },
    Train(1, 1) {
        override val handler = PacketHandler { bb, l, demo, lengthBits ->
            l["Pos"] = bb.getByte()
            true
        }
    },
    HudText(2, -1) {
        override val handler = PacketHandler { bb, l, demo, lengthBits ->
            l["Text"] = bb.getString()
            true
        }
    },
    SayText(3, -1),
    SayText2(4, -1) {
        override val handler = PacketHandler { bb, l, demo, lengthBits ->
            val endBit = bb.positionBits() + lengthBits
            val client = bb.getBits(8)
            l["Client"] = client
            // 0 - raw text, 1 - sets CHAT_FILTER_PUBLICCHAT
            val isRaw = bb.getBits(8) != 0L
            l["Raw"] = isRaw
            // \x03 in the message for the team color of the specified clientid
            val kind = bb.getString()
            l["Kind"] = kind
            val from = bb.getString()
            l["From"] = from
            val msg = bb.getString()
            l["Text"] = msg
            // This message can have two optional string parameters.
            val args = LinkedList<String>()
            while (bb.positionBits() < endBit) {
                val arg = bb.getString()
                args.add(arg)
            }
            l["Args"] = args
            true
        }
    },
    TextMsg(5, -1) {
        override val handler = PacketHandler { bb, l, demo, lengthBits ->
            val destination = arrayOf("HUD_PRINTCONSOLE", "HUD_PRINTNOTIFY", "HUD_PRINTTALK", "HUD_PRINTCENTER")
            val msgDest = bb.getByte()
            l["Destination"] = destination[msgDest.toInt()]
            l["Message"] = bb.getString()
            // These seem to be disabled in TF2
            // l.add(new Pair<Object, Object>("args[0]", bb.getString()));
            // l.add(new Pair<Object, Object>("args[1]", bb.getString()));
            // l.add(new Pair<Object, Object>("args[2]", bb.getString()));
            // l.add(new Pair<Object, Object>("args[3]", bb.getString()));
            true
        }
    },
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
     * Effect command $effect
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
    HudMsg(21, -1) {
        override val handler = PacketHandler { bb, l, demo, lengthBits ->
            val pos = Point(bb.getByte().toInt(), bb.getByte().toInt())
            val color = Color(bb.getByte().toInt(), bb.getByte().toInt(), bb.getByte().toInt(), bb.getByte().toInt())
            val color2 = Color(bb.getByte().toInt(), bb.getByte().toInt(), bb.getByte().toInt(), bb.getByte().toInt())
            val effect = bb.getByte().toFloat()
            val fadein = bb.getFloat()
            val fadeout = bb.getFloat()
            val holdtime = bb.getFloat()
            val fxtime = bb.getFloat()
            l["Text"] = bb.getString()
            true
        }

        private val MAX_NETMESSAGE = 6
    },
    AmmoDenied(22, 2) {
        override val handler = PacketHandler { bb, l, demo, lengthBits ->
            l["Ammo"] = bb.getShort().toUnsigned()
            true
        }
    },
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

    companion object {
        fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM): Boolean {
            val msgType = bb.getByte().toInt()
            val m = UserMessage[msgType]
            l["Message type"] = (m?.name() ?: "Unknown: $msgType")
            val length = bb.getBits(11).toInt()
            l["Length in bits"] = length
            l["Start bit"] = bb.positionBits()
            l["End bit"] = bb.positionBits() + length
            m?.handler?.let { return it.read(bb, l, demo, length) }
            l["TODO"] = msgType
            bb.getBits(length) // Skip
            return true
        }

        fun get(i: Int) = values().firstOrNull { it.id == i }
    }
}
