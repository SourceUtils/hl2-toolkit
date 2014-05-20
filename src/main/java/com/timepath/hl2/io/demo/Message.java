package com.timepath.hl2.io.demo;

import com.timepath.DataUtils;
import com.timepath.Pair;
import com.timepath.io.BitBuffer;

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
    /**
     * Actually 3 bytes
     */
    public final  int         tick;
    public final  MessageType type;
    private final HL2DEM      outer;
    public        ByteBuffer  data;
    public Collection<Pair<Object, Object>> meta = new LinkedList<>();
    public boolean incomplete;

    Message(ByteBuffer buffer, HL2DEM outer) {
        this.outer = outer;
        int b = buffer.get();
        type = MessageType.get(b);
        if(type == null) {
            LOG.log(Level.SEVERE, "Unknown demo message type encountered: {0}", b);
        }
        tick = buffer.getShort() + ( buffer.get() << 16 );
        if(type != MessageType.Stop) {
            buffer.get();
        }
        LOG.log(Level.FINE,
                "{0} at tick {1} ({2}), {3} remaining bytes",
                new Object[] { type, tick, buffer.position(), buffer.remaining() });
    }

    @Override
    public String toString() {
        return MessageFormat.format("{0}, tick {1}, {2} bytes", type, tick, ( data != null ) ? data.limit() : 0);
    }

    void parse() {
        if(data == null) {
            return;
        }
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
                ByteBuffer b = data;
                Level l = Level.FINE;
                String cmd = DataUtils.getText(b).trim();
                meta.add(new Pair<Object, Object>(this, cmd));
                if(b.remaining() > 0) {
                    LOG.log(l, "Underflow: {0}, {1}", new Object[] { b.remaining(), b.position() });
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
    }
}
