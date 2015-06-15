package com.timepath.hl2.io.demo

public enum class MessageType(private val id: Int) {

    Signon(1),
    Packet(2),
    Synctick(3),
    ConsoleCmd(4),
    UserCmd(5),
    DataTables(6),
    Stop(7),
    StringTables(8);

    companion object {
        fun get(i: Int): MessageType? = values().first { it.id == i }
    }
}
