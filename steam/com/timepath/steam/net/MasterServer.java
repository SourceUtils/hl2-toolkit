package com.timepath.steam.net;

import com.timepath.Utils;
import com.timepath.backports.javax.swing.SwingWorker;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class MasterServer {

    public static final MasterServer SOURCE = new MasterServer("hl2master.steampowered.com"); // :27011

    private InetAddress address;

    public InetAddress getAddress() {
        return address;
    }

    public static enum Region {

        ALL((byte) 255),
        US_EAST((byte) 0),
        US_WEST((byte) 1),
        SOUTH_AMERICA((byte) 2),
        EUROPE((byte) 3),
        ASIA((byte) 4),
        AUSTRALIA((byte) 5),
        MIDDLE_EAST((byte) 6),
        AFRICA((byte) 7);

        private Region(byte code) {
            this.code = code;
        }

        private byte code;

        public byte getCode() {
            return code;
        }
    }

    public MasterServer(String hostname) {
        try {
            this.address = InetAddress.getByName(hostname);
        } catch(UnknownHostException ex) {
            Logger.getLogger(MasterServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void query(Region r) throws IOException {
        final DatagramSocket sock = new DatagramSocket();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(0x31);
        baos.write(r.getCode());
        String lastAddress = "0.0.0.0:0";
        baos.write((lastAddress + "\0").getBytes());
        String filter = "";//"\\gamedir\\tf";
        baos.write((filter + "\0").getBytes());
        
        byte[] buf = null;
        for(;;) {
            byte[] out = baos.toByteArray();
            LOG.log(Level.INFO, "Sending {0}\nAddress: {1}", new Object[]{Utils.hex(out), this.getAddress()});
            DatagramPacket packet = new DatagramPacket(out, out.length, this.getAddress(), 27011);
            sock.send(packet);
            try {
                buf = new SwingWorker<byte[], Void>() {
                    @Override
                    protected byte[] doInBackground() throws Exception {
                        byte[] buf = new byte[256];
                        DatagramPacket receivePacket = new DatagramPacket(buf, buf.length);
                        sock.receive(receivePacket);
                        return new String(buf, 0, receivePacket.getLength()).getBytes();
                    }
                }.get(5, TimeUnit.SECONDS);
                break;
            } catch(InterruptedException ex) {
                Logger.getLogger(MasterServer.class.getName()).log(Level.SEVERE, null, ex);
            } catch(ExecutionException ex) {
                Logger.getLogger(MasterServer.class.getName()).log(Level.SEVERE, null, ex);
            } catch(TimeoutException ex) {
                LOG.log(Level.WARNING, "Timed out");
            }
        }

        LOG.log(Level.INFO, "Received {0}", Utils.hex(buf));
    }

    public static void main(String... args) {
        try {
            MasterServer.SOURCE.query(Region.AUSTRALIA);
        } catch(IOException ex) {
            Logger.getLogger(MasterServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static final Logger LOG = Logger.getLogger(MasterServer.class.getName());

}
