package com.timepath.hl2.io.demo

public enum class MessageType(private val id: Int) {

    Signon : MessageType(1)
    Packet : MessageType(2)
    Synctick : MessageType(3)
    ConsoleCmd : MessageType(4)
    UserCmd : MessageType(5)
    DataTables : MessageType(6)
    Stop : MessageType(7)
    StringTables : MessageType(8)

    companion object {
        fun get(i: Int): MessageType? = values().first { it.id == i }
    }
}
