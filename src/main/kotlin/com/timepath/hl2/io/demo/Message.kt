package com.timepath.hl2.io.demo

import com.timepath.DataUtils
import com.timepath.Logger
import com.timepath.io.BitBuffer
import com.timepath.io.OrderedOutputStream
import com.timepath.io.struct.StructField
import com.timepath.with
import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.text.MessageFormat
import java.util.LinkedList
import java.util.logging.Level

public class Message(
        private val outer: HL2DEM,
        public val type: MessageType?,
        StructField(index = 1) public val tick: Int) {

    public var data: ByteBuffer? = null
        set(data) {
            $data = data
            $size = data?.capacity() ?: 0
        }

    public var meta: TupleMap<Any, Any?> = TupleMap()
    public var incomplete: Boolean = false
    /** Command / sequence info. TODO: use */
    StructField(index = 2, nullable = true) public var cseq: ByteArray? = null
    /** Outgoing sequence number. TODO: use */
    StructField(index = 3, nullable = true) public var oseq: ByteArray? = null
    StructField(index = 4) public var size: Int = 0
    StructField(index = 0) private val op: Byte = 0
    private var parsed: Boolean = false

    public fun write(out: OrderedOutputStream) {
        out.writeByte(type!!.ordinal() + 1)
        out.writeInt(tick)
        cseq?.let { out.write(it) }
        oseq?.let { out.write(it) }
        when (type) {
            MessageType.Synctick, MessageType.Stop -> Unit
            else -> out.writeInt(size)
        }
        data?.let {
            it.position(0)
            val dst = ByteArray(it.limit())
            it.get(dst)
            out.write(dst)
        }
    }

    override fun toString() = "${type}, tick ${tick}, ${data?.limit() ?: 0} bytes"

    fun parse() {
        if (type == null) return
        if (data == null) return
        if (parsed) return
        when (type) {
            MessageType.Signon, MessageType.Packet -> {
                val bb = BitBuffer(data!!)
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

                            meta[p] = p.list
                        }
                    } catch (e: BufferUnderflowException) {
                        error = MessageFormat.format("Out of data in {0}", this)
                    }

                    meta["remaining bits"] = bb.remainingBits()
                    if (error != null) {
                        incomplete = true
                        meta["error"] = error
                        thrown?.let { LOG.log(Level.WARNING, { error }, it) }
                        break
                    }
                }
            }
            MessageType.ConsoleCmd -> {
                val cmd = DataUtils.getText(data!!, true)
                meta["cmd"] = cmd
            }
            MessageType.UserCmd -> {
                // https://github.com/LestaD/SourceEngine2007/blob/master/se2007/game/shared/usercmd.cpp#L199
                val bb = BitBuffer(data!!)
                if (bb.getBoolean()) {
                    meta["Command number"] = bb.getInt()
                } // else assume steady increment
                if (bb.getBoolean()) {
                    meta["Tick count"] = bb.getInt()
                } // else assume steady increment
                if (bb.getBoolean()) {
                    meta["Viewangle pitch"] = bb.getFloat()
                }
                if (bb.getBoolean()) {
                    meta["Viewangle yaw"] = bb.getFloat()
                }
                if (bb.getBoolean()) {
                    meta["Viewangle roll"] = bb.getFloat()
                }
                if (bb.getBoolean()) {
                    meta["Foward move"] = bb.getFloat()
                }
                if (bb.getBoolean()) {
                    meta["Side move"] = bb.getFloat()
                }
                if (bb.getBoolean()) {
                    meta["Up move"] = bb.getFloat()
                }
                if (bb.getBoolean()) {
                    meta["Buttons"] = Input[bb.getInt()]
                }
                if (bb.getBoolean()) {
                    meta["Impulse"] = bb.getByte()
                }
                if (bb.getBoolean()) {
                    meta["Weapon select"] = bb.getBits(HL2DEM.MAX_EDICT_BITS)
                    if (bb.getBoolean()) {
                        meta["Weapon subtype"] = bb.getBits(HL2DEM.WEAPON_SUBTYPE_BITS)
                    }
                }
                if (bb.getBoolean()) {
                    meta["Mouse Dx"] = bb.getShort()
                }
                if (bb.getBoolean()) {
                    meta["Mouse Dy"] = bb.getShort()
                }
                if (bb.remaining() > 0) {
                    meta["Underflow"] = bb.remaining()
                }
            }
            MessageType.DataTables, MessageType.StringTables -> Unit // TODO
        }
        parsed = true
    }

    companion object {

        private val LOG = Logger()

        fun parse(outer: HL2DEM, buffer: ByteBuffer): Message {
            val op = buffer.get().toInt()
            val type = MessageType[op]
            if (type == null) {
                LOG.severe { "Unknown demo message type encountered: ${op}" }
            }
            val tick = buffer.getInt()
            LOG.fine { "${type} at tick ${tick} (${buffer.position()}), ${buffer.remaining()} remaining bytes" }
            return Message(outer, type, tick).with {
                val m = this
                when {
                    m.type == MessageType.Synctick,
                    m.type == MessageType.Stop -> Unit
                    else -> {
                        when {
                            m.type == MessageType.Packet,
                            m.type == MessageType.Signon -> {
                                val dst = ByteArray(21 * 4)
                                buffer.get(dst)
                                m.cseq = dst
                            }
                            m.type == MessageType.UserCmd -> {
                                val dst = ByteArray(4)
                                buffer.get(dst)
                                m.oseq = dst
                            }
                        }
                        m.size = buffer.getInt()
                    }
                }
            }
        }
    }
}
