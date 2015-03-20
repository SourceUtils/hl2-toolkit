package com.timepath.hl2.io.demo


import java.util.LinkedList

/**
 * @author TimePath
 * @see <a>https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/game/shared/in_buttons.h</a>
 */
public enum class Input(private val mask: Int) {
    ATTACK : Input(1)
    JUMP : Input(1 shl 1)
    DUCK : Input(1 shl 2)
    FORWARD : Input(1 shl 3)
    BACK : Input(1 shl 4)
    USE : Input(1 shl 5)
    CANCEL : Input(1 shl 6)
    LEFT : Input(1 shl 7)
    RIGHT : Input(1 shl 8)
    MOVELEFT : Input(1 shl 9)
    MOVERIGHT : Input(1 shl 10)
    ATTACK2 : Input(1 shl 11)
    RUN : Input(1 shl 12)
    RELOAD : Input(1 shl 13)
    ALT1 : Input(1 shl 14)
    ALT2 : Input(1 shl 15)
    SCORE : Input(1 shl 16)
    SPEED : Input(1 shl 17)
    WALK : Input(1 shl 18)
    /**
     * Zoom key for HUD zoom
     */
    ZOOM : Input(1 shl 19)
    /**
     * Weapon defines these bits
     */
    WEAPON1 : Input(1 shl 20)
    /**
     * Weapon defines these bits
     */
    WEAPON2 : Input(1 shl 21)
    BULLRUSH : Input(1 shl 22)
    GRENADE1 : Input(1 shl 23)
    GRENADE2 : Input(1 shl 24)
    ATTACK3 : Input(1 shl 25)

    companion object {
        public fun get(bits: Int): List<Input> {
            val l = LinkedList<Input>()
            for (name in Input.values()) {
                if ((name.mask and bits) != 0) {
                    l.add(name)
                }
            }
            return l
        }
    }
}
