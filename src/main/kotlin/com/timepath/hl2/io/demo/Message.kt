package com.timepath.hl2.io.demo

import com.timepath.DataUtils
import com.timepath.io.BitBuffer
import com.timepath.io.OrderedOutputStream
import com.timepath.io.struct.StructField

import java.io.IOException
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.text.MessageFormat
import java.util.LinkedList
import java.util.logging.Level
import java.util.logging.Logger

/**
 * @author TimePath
 */
public class Message(private val outer: HL2DEM, public val type: MessageType?,
                     /**
                      * Actually 3 bytes
                      */
                     StructField(index = 1)
                     public val tick: Int) {
    public var data: ByteBuffer? = null
    public var meta: MutableList<Pair<Any, Any?>> = LinkedList()
    public var incomplete: Boolean = false
    /**
     * Command / sequence info. TODO: use
     */
    StructField(index = 2, nullable = true)
    public var cseq: ByteArray? = null
    /**
     * Outgoing sequence number. TODO: use
     */
    StructField(index = 3, nullable = true)
    public var oseq: ByteArray? = null
    StructField(index = 4)
    public var size: Int = 0
    StructField(index = 0)
    private val op: Byte = 0
    private var parsed: Boolean = false

    throws(javaClass<IOException>())
    public fun write(out: OrderedOutputStream) {
        out.writeByte(type!!.ordinal() + 1)
        out.writeInt(tick) // TODO: technically MessageType.Stop is 1 byte less
        if (cseq != null) out.write(cseq)
        if (oseq != null) out.write(oseq)
        if (!(type == MessageType.Synctick || type == MessageType.Stop)) out.writeInt(size)
        if (data == null) return
        data!!.position(0)
        val dst = ByteArray(data!!.limit())
        data!!.get(dst)
        out.write(dst)
    }

    override fun toString(): String {
        return MessageFormat.format("{0}, tick {1}, {2} bytes", type, tick, if ((data != null)) data!!.limit() else 0)
    }

    fun parse() {
        if (type == null) return
        if (data == null) return
        if (parsed) return
        when (type) {
            MessageType.Signon, MessageType.Packet -> {
                val bb = BitBuffer(data)
                var error: String? = null
                var thrown: Throwable? = null
                val opSize = if ((outer.header.networkProtocol >= 16)) 6 else 5
                while (bb.remainingBits() > opSize) {
                    try {
                        val op = bb.getBits(opSize).toInt()
                        val type = Packet.Type[op]
                        if (type == null) {
                            error = MessageFormat.format("Unknown message type {0} in {1}", op, this)
                            thrown = Exception("Unknown message")
                        } else {
                            val p = Packet(type, bb.positionBits())
                            try {
                                if (!type.handler.read(bb, p.list, outer)) {
                                    error = MessageFormat.format("Incomplete read of {0} in {1}", p, this)
                                }
                            } catch (e: BufferUnderflowException) {
                                error = MessageFormat.format("Out of data in {0}", this)
                            } catch (e: Exception) {
                                error = MessageFormat.format("Exception in {0} in {1}", p, this)
                                thrown = e
                            }

                            meta.add(p to p.list)
                        }
                    } catch (e: BufferUnderflowException) {
                        error = MessageFormat.format("Out of data in {0}", this)
                    }

                    meta.add("remaining bits" to bb.remainingBits())
                    if (error != null) {
                        incomplete = true
                        meta.add("error" to error)
                        if (thrown != null) LOG.log(Level.WARNING, error, thrown)
                        break
                    }
                }
            }
            MessageType.ConsoleCmd -> {
                val cmd = DataUtils.getText(data, true)
                meta.add("cmd" to cmd)
            }
            MessageType.UserCmd -> {
                // https://github.com/LestaD/SourceEngine2007/blob/master/se2007/game/shared/usercmd.cpp#L199
                val bb = BitBuffer(data)
                if (bb.getBoolean()) {
                    meta.add("Command number" to bb.getInt())
                } // else assume steady increment
                if (bb.getBoolean()) {
                    meta.add("Tick count" to bb.getInt())
                } // else assume steady increment
                if (bb.getBoolean()) {
                    meta.add("Viewangle pitch" to bb.getFloat())
                }
                if (bb.getBoolean()) {
                    meta.add("Viewangle yaw" to bb.getFloat())
                }
                if (bb.getBoolean()) {
                    meta.add("Viewangle roll" to bb.getFloat())
                }
                if (bb.getBoolean()) {
                    meta.add("Foward move" to bb.getFloat())
                }
                if (bb.getBoolean()) {
                    meta.add("Side move" to bb.getFloat())
                }
                if (bb.getBoolean()) {
                    meta.add("Up move" to bb.getFloat())
                }
                if (bb.getBoolean()) {
                    meta.add("Buttons" to Input[bb.getInt()])
                }
                if (bb.getBoolean()) {
                    meta.add("Impulse" to bb.getByte())
                }
                if (bb.getBoolean()) {
                    meta.add("Weapon select" to bb.getBits(HL2DEM.MAX_EDICT_BITS))
                    if (bb.getBoolean()) {
                        meta.add("Weapon subtype" to bb.getBits(HL2DEM.WEAPON_SUBTYPE_BITS))
                    }
                }
                if (bb.getBoolean()) {
                    meta.add("Mouse Dx" to bb.getShort())
                }
                if (bb.getBoolean()) {
                    meta.add("Mouse Dy" to bb.getShort())
                }
                if (bb.remaining() > 0) {
                    meta.add("Underflow" to bb.remaining())
                }
            }
        // TODO
            MessageType.DataTables, MessageType.StringTables -> {
            }
        }
        parsed = true
    }

    public fun setData(data: ByteArray) {
        this.data = ByteBuffer.wrap(data)
        this.size = data.size
    }

    class object {

        private val LOG = Logger.getLogger(javaClass<Message>().getName())

        fun parse(outer: HL2DEM, buffer: ByteBuffer): Message {
            val op = buffer.get().toInt()
            val type = MessageType[op]
            if (type == null) {
                LOG.log(Level.SEVERE, "Unknown demo message type encountered: {0}", op)
            }
            val tick = (65535 and buffer.getShort().toInt()) + (255 and (buffer.get().toInt() shl 16))
            if (type != MessageType.Stop) buffer.get()
            LOG.log(Level.FINE, "{0} at tick {1} ({2}), {3} remaining bytes", array(type, tick, buffer.position(), buffer.remaining()))
            val m = Message(outer, type, tick)
            if (!(m.type == MessageType.Synctick || m.type == MessageType.Stop)) {
                if (m.type == MessageType.Packet || m.type == MessageType.Signon) {
                    val dst = ByteArray(21 * 4)
                    buffer.get(dst)
                    m.cseq = dst
                }
                if (m.type == MessageType.UserCmd) {
                    val dst = ByteArray(4)
                    buffer.get(dst)
                    m.oseq = dst
                }
                m.size = buffer.getInt()
            }
            return m
        }
    }
}
