package com.timepath.steam.net;

import com.timepath.DataUtils;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class Server {

    protected InetSocketAddress sock;

    protected DatagramChannel chan;

    protected InetAddress address;

    public InetAddress getAddress() {
        return address;
    }

    protected int port;

    public int getPort() {
        return port;
    }

    public Server() {
    }

    public Server(String hostname) {
        this(hostname, 27011); // TODO: split
    }

    public Server(String hostname, int port) {
        try {
            this.address = InetAddress.getByName(hostname);
            this.port = port;
            initSocket();
        } catch(UnknownHostException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    protected void initSocket() throws IOException {
        sock = new InetSocketAddress(this.address, port);
        chan = DatagramChannel.open();
        chan.connect(sock);
    }
    
    private static Level sendLevel = Level.INFO;
    
    public void send(ByteBuffer buf) throws IOException {
        LOG.log(sendLevel, "Sending {0} bytes\nPayload: {1}\nAddress: {2}", new Object[]{buf.limit(), DataUtils.hexDump(buf), this.getAddress()});
        chan.write(buf);
    }
    
    private static Level getLevel = Level.INFO;
    
    public ByteBuffer get() throws IOException {
        ByteBuffer buf = ByteBuffer.allocate(1392);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        int bytesRead = chan.read(buf);
        if(bytesRead > 0) {
            buf.rewind();
            buf.limit(bytesRead);
            buf = buf.slice();
        }
        LOG.log(getLevel, "Receiving {0} bytes\nPayload: {1}\nAddress: {2}", new Object[]{buf.limit(), DataUtils.hexDump(buf), this.getAddress()});
        return buf;
    }

    private static final Logger LOG = Logger.getLogger(Server.class.getName());

}
