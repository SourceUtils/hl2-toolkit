package com.timepath.hl2.io.demo


/**
 * @see <a>https://github.com/ValveSoftware/source-sdk-2013/blob/master/mp/src/game/shared/in_buttons.h</a>
 */
public enum class Input(bit: Int) {

    ATTACK(0),
    JUMP(1),
    DUCK(2),
    FORWARD(3),
    BACK(4),
    USE(5),
    CANCEL(6),
    LEFT(7),
    RIGHT(8),
    MOVELEFT(9),
    MOVERIGHT(10),
    ATTACK2(11),
    RUN(12),
    RELOAD(13),
    ALT1(14),
    ALT2(15),
    SCORE(16),
    SPEED(17),
    WALK(18),
    /**
     * Zoom key for HUD zoom
     */
    ZOOM(19),
    /**
     * Weapon defines these bits
     */
    WEAPON1(20),
    /**
     * Weapon defines these bits
     */
    WEAPON2(21),
    BULLRUSH(22),
    GRENADE1(23),
    GRENADE2(24),
    ATTACK3(25);

    private val mask = 1 shl bit

    companion object {
        public fun get(bits: Int): Set<Input> = Input.values().filter { (it.mask and bits) != 0 }.toSet()
    }
}
