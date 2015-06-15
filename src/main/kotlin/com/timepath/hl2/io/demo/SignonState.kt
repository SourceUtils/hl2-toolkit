package com.timepath.hl2.io.demo

public enum class SignonState {
    /** 0: No state yet; about to connect */
    NONE,
    /** 1: Client challenging server; all OOB packets */
    CHALLENGE,
    /** 2: Client is connected to server; netchans ready */
    CONNECTED,
    /** 3: Just got serverinfo and string tables */
    NEW,
    /** 4: Received signon buffers */
    PRESPAWN,
    /** 5: Ready to receive entity packets */
    SPAWN,
    /** 6: Fully connected; first non-delta packet received */
    FULL,
    /** 7: Server is changing level; please wait */
    CHANGELEVEL
}
