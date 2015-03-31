package com.timepath.hl2.io.demo

/**
 * @author TimePath
 */
public enum class MessageType(i: Int) {
    Signon : MessageType(1)
    Packet : MessageType(2)
    Synctick : MessageType(3)
    ConsoleCmd : MessageType(4)
    UserCmd : MessageType(5)
    DataTables : MessageType(6)
    Stop : MessageType(7)
    StringTables : MessageType(8)

    companion object {
        fun get(i: Int): MessageType? {
            val vals = MessageType.values()
            if ((i < 1) || (i > vals.size())) {
                return null
            }
            return vals[i - 1]
        }
    }
}
