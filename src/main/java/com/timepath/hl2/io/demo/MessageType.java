package com.timepath.hl2.io.demo;

import org.jetbrains.annotations.Nullable;

/**
 * @author TimePath
 */
public enum MessageType {
    Signon(1),
    Packet(2),
    Synctick(3),
    ConsoleCmd(4),
    UserCmd(5),
    DataTables(6),
    Stop(7),
    StringTables(8);

    MessageType(int i) {
    }

    @Nullable
    static MessageType get(int i) {
        MessageType[] vals = MessageType.values();
        if ((i < 1) || (i > vals.length)) {
            return null;
        }
        return vals[i - 1];
    }
}
