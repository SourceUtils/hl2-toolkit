package com.timepath.steam.net.test;

import com.timepath.DateUtils;
import com.timepath.backports.javax.swing.SwingWorker;
import com.timepath.steam.SteamUtils;
import com.timepath.steam.io.VDF;
import com.timepath.steam.io.util.VDFNode;
import com.timepath.steam.net.MasterServer;
import com.timepath.steam.net.Region;
import com.timepath.steam.net.ServerListener;
import com.timepath.steam.net.SourceServer;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
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
public class ServerTest {

    public static void main(String... args) {
        try {
            JFrame f = new JFrame("Servers");
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            f.setPreferredSize(new Dimension(400, 600));
            JTabbedPane tabs = new JTabbedPane();
            final JTextArea test = new JTextArea();
            tabs.add("Test", new JScrollPane(test));
            final JTextArea net = new JTextArea();
            tabs.add("Internet", new JScrollPane(net));
            final JTextArea favs = new JTextArea();
            tabs.add("Favorites", new JScrollPane(favs));
            final JTextArea hist = new JTextArea();
            tabs.add("History", new JScrollPane(hist));
            f.setContentPane(tabs);
            f.pack();
            f.setVisible(true);

            new SwingWorker<Void, Void>() {
                @Override
                protected Void doInBackground() throws Exception {
                    SteamUtils.SteamID user = SteamUtils.getUser();
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
                    return null;
                }
            }.execute();
            
            LOG.info("Getting info ...");
            
            SourceServer ss = new SourceServer("203.33.121.209", 27027);
            ss.getInfo(new ServerListener() { public void inform(String update) { test.append(update + "\n"); } });
            ss.getRules(new ServerListener() { public void inform(String update) { test.append(update + "\n"); } });
            
//            LOG.info("Querying ...");
//
//            MasterServer.SOURCE.query(Region.AUSTRALIA, "\\gamedir\\tf", new ServerListener() {
//                public void inform(String update) {
////                    String ip = update.split(":")[0];
////                    InetAddress addr = null;
////                    try {
////                        addr = InetAddress.getByName(ip);
////                    } catch(UnknownHostException ex) {
////                    }
//                    String host = update;
////                    String host = (addr != null && !addr.equals(ip)) ? addr.getCanonicalHostName() + " (" + update + ")" : update;
//                    net.append(host + "\n");
//                }
//            });
        } catch(IOException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

    private static final Logger LOG = Logger.getLogger(ServerTest.class.getName());

}
