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
public abstract class UserMessage(private val size: Int = -1) : PacketHandler {

    override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean = size == -1

    object Geiger : UserMessage(1) {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            l["Range"] = bb.getByte().toUnsigned() * 2
            return true
        }
    }

    object Train : UserMessage(1) {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            l["Pos"] = bb.getByte()
            return true
        }
    }

    object HudText : UserMessage() {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            l["Text"] = bb.getString()
            return true
        }
    }

    object SayText : UserMessage()

    object SayText2 : UserMessage() {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
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
            return true
        }
    }

    object TextMsg : UserMessage() {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            val destination = arrayOf("HUD_PRINTCONSOLE", "HUD_PRINTNOTIFY", "HUD_PRINTTALK", "HUD_PRINTCENTER")
            val msgDest = bb.getByte()
            l["Destination"] = destination[msgDest.toInt()]
            l["Message"] = bb.getString()
            // These seem to be disabled in TF2
            // l.add(new Pair<Object, Object>("args[0]", bb.getString()));
            // l.add(new Pair<Object, Object>("args[1]", bb.getString()));
            // l.add(new Pair<Object, Object>("args[2]", bb.getString()));
            // l.add(new Pair<Object, Object>("args[3]", bb.getString()));
            return true
        }
    }

    object ResetHUD : UserMessage(1)

    object GameTitle : UserMessage(0)

    object ItemPickup : UserMessage()

    object ShowMenu : UserMessage()

    object Shake : UserMessage(13)

    object Fade : UserMessage(10)

    object VGUIMenu : UserMessage()

    object Rumble : UserMessage(3)

    object CloseCaption : UserMessage()

    object SendAudio : UserMessage()

    object VoiceMask : UserMessage(17)

    object RequestState : UserMessage(0)

    object Damage : UserMessage()

    object HintText : UserMessage()

    object KeyHintText : UserMessage()

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
    object HudMsg : UserMessage() {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            val pos = Point(bb.getByte().toInt(), bb.getByte().toInt())
            val color = Color(bb.getByte().toInt(), bb.getByte().toInt(), bb.getByte().toInt(), bb.getByte().toInt())
            val color2 = Color(bb.getByte().toInt(), bb.getByte().toInt(), bb.getByte().toInt(), bb.getByte().toInt())
            val effect = bb.getByte().toFloat()
            val fadein = bb.getFloat()
            val fadeout = bb.getFloat()
            val holdtime = bb.getFloat()
            val fxtime = bb.getFloat()
            l["Text"] = bb.getString()
            return true
        }

        private val MAX_NETMESSAGE = 6
    }

    object AmmoDenied : UserMessage(2) {
        override fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM, lengthBits: Int): Boolean {
            l["Ammo"] = bb.getShort().toUnsigned()
            return true
        }
    }

    object AchievementEvent : UserMessage()

    object UpdateRadar : UserMessage()

    object VoiceSubtitle : UserMessage(3)

    object HudNotify : UserMessage(1)

    object HudNotifyCustom : UserMessage()

    object PlayerStatsUpdate : UserMessage()

    object PlayerIgnited : UserMessage(3)

    object PlayerIgnitedInv : UserMessage(3)

    object HudArenaNotify : UserMessage(2)

    object UpdateAchievement : UserMessage()

    object TrainingMsg : UserMessage()

    object TrainingObjective : UserMessage()

    object DamageDodged : UserMessage()

    object PlayerJarated : UserMessage(2)

    object PlayerExtinguished : UserMessage(2)

    object PlayerJaratedFade : UserMessage(2)

    object PlayerShieldBlocked : UserMessage(2)

    object BreakModel : UserMessage()

    object CheapBreakModel : UserMessage()

    object BreakModel_Pumpkin : UserMessage()

    object BreakModelRocketDud : UserMessage()

    object CallVoteFailed : UserMessage()

    object VoteStart : UserMessage()

    object VotePass : UserMessage()

    object VoteFailed : UserMessage(2)

    object VoteSetup : UserMessage()

    object PlayerBonusPoints : UserMessage(3)

    object SpawnFlyingBird : UserMessage()

    object PlayerGodRayEffect : UserMessage()

    object SPHapWeapEvent : UserMessage(4)

    object HapDmg : UserMessage()

    object HapPunch : UserMessage()

    object HapSetDrag : UserMessage()

    object HapSetConst : UserMessage()

    object HapMeleeContact : UserMessage(0)

    companion object {
        fun read(bb: BitBuffer, l: TupleMap<Any, Any>, demo: HL2DEM): Boolean {
            val msgType = bb.getByte().toInt()
            val m = UserMessage[msgType]
            l["Message"] = m?.let { "${it.javaClass.getSimpleName()}(${msgType})" } ?: "Unknown($msgType)"
            val length = bb.getBits(11).toInt()
            l["Length"] = length
            l["Range"] = bb.positionBits() to bb.positionBits() + length
            m?.let {
                if (it.read(bb, l, demo, length)) return true
                l["Status"] = "Error"
                return false
            }
            l["Status"] = "TODO"
            bb.getBits(length) // Skip
            return true
        }

        fun get(i: Int): UserMessage? = values[i]

        private val values = mapOf(
                0 to Geiger,
                1 to Train,
                2 to HudText,
                3 to SayText,
                4 to SayText2,
                5 to TextMsg,
                6 to ResetHUD,
                7 to GameTitle,
                8 to ItemPickup,
                9 to ShowMenu,
                10 to Shake,
                11 to Fade,
                12 to VGUIMenu,
                13 to Rumble,
                14 to CloseCaption,
                15 to SendAudio,
                16 to VoiceMask,
                17 to RequestState,
                18 to Damage,
                19 to HintText,
                20 to KeyHintText,
                21 to HudMsg,
                22 to AmmoDenied,
                23 to AchievementEvent,
                24 to UpdateRadar,
                25 to VoiceSubtitle,
                26 to HudNotify,
                27 to HudNotifyCustom,
                28 to PlayerStatsUpdate,
                29 to PlayerIgnited,
                30 to PlayerIgnitedInv,
                31 to HudArenaNotify,
                32 to UpdateAchievement,
                33 to TrainingMsg,
                34 to TrainingObjective,
                35 to DamageDodged,
                36 to PlayerJarated,
                37 to PlayerExtinguished,
                38 to PlayerJaratedFade,
                39 to PlayerShieldBlocked,
                40 to BreakModel,
                41 to CheapBreakModel,
                42 to BreakModel_Pumpkin,
                43 to BreakModelRocketDud,
                44 to CallVoteFailed,
                45 to VoteStart,
                46 to VotePass,
                47 to VoteFailed,
                48 to VoteSetup,
                49 to PlayerBonusPoints,
                50 to SpawnFlyingBird,
                51 to PlayerGodRayEffect,
                52 to SPHapWeapEvent,
                53 to HapDmg,
                54 to HapPunch,
                55 to HapSetDrag,
                56 to HapSetConst,
                57 to HapMeleeContact
        )
    }
}
