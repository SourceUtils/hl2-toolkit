package com.timepath.steam.net;

import com.timepath.DataUtils;
import com.timepath.Utils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * https://developer.valvesoftware.com/wiki/Master_Server_Query_Protocol
 *
 * @author timepath
 */
public class MasterServer extends Server {

    public static final MasterServer SOURCE = new MasterServer("hl2master.steampowered.com", 27011);

    public MasterServer(String hostname) {
        super(hostname);
    }

    public MasterServer(String hostname, int port) {
        super(hostname, port);
    }
    
    public void query(Region r, ServerListener l) throws IOException {
        query(r, "", l);
    }

    public void query(Region r, String filter, ServerListener l) throws IOException {
        if(l == null) {
            l = ServerListener.DUMMY;
        }

        String initialAddress = "0.0.0.0:0";
        String lastAddress = initialAddress;
        boolean looping = true;
        while(looping) {
            LOG.log(Level.FINE, "Last address: {0}", lastAddress);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            baos.write(0x31);
            baos.write(r.getCode());
            baos.write((lastAddress + "\0").getBytes());
            baos.write((filter + "\0").getBytes());
            ByteBuffer send = ByteBuffer.wrap(baos.toByteArray());
            send(send);

            ByteBuffer buf = get();

            int header = buf.getInt();
            if(header != -1) {
                LOG.log(Level.WARNING, "Invalid header {0}", header);
                break;
            }

            byte head = buf.get();
            if(head != 0x66) {
                LOG.log(Level.WARNING, "Unknown header {0}", head);
                String rec = DataUtils.hexDump(buf);
                LOG.log(Level.INFO, "Received {0}", rec);
                l.inform(rec);
                break;
            }
            
            byte ten = buf.get();
            if(ten != 0x0A) {
                LOG.log(Level.WARNING, "Malformed byte {0}", ten);
                break;
            }
            
            int[] octet = new int[4];
            int serverPort;
            do {
                octet[0] = buf.get() & 0xFF;
                octet[1] = buf.get() & 0xFF;
                octet[2] = buf.get() & 0xFF;
                octet[3] = buf.get() & 0xFF;
                serverPort = buf.getShort() & 0xFFFF;
                lastAddress = (octet[0] + "." + octet[1] + "." + octet[2] + "." + octet[3] + ":" + serverPort);
                if(looping = !initialAddress.equals(lastAddress)) {
                    l.inform(lastAddress);
                }
            } while(buf.remaining() >= 6);
            
            if(buf.remaining() > 0) {
                byte[] under = new byte[buf.remaining()];
                if(under.length > 0) {
                    LOG.log(Level.INFO, "{0} byte underflow: {0}", new Object[]{buf.remaining(), Utils.hex(under)});
                }
            }
        }
    }

    private static final Logger LOG = Logger.getLogger(MasterServer.class.getName());

}
