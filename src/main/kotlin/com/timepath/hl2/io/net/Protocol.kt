package com.timepath.hl2.io.net

import com.timepath.steam.SteamUtils
import java.lang.management.ManagementFactory
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.URL
import java.nio.Buffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Arrays
import java.util.Random
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

// net_usesocketsforloopback 1

// tshark -r capture.pcapng -T fields -e data > raw

/*
#!/bin/env python

import binascii
import sys

if not len(sys.argv) == 3:
    print("Usage: {} <input> <output>".format(sys.argv[0]))
else:
    i = 0
    outname = sys.argv[2]
    for line in open(sys.argv[1], 'r'):
        i += 1
        with open(outname + '.' + str(i), 'wb') as o:
            o.write(binascii.unhexlify(line[:-1]))
*/

val CONNECTIONLESS_HEADER = 0xffffffff.toInt()

val PROTOCOL_VERSION = 24

val GAME_VERSION = "2827522"
val GAME_ID = 440

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
        val challenge: Long,
        val protocol: Protocol,
        val gameConTicket: ByteArray = byteArrayOf()
) {
    companion object : Readable<S2C_CHALLENGE> {
        val ID = 'A'.toByte()

        override fun read(it: Packet): S2C_CHALLENGE {
            check(it.readLong() == CONNECTIONLESS_HEADER)
            check(it.readByte() == ID)
            val k = it.readLong()
            check(k == 0x5A4F4933)
            val challenge = it.readLongLong() // high bytes relatively unstable (5s), low bytes unstable (1s)
            val authprotocol = it.readLong()
            if (authprotocol == 3) {
                val keySize = it.readShort()
                check(keySize.toInt() == 0)
                val key = it.readBytes(keySize.toInt())
                val sid = it.readLongLong()
                val secure = it.readByte()
            }
            check(it.readString() == "000000")
            return S2C_CHALLENGE(challenge = challenge, protocol = Protocol[authprotocol])
        }
    }
}

object PrivateData {
    var ticket by Delegates.notNull<IntArray>()
    var hash: IntArray? = null
}

/**
 * https://partner.steamgames.com/documentation/auth
 */
data class C2S_CONNECT(
        val challenge: S2C_CHALLENGE,
        val name: String,
        val password: String = "",
        val steamid: Int = SteamUtils.getUser()!!.ID32.substringAfterLast(":").toInt(),
        val publicIpv4: IntArray = URL("http://checkip.amazonaws.com").openStream().use {
            it.bufferedReader().readLine().splitBy(".").reverse().map { it.toInt() }.toIntArray()
        },
        val localipv4: IntArray = InetAddress.getLocalHost().getAddress().reverse().map { it.toInt() }.toIntArray(),
        val minutes: Int = TimeUnit.MILLISECONDS.toMinutes(ManagementFactory.getRuntimeMXBean().getUptime()).toInt()
) : Sendable {
    companion object {
        val ID = 'k'.toByte()
        val STEAMID64_OFFSET = 17825793
        val clientNumber = sequence(1) { it + 1 }.iterator()
        /** 21 day lease, first two fields are from and to */
        val ticket = PrivateData.ticket
    }

    override fun send() = with(Packet()) {
        writeLong(CONNECTIONLESS_HEADER)
        writeByte(ID)
        writeLong(PROTOCOL_VERSION)
        writeLong(challenge.protocol.i)
        writeLongLong(challenge.challenge)
        writeString(name)
        writeString(password)
        writeString(GAME_VERSION)
        run {
            val keySize = ticket.size() + 96
            writeShort(keySize.toShort())
            // The key follows
            writeLong(steamid)
            writeLong(STEAMID64_OFFSET)
            run {
                // Obtained from `SteamUser->InitiateGameConnection`, last 4 bytes discarded
                val sectionSize = 20 // InitiateGameConnection returns about this many bytes
                if (challenge.gameConTicket.size() >= sectionSize) {
                    writeBytes(*challenge.gameConTicket.slice(0..4 + sectionSize - 1).toByteArray())
                } else {
                    writeLong(sectionSize)
                    writeBytes(*PrivateData.hash ?: IntArray(8))
                    writeLong(steamid)
                    writeLong(STEAMID64_OFFSET)
                    writeLong((System.currentTimeMillis() / 1000).toInt())
                }
            }
            writeLong(PROTOCOL_VERSION)
            writeLong(1)
            writeLong(2)
            writeBytes(*publicIpv4)
            writeLong(0)
            writeLong(minutes * 100000)
            writeLong(clientNumber.next())
            writeLong(ticket.size() + 32)
            writeLong(ticket.size() - 96)
            writeLong(4)
            writeLong(steamid)
            writeLong(STEAMID64_OFFSET)
            writeLong(GAME_ID)
            writeBytes(*publicIpv4)
            writeBytes(*localipv4)
            writeLong(0)
            writeBytes(*ticket)
        }
        this
    }
}

