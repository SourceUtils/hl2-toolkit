package com.timepath.hl2.io.demo

public enum class SignonState(val i: Int) {
    /** No state yet; about to connect */
    NONE(0),
    /** Client challenging server; all OOB packets */
    CHALLENGE(1),
    /** Client is connected to server; netchans ready */
    CONNECTED(2),
    /** Just got serverinfo and string tables */
    NEW(3),
    /** Received signon buffers */
    PRESPAWN(4),
    /** Ready to receive entity packets */
    SPAWN(5),
    /** Fully connected; first non-delta packet received */
    FULL(6),
    /** Server is changing level; please wait */
    CHANGELEVEL(7);

    companion object {
        fun get(i: Int) = values().firstOrNull { it.i == i }
    }

}
