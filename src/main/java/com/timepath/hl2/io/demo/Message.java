package com.timepath.hl2.io.demo;

import com.timepath.DataUtils;
import com.timepath.Pair;
import com.timepath.hl2.io.demo.Packet.Type;
import com.timepath.io.BitBuffer;
import com.timepath.io.OrderedOutputStream;
import com.timepath.io.struct.StructField;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class Message {

    private static final Logger LOG = Logger.getLogger(Message.class.getName());
    /**
     * Actually 3 bytes
     */
    @StructField(index = 1)
    public final int tick;
    public final MessageType type;
    private final HL2DEM outer;
    public ByteBuffer data;
    @NotNull
    public List<Pair<Object, Object>> meta = new LinkedList<>();
    public boolean incomplete;
    /**
     * Command / sequence info. TODO: use
     */
    @StructField(index = 2, nullable = true)
    public byte[] cseq;
    /**
     * Outgoing sequence number. TODO: use
     */
    @StructField(index = 3, nullable = true)
    public byte[] oseq;
    @StructField(index = 4)
    public int size;
    @StructField(index = 0)
    private byte op;
    private boolean parsed;

    public Message(HL2DEM outer, MessageType type, int tick) {
        this.outer = outer;
        this.type = type;
        this.tick = tick;
    }

    @NotNull
    static Message parse(HL2DEM outer, @NotNull ByteBuffer buffer) {
        int op = buffer.get();
        @Nullable MessageType type = MessageType.get(op);
        if (type == null) {
            LOG.log(Level.SEVERE, "Unknown demo message type encountered: {0}", op);
        }
        int tick = (0xFFFF & buffer.getShort()) + (0xFF & (buffer.get() << 16));
        if (type != MessageType.Stop) buffer.get();
        LOG.log(Level.FINE,
                "{0} at tick {1} ({2}), {3} remaining bytes",
                new Object[]{type, tick, buffer.position(), buffer.remaining()});
        @NotNull Message m = new Message(outer, type, tick);
        if (!(m.type == MessageType.Synctick || m.type == MessageType.Stop)) {
            if (m.type == MessageType.Packet || m.type == MessageType.Signon) {
                @NotNull byte[] dst = new byte[21 * 4];
                buffer.get(dst);
                m.cseq = dst;
            }
            if (m.type == MessageType.UserCmd) {
                @NotNull byte[] dst = new byte[4];
                buffer.get(dst);
                m.oseq = dst;
            }
            m.size = buffer.getInt();
        }
        return m;
    }

    public void write(@NotNull final OrderedOutputStream out) throws IOException {
        out.writeByte(type.ordinal() + 1);
        out.writeInt(tick); // TODO: technically MessageType.Stop is 1 byte less
        if (cseq != null) out.write(cseq);
        if (oseq != null) out.write(oseq);
        if (!(type == MessageType.Synctick || type == MessageType.Stop)) out.writeInt(size);
        if (data == null) return;
        data.position(0);
        @NotNull byte[] dst = new byte[data.limit()];
        data.get(dst);
        out.write(dst);
    }

    @NotNull
    @Override
    public String toString() {
        return MessageFormat.format("{0}, tick {1}, {2} bytes", type, tick, (data != null) ? data.limit() : 0);
    }

    void parse() {
        if (type == null) return;
        if (data == null) return;
        if (parsed) return;
        switch (type) {
            case Signon:
            case Packet: {
                @NotNull BitBuffer bb = new BitBuffer(data);
                @Nullable String error = null;
                @Nullable Throwable thrown = null;
                int opSize = (outer.header.networkProtocol >= 16) ? 6 : 5;
                while (bb.remainingBits() > opSize) {
                    try {
                        int op = (int) bb.getBits(opSize);
                        @Nullable Type type = Type.get(op);
                        if (type == null) {
                            error = MessageFormat.format("Unknown message type {0} in {1}", op, this);
                            thrown = new Exception("Unknown message");
                        } else {
                            @NotNull Packet p = new Packet(type, bb.positionBits());
                            try {
                                if (!type.handler.read(bb, p.list, outer)) {
                                    error = MessageFormat.format("Incomplete read of {0} in {1}", p, this);
                                }
                            } catch (BufferUnderflowException e) {
                                error = MessageFormat.format("Out of data in {0}", this);
                            } catch (Exception e) {
                                error = MessageFormat.format("Exception in {0} in {1}", p, this);
                                thrown = e;
                            }
                            meta.add(new Pair<Object, Object>(p, p.list));
                        }
                    } catch (BufferUnderflowException e) {
                        error = MessageFormat.format("Out of data in {0}", this);
                    }
                    meta.add(new Pair<Object, Object>("remaining bits", bb.remainingBits()));
                    if (error != null) {
                        incomplete = true;
                        meta.add(new Pair<Object, Object>("error", error));
                        if (thrown != null) LOG.log(Level.WARNING, error, thrown);
                        break;
                    }
                }
                break;
            }
            case ConsoleCmd: {
                @NotNull String cmd = DataUtils.getText(data, true);
                meta.add(new Pair<Object, Object>("cmd", cmd));
                break;
            }
            case UserCmd: {
                // https://github.com/LestaD/SourceEngine2007/blob/master/se2007/game/shared/usercmd.cpp#L199
                @NotNull BitBuffer bb = new BitBuffer(data);
                if (bb.getBoolean()) {
                    meta.add(new Pair<Object, Object>("Command number", bb.getInt()));
                } // else assume steady increment
                if (bb.getBoolean()) {
                    meta.add(new Pair<Object, Object>("Tick count", bb.getInt()));
                } // else assume steady increment
                if (bb.getBoolean()) {
                    meta.add(new Pair<Object, Object>("Viewangle pitch", bb.getFloat()));
                }
                if (bb.getBoolean()) {
                    meta.add(new Pair<Object, Object>("Viewangle yaw", bb.getFloat()));
                }
                if (bb.getBoolean()) {
                    meta.add(new Pair<Object, Object>("Viewangle roll", bb.getFloat()));
                }
                if (bb.getBoolean()) {
                    meta.add(new Pair<Object, Object>("Foward move", bb.getFloat()));
                }
                if (bb.getBoolean()) {
                    meta.add(new Pair<Object, Object>("Side move", bb.getFloat()));
                }
                if (bb.getBoolean()) {
                    meta.add(new Pair<Object, Object>("Up move", bb.getFloat()));
                }
                if (bb.getBoolean()) {
                    meta.add(new Pair<Object, Object>("Buttons", Input.get(bb.getInt())));
                }
                if (bb.getBoolean()) {
                    meta.add(new Pair<Object, Object>("Impulse", bb.getByte()));
                }
                if (bb.getBoolean()) {
                    meta.add(new Pair<Object, Object>("Weapon select", bb.getBits(HL2DEM.MAX_EDICT_BITS)));
                    if (bb.getBoolean()) {
                        meta.add(new Pair<Object, Object>("Weapon subtype", bb.getBits(HL2DEM.WEAPON_SUBTYPE_BITS)));
                    }
                }
                if (bb.getBoolean()) {
                    meta.add(new Pair<Object, Object>("Mouse Dx", bb.getShort()));
                }
                if (bb.getBoolean()) {
                    meta.add(new Pair<Object, Object>("Mouse Dy", bb.getShort()));
                }
                if (bb.remaining() > 0) {
                    meta.add(new Pair<Object, Object>("Underflow", bb.remaining()));
                }
                break;
            }
            // TODO
            case DataTables:
            case StringTables:
                break;
        }
        parsed = true;
    }

    public void setData(@NotNull final byte[] data) {
        this.data = ByteBuffer.wrap(data);
        this.size = data.length;
    }
}
