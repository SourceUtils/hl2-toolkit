package com.timepath.hl2.io.demo

import com.timepath.io.BitBuffer
import java.awt.Color
import java.awt.Point
import java.util.LinkedList

/**
 * @author TimePath
 * *
 * @see [https://forums.alliedmods.net/showthread.php?t=163224](null)

 * @see [https://github.com/LestaD/SourceEngine2007/blob/master/src_main/game/server/util.cpp.L1115](null)

 * @see [https://github.com/LestaD/SourceEngine2007/blob/master/se2007/game/shared/hl2/hl2_usermessages.cpp](null)
 * HookMessage, HOOK_HUD_MESSAGE, MsgFunc_
 */
public enum class UserMessage(private val i: Int, private val size: Int, private val handler: PacketHandler? = null) {

    Geiger : UserMessage(0, 1, object : PacketHandler {
        override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
            l.add(("Range" to (bb.getByte().toInt() and 255) * 2))
            return true
        }
    })
    Train : UserMessage(1, 1, object : PacketHandler {
        override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
            l.add(("Pos" to bb.getByte()))
            return true
        }
    })
    HudText : UserMessage(2, -1, object : PacketHandler {
        override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
            l.add(("Text" to bb.getString()))
            return true
        }
    })
    SayText : UserMessage(3, -1)
    SayText2 : UserMessage(4, -1, object : PacketHandler {
        override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
            val endBit = bb.positionBits() + lengthBits
            val client = bb.getBits(8)
            l.add(("Client" to client))
            // 0 - raw text, 1 - sets CHAT_FILTER_PUBLICCHAT
            val isRaw = bb.getBits(8) != 0L
            l.add(("Raw" to isRaw))
            // \x03 in the message for the team color of the specified clientid
            val kind = bb.getString()
            l.add(("Kind" to kind))
            val from = bb.getString()
            l.add(("From" to from))
            val msg = bb.getString()
            l.add(("Text" to msg))
            // This message can have two optional string parameters.
            val args = LinkedList<String>()
            while (bb.positionBits() < endBit) {
                val arg = bb.getString()
                args.add(arg)
            }
            l.add(("Args" to args))
            return true
        }
    })
    TextMsg : UserMessage(5, -1, object : PacketHandler {
        override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
            val destination = array("HUD_PRINTCONSOLE", "HUD_PRINTNOTIFY", "HUD_PRINTTALK", "HUD_PRINTCENTER")
            val msgDest = bb.getByte()
            l.add(("Destination" to destination[msgDest.toInt()]))
            l.add(("Message" to bb.getString()))
            // These seem to be disabled in TF2
            //            l.add(new Pair<Object, Object>("args[0]", bb.getString()));
            //            l.add(new Pair<Object, Object>("args[1]", bb.getString()));
            //            l.add(new Pair<Object, Object>("args[2]", bb.getString()));
            //            l.add(new Pair<Object, Object>("args[3]", bb.getString()));
            return true
        }
    })
    ResetHUD : UserMessage(6, 1)
    GameTitle : UserMessage(7, 0)
    ItemPickup : UserMessage(8, -1)
    ShowMenu : UserMessage(9, -1)
    Shake : UserMessage(10, 13)
    Fade : UserMessage(11, 10)
    VGUIMenu : UserMessage(12, -1)
    Rumble : UserMessage(13, 3)
    CloseCaption : UserMessage(14, -1)
    SendAudio : UserMessage(15, -1)
    VoiceMask : UserMessage(16, 17)
    RequestState : UserMessage(17, 0)
    Damage : UserMessage(18, -1)
    HintText : UserMessage(19, -1)
    KeyHintText : UserMessage(20, -1)
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
    HudMsg : UserMessage(21, -1, object : PacketHandler {
        override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
            val pos = Point(bb.getByte().toInt(), bb.getByte().toInt())
            val color = Color(bb.getByte().toInt(), bb.getByte().toInt(), bb.getByte().toInt(), bb.getByte().toInt())
            val color2 = Color(bb.getByte().toInt(), bb.getByte().toInt(), bb.getByte().toInt(), bb.getByte().toInt())
            val effect = bb.getByte().toFloat()
            val fadein = bb.getFloat()
            val fadeout = bb.getFloat()
            val holdtime = bb.getFloat()
            val fxtime = bb.getFloat()
            l.add(("Text" to bb.getString()))
            return true
        }

        private val MAX_NETMESSAGE = 6
    })
    AmmoDenied : UserMessage(22, 2, object : PacketHandler {
        override fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM, lengthBits: Int): Boolean {
            l.add("Ammo" to (bb.getShort().toInt() and 65535))
            return true
        }
    })
    AchievementEvent : UserMessage(23, -1)
    UpdateRadar : UserMessage(24, -1)
    VoiceSubtitle : UserMessage(25, 3)
    HudNotify : UserMessage(26, 1)
    HudNotifyCustom : UserMessage(27, -1)
    PlayerStatsUpdate : UserMessage(28, -1)
    PlayerIgnited : UserMessage(29, 3)
    PlayerIgnitedInv : UserMessage(30, 3)
    HudArenaNotify : UserMessage(31, 2)
    UpdateAchievement : UserMessage(32, -1)
    TrainingMsg : UserMessage(33, -1)
    TrainingObjective : UserMessage(34, -1)
    DamageDodged : UserMessage(35, -1)
    PlayerJarated : UserMessage(36, 2)
    PlayerExtinguished : UserMessage(37, 2)
    PlayerJaratedFade : UserMessage(38, 2)
    PlayerShieldBlocked : UserMessage(39, 2)
    BreakModel : UserMessage(40, -1)
    CheapBreakModel : UserMessage(41, -1)
    BreakModel_Pumpkin : UserMessage(42, -1)
    BreakModelRocketDud : UserMessage(43, -1)
    CallVoteFailed : UserMessage(44, -1)
    VoteStart : UserMessage(45, -1)
    VotePass : UserMessage(46, -1)
    VoteFailed : UserMessage(47, 2)
    VoteSetup : UserMessage(48, -1)
    PlayerBonusPoints : UserMessage(49, 3)
    SpawnFlyingBird : UserMessage(50, -1)
    PlayerGodRayEffect : UserMessage(51, -1)
    SPHapWeapEvent : UserMessage(52, 4)
    HapDmg : UserMessage(53, -1)
    HapPunch : UserMessage(54, -1)
    HapSetDrag : UserMessage(55, -1)
    HapSetConst : UserMessage(56, -1)
    HapMeleeContact : UserMessage(57, 0)

    companion object {
        fun read(bb: BitBuffer, l: MutableList<Pair<Any, Any>>, demo: HL2DEM): Boolean {
            val msgType = bb.getByte().toInt()
            val m = UserMessage[msgType]
            l.add(("Message type" to if ((m != null)) m.name() else ("Unknown: " + msgType)))
            val length = bb.getBits(11).toInt()
            l.add(("Length in bits" to length))
            l.add(("Start bit" to bb.positionBits()))
            l.add(("End bit" to bb.positionBits() + length))
            if ((m == null) || (m.handler == null)) {
                l.add(("TODO" to msgType))
                bb.getBits(length) // Skip
                return true
            }
            return m.handler.read(bb, l, demo, length)
        }

        private fun get(i: Int): UserMessage? = values().let {
            when {
                i < it.size() -> it[i]
                else -> null
            }
        }
    }
}
