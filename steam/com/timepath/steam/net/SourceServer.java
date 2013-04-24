package com.timepath.steam.net;

import com.timepath.DataUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * https://developer.valvesoftware.com/wiki/Server_Queries
 *
 * @author timepath
 */
public class SourceServer extends Server {

    public SourceServer(String hostname) {
        super(hostname);
    }

    public SourceServer(String hostname, int port) {
        super(hostname, port);
    }

    private String getString(ByteBuffer buf) {
        ByteBuffer cloned = DataUtils.getTextBuffer(buf.duplicate(), true);
        buf.position(buf.position() + cloned.limit() - cloned.position());
        return Charset.forName("UTF-8").decode(cloned).toString();
    }

    public void getInfo(ServerListener l) throws IOException {
        if(l == null) {
            l = ServerListener.DUMMY;
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
        baos.write(0x54);
        baos.write(("Source Engine Query" + "\0").getBytes());
        ByteBuffer send = ByteBuffer.wrap(baos.toByteArray());
        this.send(send);

        ByteBuffer buf = this.get();
        buf.order(ByteOrder.LITTLE_ENDIAN);

        int packHeader = buf.getInt();
        if(packHeader != -1) {
            LOG.log(Level.SEVERE, "Invalid packet header {0}", packHeader);
        }

        byte header = buf.get();
        if(header != 0x49) {
            LOG.log(Level.SEVERE, "Invalid header {0}", header);
        }

        byte protocol = buf.get();
        l.inform("Protocol: " + protocol);

        String name = getString(buf);
        l.inform("Name: '" + name + "'");

        String map = getString(buf);
        l.inform("Map: '" + map + "'");

        String gamedir = getString(buf);
        l.inform("Gamedir: '" + gamedir + "'");

        String game = getString(buf);
        l.inform("Game: '" + game + "'");

        short appID = buf.getShort();
        l.inform("AppID: '" + appID + "'");

        byte playerCount = buf.get();
        l.inform("Players: '" + playerCount + "'");

        byte playerCountMax = buf.get();
        l.inform("Capacity: '" + playerCountMax + "'");

        byte botCount = buf.get();
        l.inform("Bots: '" + botCount + "'");

        ServerType type = ServerType.valueFor(buf.get());
        l.inform("Type: '" + type + "'");

        Environment env = Environment.valueFor(buf.get());
        l.inform("Environment: '" + env + "'");

        boolean visibility = (buf.get() == 0);
        l.inform("Visible: '" + visibility + "'");

        boolean secure = (buf.get() == 1);
        l.inform("VAC: '" + secure + "'");

        String version = getString(buf);
        l.inform("Version: '" + version + "'");

        byte edf = buf.get();
        boolean edfPort = (edf & 0x80) != 0;
        boolean edfSteamID = (edf & 0x10) != 0;
        boolean edfSTV = (edf & 0x40) != 0;
        boolean edfTags = (edf & 0x20) != 0;
        boolean edfGameID = (edf & 0x01) != 0;

        if(edfPort) {
            short portLocal = buf.getShort();
            l.inform("Port: '" + portLocal + "'");
        }
        if(edfSteamID) { // TODO: check
//            ByteBuffer d = buf.duplicate();
//            d.limit(buf.position() + 8);
//            LOG.info(DataUtils.hexDump(d.slice()));
            BigInteger sid = BigInteger.valueOf(buf.getLong());
            if(sid.compareTo(BigInteger.ZERO) < 0) {
                sid = sid.add(BigInteger.ONE.shiftLeft(64));
            }
            l.inform("SteamID: '" + sid.toString() + "'");
        }
        if(edfSTV) {
            short stvPort = buf.getShort();
            l.inform("STV Port: '" + port + "'");
            String stvName = getString(buf);
            l.inform("STV Name: '" + stvName + "'");
        }
        if(edfTags) {
            String tags = getString(buf);
            l.inform("Tags: '" + tags + "'");
        }
        if(edfGameID) {
            BigInteger gid = BigInteger.valueOf(buf.getLong());
            if(gid.compareTo(BigInteger.ZERO) < 0) {
                gid = gid.add(BigInteger.ONE.shiftLeft(64));
            }
            l.inform("GameID: '" + gid + "'");
        }
    }

    public void getRules(ServerListener l) throws IOException {
        if(l == null) {
            l = ServerListener.DUMMY;
        }

        //<editor-fold defaultstate="collapsed" desc="Get a challenge key">
        ByteArrayOutputStream challengeOut = new ByteArrayOutputStream();
        challengeOut.write(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
        challengeOut.write(0x56);
        challengeOut.write(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
        ByteBuffer challengeSend = ByteBuffer.wrap(challengeOut.toByteArray());
        this.send(challengeSend);

        ByteBuffer challengeGet = this.get();
        challengeGet.order(ByteOrder.LITTLE_ENDIAN);

        int challengepackHeader = challengeGet.getInt();
        if(challengepackHeader != -1) {
            LOG.log(Level.SEVERE, "Invalid packet header {0}", challengepackHeader);
        }

        byte challengeheader = challengeGet.get();
        if(challengeheader != 0x41) {
            LOG.log(Level.SEVERE, "Invalid header {0}", challengeheader);
        }

        byte[] challengeKey = new byte[4];
        challengeGet.get(challengeKey);
        //</editor-fold>


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF});
        baos.write(0x56);
        baos.write(challengeKey);
        ByteBuffer send = ByteBuffer.wrap(baos.toByteArray());

        ByteBuffer ruleBuf = ByteBuffer.allocate(4000);
        ruleBuf.order(ByteOrder.LITTLE_ENDIAN);

        int ruleCount = 0;
        for(;;) {
            send.rewind();
            this.send(send);
            ByteBuffer buf = this.get();
            buf.order(ByteOrder.LITTLE_ENDIAN);
            int packHeader = buf.getInt();
            if(packHeader != -2) {
                LOG.log(Level.SEVERE, "Invalid packet header {0}", packHeader);
            }
            int reqID = buf.getInt();
            int fragments = buf.get();
            int id = buf.get() + 1; // zero-indexed
            int payloadLength = buf.getShort();
            if(id == 1) {
                int pack2Header = buf.getInt();
                if(pack2Header != -1) {
                    LOG.log(Level.SEVERE, "Invalid packHeader {0}", pack2Header);
                }
                byte header = buf.get();
                if(header != 0x45) {
                    LOG.log(Level.SEVERE, "Invalid header {0}", header);
                }
                ruleCount = buf.getShort();
            }

            LOG.log(Level.INFO, "{0} / {1}", new Object[]{id, fragments});
            byte[] data = new byte[buf.remaining()];
            buf.get(data);
            ruleBuf.put(data);
            if(id == fragments) {
                break;
            }
        }
        ruleBuf.flip();
        LOG.log(Level.INFO, "Rules: {0}", ruleCount);
        LOG.log(Level.INFO, "Remaining: {0}", ruleBuf.remaining());
        for(int ruleIndex = 1; ruleIndex < ruleCount + 1; ruleIndex++) {
            if(ruleBuf.remaining() == 0) {
                break;
            }
            String key = getString(ruleBuf);
            String value = getString(ruleBuf);
            l.inform("[" + ruleIndex + "/" + ruleCount + "] " + "'" + key + "' = '" + value + "'");
        }

        LOG.info("Received");
    }

    private enum ServerType {

        DEDICATED('d'), LISTEN('l'), SOURCE_TV('p');

        char code;

        private ServerType(char code) {
            this.code = code;
        }

        public static ServerType valueFor(byte b) {
            for(ServerType t : ServerType.values()) {
                if(t.code == b) {
                    return t;
                }
            }
            return null;
        }
    }

    private enum Environment {

        WINDOWS('w'), LINUX('l');

        char code;

        private Environment(char code) {
            this.code = code;
        }

        public static Environment valueFor(byte b) {
            for(Environment t : Environment.values()) {
                if(t.code == b) {
                    return t;
                }
            }
            return null;
        }
    }

    private static final Logger LOG = Logger.getLogger(SourceServer.class.getName());

}
