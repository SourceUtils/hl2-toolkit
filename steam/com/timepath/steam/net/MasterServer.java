package com.timepath.steam.net;

import com.timepath.DateUtils;
import com.timepath.Utils;
import com.timepath.backports.javax.swing.SwingWorker;
import com.timepath.steam.SteamUtils;
import com.timepath.steam.SteamUtils.SteamID;
import com.timepath.steam.io.VDF;
import com.timepath.steam.io.util.VDFNode;
import java.awt.Dimension;
import java.io.ByteArrayOutputStream;
import java.io.File;
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
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;

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

    public void query(Region r, ServerListener l) throws IOException {
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
        String rec = Utils.hex(buf);
        if(l != null) {
            l.inform(rec);
        }
        LOG.log(Level.INFO, "Received {0}", rec);
    }
    
    public static abstract class ServerListener {
        abstract void inform(String update);
    }

    public static void main(String... args) {
        try {
            JFrame f = new JFrame("Servers");
            f.setPreferredSize(new Dimension(400, 600));
            JTabbedPane tabs = new JTabbedPane();
            final JTextArea net = new JTextArea();
            tabs.add("Internet", new JScrollPane(net));
            JTextArea favs = new JTextArea();
            tabs.add("Favorites", new JScrollPane(favs));
            JTextArea hist = new JTextArea();
            tabs.add("History", new JScrollPane(hist));
            f.setContentPane(tabs);
            f.pack();
            f.setVisible(true);
            SteamID user = SteamUtils.getUser();
            VDFNode v = VDF.load(new File(SteamUtils.getSteam(), "userdata/" + user.uid.split(":")[2] + "/7/remote/serverbrowser_hist.vdf")).get("Filters");
            VDFNode favorites = v.get("Favorites");
            VDFNode history = v.get("History");
            long lastPlayed = 0;
            for(int i = 0; i < favorites.getChildCount(); i++) {
                VDFNode favorite = favorites.get(i);
                favs.append("Favorite " + favorite.getKey() + "\n");
                favs.append("Name: " + favorite.get("name").getValue() + "\n");
                favs.append("Address: " + favorite.get("address").getValue() + "\n");
                long newLastPlayed = Long.parseLong(favorite.get("lastplayed").getValue());
                if(newLastPlayed < lastPlayed) {
                    favs.append("Out of order" + "\n");
                }
                lastPlayed = newLastPlayed;
                favs.append("Last Played: " + DateUtils.parse(lastPlayed) + "\n" + "\n");
            }
            lastPlayed = 0;
            for(int i = 0; i < history.getChildCount(); i++) {
                VDFNode historyItem = history.get(i);
                hist.append("History " + historyItem.getKey() + "\n");
                hist.append("Name: " + historyItem.get("name").getValue() + "\n");
                hist.append("Address: " + historyItem.get("address").getValue() + "\n");
                long newLastPlayed = Long.parseLong(historyItem.get("lastplayed").getValue());
                if(newLastPlayed < lastPlayed) {
                    hist.append("Out of order" + "\n");
                }
                lastPlayed = newLastPlayed;
                hist.append("Last Played: " + DateUtils.parse(lastPlayed) + "\n" + "\n");
            }
            MasterServer.SOURCE.query(Region.AUSTRALIA, new ServerListener() {

                @Override
                void inform(String update) {
                    net.append(update + "\n");
                }
                
            });
        } catch(IOException ex) {
            Logger.getLogger(MasterServer.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static final Logger LOG = Logger.getLogger(MasterServer.class.getName());

}
