package com.timepath.hl2.io.demo;

import com.timepath.DataUtils;
import com.timepath.Pair;
import com.timepath.io.BitBuffer;
import com.timepath.io.OrderedOutputStream;
import com.timepath.io.struct.StructField;

import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author TimePath
 */
public class Message {

    private static final Logger LOG = Logger.getLogger(Message.class.getName());
    @StructField(index = 0)
    private       byte        op;
    /**
     * Actually 3 bytes
     */
    @StructField(index = 1)
    public final  int         tick;
    public final  MessageType type;
    private final HL2DEM      outer;
    public        ByteBuffer  data;
    public Collection<Pair<Object, Object>> meta = new LinkedList<>();
    public  boolean incomplete;
    private boolean parsed;
    /**
     * Command / sequence info. TODO: use
     */
    @StructField(index = 2, nullable = true)
    public  byte[]  cseq;
    /**
     * Outgoing sequence number. TODO: use
     */
    @StructField(index = 3, nullable = true)
    public  byte[]  oseq;
    @StructField(index = 4)
    public  int     size;

    static Message parse(HL2DEM outer, ByteBuffer buffer) {
        int op = buffer.get();
        MessageType type = MessageType.get(op);
        if(type == null) {
            LOG.log(Level.SEVERE, "Unknown demo message type encountered: {0}", op);
        }
        int tick = ( 0xFFFF & buffer.getShort() ) + ( 0xFF & ( buffer.get() << 16 ) );
        if(type != MessageType.Stop) buffer.get();
        LOG.log(Level.FINE,
                "{0} at tick {1} ({2}), {3} remaining bytes",
                new Object[] { type, tick, buffer.position(), buffer.remaining() });
        Message m = new Message(outer, type, tick);
        if(!( m.type == MessageType.Synctick || m.type == MessageType.Stop )) {
            if(m.type == MessageType.Packet || m.type == MessageType.Signon) {
                byte[] dst = new byte[21 * 4];
                buffer.get(dst);
                m.cseq = dst;
            }
            if(m.type == MessageType.UserCmd) {
                byte[] dst = new byte[4];
                buffer.get(dst);
                m.oseq = dst;
            }
            m.size = buffer.getInt();
        }
        return m;
    }

    public Message(HL2DEM outer, MessageType type, int tick) {
        this.outer = outer;
        this.type = type;
        this.tick = tick;
    }

    public void write(final OrderedOutputStream out) throws IOException {
        out.writeByte(type.ordinal() + 1);
        out.writeInt(tick); // TODO: technically MessageType.Stop is 1 byte less
        if(cseq != null) out.write(cseq);
        if(oseq != null) out.write(oseq);
        if(!( type == MessageType.Synctick || type == MessageType.Stop )) out.writeInt(size);
        if(data == null) return;
        data.position(0);
        byte[] dst = new byte[data.limit()];
        data.get(dst);
        out.write(dst);
    }

    @Override
    public String toString() {
        return MessageFormat.format("{0}, tick {1}, {2} bytes", type, tick, ( data != null ) ? data.limit() : 0);
    }

    void parse() {
        if(type == null) return;
        if(data == null) return;
        if(parsed) return;
        switch(type) {
            case Signon:
            case Packet: {
                BitBuffer bb = new BitBuffer(data);
                List<Pair<Object, Object>> values = new LinkedList<>();
                String error = null;
                while(bb.remaining() >= 1) {
                    try {
                        int mid = (int) bb.getBits(( outer.header.networkProtocol >= 16 ) ? 6 : 5);
                        Packet p = Packet.get(mid);
                        if(p != null) {
                            List<Pair<Object, Object>> list = new LinkedList<>();
                            try {
                                if(!p.handler.read(bb, list, outer)) {
                                    error = MessageFormat.format("Incomplete read of {0} at {1}", p, tick);
                                }
                            } catch(BufferUnderflowException ignored) {
                            } catch(Exception e) {
                                error = MessageFormat.format("Exception {0} in {1} at {2}", e, p, tick);
                                LOG.log(Level.WARNING, null, e);
                            }
                            values.add(new Pair<Object, Object>(p, list));
                        } else {
                            error = MessageFormat.format("Unknown message type {0} at {1}", mid, tick);
                        }
                    } catch(BufferUnderflowException e) {
                        error = e.toString();
                    }
                    if(error != null) {
                        incomplete = true;
                        values.add(new Pair<Object, Object>("error", error));
                        LOG.log(Level.WARNING, error);
                        break;
                    }
                }
                meta.add(new Pair<Object, Object>(this, values));
            }
            break;
            case ConsoleCmd: {
                String cmd = DataUtils.getText(data, true);
                meta.add(new Pair<Object, Object>(this, cmd));
                if(data.remaining() > 0) {
                    LOG.log(Level.FINE, "Underflow: {0}, {1}", new Object[] { data.remaining(), data.position() });
                }
            }
            break;
            case UserCmd: {
                List<Pair<Object, Object>> values = new LinkedList<>();
                // https://github.com/LestaD/SourceEngine2007/blob/master/se2007/game/shared/usercmd.cpp#L199
                BitBuffer bb = new BitBuffer(data);
                if(bb.getBoolean()) {
                    values.add(new Pair<Object, Object>("Command number", bb.getInt()));
                } // else assume steady increment
                if(bb.getBoolean()) {
                    values.add(new Pair<Object, Object>("Tick count", bb.getInt()));
                } // else assume steady increment
                if(bb.getBoolean()) {
                    values.add(new Pair<Object, Object>("Viewangle pitch", bb.getFloat()));
                }
                if(bb.getBoolean()) {
                    values.add(new Pair<Object, Object>("Viewangle yaw", bb.getFloat()));
                }
                if(bb.getBoolean()) {
                    values.add(new Pair<Object, Object>("Viewangle roll", bb.getFloat()));
                }
                if(bb.getBoolean()) {
                    values.add(new Pair<Object, Object>("Foward move", bb.getFloat()));
                }
                if(bb.getBoolean()) {
                    values.add(new Pair<Object, Object>("Side move", bb.getFloat()));
                }
                if(bb.getBoolean()) {
                    values.add(new Pair<Object, Object>("Up move", bb.getFloat()));
                }
                if(bb.getBoolean()) {
                    values.add(new Pair<Object, Object>("Buttons", Input.get(bb.getInt())));
                }
                if(bb.getBoolean()) {
                    values.add(new Pair<Object, Object>("Impulse", bb.getByte()));
                }
                if(bb.getBoolean()) {
                    values.add(new Pair<Object, Object>("Weapon select", bb.getBits(HL2DEM.MAX_EDICT_BITS)));
                    if(bb.getBoolean()) {
                        values.add(new Pair<Object, Object>("Weapon subtype", bb.getBits(HL2DEM.WEAPON_SUBTYPE_BITS)));
                    }
                }
                if(bb.getBoolean()) {
                    values.add(new Pair<Object, Object>("Mouse Dx", bb.getShort()));
                }
                if(bb.getBoolean()) {
                    values.add(new Pair<Object, Object>("Mouse Dy", bb.getShort()));
                }
                if(bb.remaining() > 0) {
                    values.add(new Pair<Object, Object>("Underflow", bb.remaining()));
                }
                meta.add(new Pair<Object, Object>(this, values));
            }
            break;
            // TODO
            case DataTables:
            case StringTables:
                break;
        }
        parsed = true;
    }

    public void setData(final byte[] data) {
        this.data = ByteBuffer.wrap(data);
        this.size = data.length;
    }
}
