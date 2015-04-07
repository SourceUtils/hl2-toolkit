package com.timepath.hl2.io.demo


/**
 * @see <a>https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/game/shared/in_buttons.h</a>
 */
public enum class Input(bit: Int) {

    private val mask = 1 shl bit

    ATTACK : Input(0)
    JUMP : Input(1)
    DUCK : Input(2)
    FORWARD : Input(3)
    BACK : Input(4)
    USE : Input(5)
    CANCEL : Input(6)
    LEFT : Input(7)
    RIGHT : Input(8)
    MOVELEFT : Input(9)
    MOVERIGHT : Input(10)
    ATTACK2 : Input(11)
    RUN : Input(12)
    RELOAD : Input(13)
    ALT1 : Input(14)
    ALT2 : Input(15)
    SCORE : Input(16)
    SPEED : Input(17)
    WALK : Input(18)
    /**
     * Zoom key for HUD zoom
     */
    ZOOM : Input(19)
    /**
     * Weapon defines these bits
     */
    WEAPON1 : Input(20)
    /**
     * Weapon defines these bits
     */
    WEAPON2 : Input(21)
    BULLRUSH : Input(22)
    GRENADE1 : Input(23)
    GRENADE2 : Input(24)
    ATTACK3 : Input(25)

    companion object {
        public fun get(bits: Int): Set<Input> = Input.values().filter { (it.mask and bits) != 0 }.toSet()
    }
}