data class S2C_CONNREJECT {
    companion object : Readable<S2C_CONNREJECT> {
        val ID = '9'.toByte()

        override fun read(it: Packet): S2C_CONNREJECT {
            check(it.readLong() == CONNECTIONLESS_HEADER)
            check(it.readByte() == ID)
            fun r() = it.readByte().toInt() and 0xFF
            val i = intArrayOf(r(), r(), r(), r())
            throw Exception("${Arrays.toString(i)}: ${it.readString()}")
        }
    }
}

data class S2C_CONNECTION {
    companion object : Readable<S2C_CONNECTION> {
        val ID = 'B'.toByte()

        override fun read(it: Packet): S2C_CONNECTION {
            check(it.readLong() == CONNECTIONLESS_HEADER)
            check(it.readByte() == ID)
            fun r() = it.readByte().toInt() and 0xFF
            val i = intArrayOf(r(), r(), r(), r())
            check(it.readString() == "0000000000")
            return S2C_CONNECTION()
        }
    }
}

fun main(args: Array<String>) {
    require(args.size() == 2, "Usage: <ip> <port>")
    with(DatagramSocket()) {
        connect(InetAddress.getByName(args[0]), args[1].toInt())
        send(A2S_GETCHALLENGE()) {
            S2C_CHALLENGE(recv(39)).let {
                send(C2S_CONNECT(it, "Hello world")) {
                    recv(42, strict = false).let {
                        try {
                            S2C_CONNREJECT(it)
                        } catch(e: IllegalStateException) {
                            it.rewind()
                            S2C_CONNECTION(it)
                            println("we're okay")
                        }
                    }
                }
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

    fun rewind() {
        buffer.rewind()
    }

    fun readBytes(n: Int) = ByteArray(n).let { buffer.get(it); it }

    fun readByte() = buffer.get()
    fun writeByte(value: Byte) {
        buffer.put(value)
    }

    fun writeBytes(vararg values: Byte) {
        buffer.put(values)
    }

    fun writeBytes(vararg values: Int) {
        values.forEach {
            buffer.put(it.toByte())
        }
    }

    fun readShort() = buffer.getShort()

    fun writeShort(value: Short) {
        buffer.putShort(value)
    }

    fun readLong() = buffer.getInt()
    /** 4 bytes */
    fun writeLong(value: Int) {
        buffer.putInt(value)
    }

    fun readLongLong() = buffer.getLong()

    fun writeLongLong(value: Long) {
        buffer.putLong(value)
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
}

fun <T> Readable<T>.invoke(it: Packet) = read(it)

inline fun DatagramSocket.send(it: Sendable, then: () -> Unit) {
    send(it.send().toDatagram())
    then()
}

fun DatagramSocket.recv(size: Int = MAX_ROUTABLE_PAYLOAD, strict: Boolean = true) = ByteArray(size + 1).let {
    val data = DatagramPacket(it, it.size())
    receive(data)
    val actualSize = data.getLength()
    check(!strict || actualSize == size, "Actual size ($actualSize) differs from expected size ($size)")
    val packet = Packet(it.copyOf(actualSize))
    println(packet)
    packet
}
