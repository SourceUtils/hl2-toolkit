package com.timepath.hl2.io.net

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Arrays
import java.util.Random

val CONNECTIONLESS_HEADER = 0xffffffff.toInt()

val PROTOCOL_VERSION = 24

data class A2S_GETCHALLENGE(
        val myKey: Int = Random().nextInt()
) : Sendable {
    companion object {
        val ID = 'q'.toByte()
    }

    override fun send() = with(Packet()) {
        writeLong(CONNECTIONLESS_HEADER)
        writeByte(ID)
        writeLong(myKey)
        writeString("0" repeat 10)
        this
    }
}

enum class Protocol {
    Unknown,
    AuthCertificate,
    HashedCDKey,
    Steam;

    val i = ordinal()

    companion object {
        private val _values = values()
        fun get(i: Int) = _values[i]
    }
}

data class S2C_CHALLENGE(
        val challenge: Int,
        val protocol: Protocol
) {
    companion object : Readable<S2C_CHALLENGE> {
        val ID = 'A'.toByte()

        override fun read(it: Packet): S2C_CHALLENGE {
            check(it.readLong() == CONNECTIONLESS_HEADER)
            check(it.readByte() == S2C_CHALLENGE.ID)
            val challenge = it.readLong() // very stable
            val dummy1 = it.readLong() // relatively unstable (5s)
            val dummy2 = it.readLong() // unstable (1s)
            val authprotocol = it.readLong()
            val keysize = it.readShort()
            val key = it.readBytes(keysize.toInt())
            val sid1 = it.readLong()
            val sid2 = it.readLong()
            val secure = it.readByte()
            check(it.readString() == "000000")
            return S2C_CHALLENGE(
                    challenge = challenge,
                    protocol = Protocol.get(authprotocol)
            )
        }
    }
}

data class C2S_CONNECT(
        val challenge: S2C_CHALLENGE,
        val name: String,
        val password: kotlin.String = ""
) : Sendable {
    companion object {
        val ID = 'k'.toByte()
    }

    override fun send() = kotlin.with(Packet()) {
        writeLong(CONNECTIONLESS_HEADER)
        writeByte(ID)
        writeLong(PROTOCOL_VERSION)
        writeLong(challenge.protocol.i)
        writeLong(challenge.challenge)
        writeString(name)
        writeString(password)
        this
    }
}

fun main(args: Array<String>): Unit {
    require(args.size() == 2, "Usage: <ip> <port>")
    with(DatagramSocket()) {
        connect(InetAddress.getByName(args[0]), args[1].toInt())
        send(A2S_GETCHALLENGE()) {
            val reply = S2C_CHALLENGE(recv(39))
            send(C2S_CONNECT(reply, "Hello world")) {
                recv(42)
            }
        }
    }
}

interface Sendable {
    fun send(): Packet
}

interface Readable<T> {
    fun read(it: Packet): T
}

val MAX_ROUTABLE_PAYLOAD = 1260

class Packet(array: ByteArray = ByteArray(MAX_ROUTABLE_PAYLOAD)) {

    private val buffer = ByteBuffer.wrap(array).let {
        it.order(ByteOrder.LITTLE_ENDIAN)
        it
    }

    fun toDatagram(): DatagramPacket {
        val len = buffer.position()
        buffer.rewind()
        buffer.limit(len)
        return DatagramPacket(ByteArray(len).let {
            buffer.get(it)
            it
        }, len)
    }

    override fun toString() = "${size()}: ${Arrays.toString(buffer.array())}"

    fun readBytes(n: Int) = ByteArray(n).let { buffer.get(it); it }

    fun readByte() = buffer.get()
    fun writeByte(value: Byte): Unit {
        buffer.put(value)
    }

    fun readShort() = buffer.getShort()

    fun readLong() = buffer.getInt()

    /** 4 bytes */
    fun writeLong(value: Int): Unit {
        buffer.putInt(value)
    }

    fun readString() = StringBuilder {
        while (true) {
            val c = buffer.get().toChar()
            if (c == 0.toChar()) break
            append(c)
        }
    }.toString()

    fun writeString(value: String) {
        value.forEach { buffer.put(it.toByte()) }
        buffer.put(0)
    }

    fun size() = buffer.limit()
    val underflow: Int get() = buffer.remaining()
}

fun <T> Readable<T>.invoke(it: Packet) = read(it)

inline fun DatagramSocket.send(it: Sendable, then: () -> Unit) {
    send(it.send().toDatagram())
    then()
}

fun DatagramSocket.recv(size: Int = MAX_ROUTABLE_PAYLOAD) = ByteArray(size + 1).let {
    val data = DatagramPacket(it, it.size())
    receive(data)
    val actualSize = data.getLength()
    check(actualSize == size, "Actual size ($actualSize) differs from expected size ($size)")
    val packet = Packet(it.copyOf(actualSize))
    println(packet)
    packet
}
