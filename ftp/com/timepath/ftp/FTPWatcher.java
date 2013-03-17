package com.timepath.ftp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * With reference to:
 * http://cr.yp.to/ftp.html
 * http://www.nsftools.com/tips/RawFTP.htm
 * http://www.ipswitch.com/support/ws_ftp-server/guide/v5/a_ftpref3.html
 * http://graham.main.nc.us/~bhammel/graham/ftp.html
 * http://www.codeguru.com/csharp/csharp/cs_network/sockets/article.php/c7409/A-C-FTP-Server.htm
 * 
 * Mounting requires CurlFtpFS
 *     # mkdir console
 *     # curlftpfs -o umask=0000,uid=1000,gid=1000,allow_other localhost:8000 console
 *     # cd console
 *     # ls -l
 *     # cd ..
 *     # fusermount -u console
 * 
 * Possible solutions:
 *     http://coderrr.wordpress.com/2008/12/20/automatically-flushing-redirected-or-piped-stdout/
 *     http://serverfault.com/questions/294218/is-there-a-way-to-redirect-output-to-a-file-without-buffering-on-unix-linux
 * Operation not supported:
 *     script -a -c 'ping www.google.com' -f out.log
 * 
 * @author timepath
 */
public class FTPWatcher {

    private static final Logger LOG = Logger.getLogger(FTPWatcher.class.getName());

    public static void main(String... args) {
        getInstance();
    }
    private static final FTPWatcher instance = new FTPWatcher();

    public static FTPWatcher getInstance() {
        return instance;
    }

    private ArrayList<FTPUpdateListener> listeners = new ArrayList<FTPUpdateListener>();
    
    public void addFileChangeListener(FTPUpdateListener listener) {
        listeners.add(listener);
    }
    
    private int port = 8000;
    private String file;
    private boolean shutdown;

    public FTPWatcher() {
        try {
            final ServerSocket sock = new ServerSocket(port, 0, InetAddress.getByName(null)); // cannot use java7 InetAddress.getLoopbackAddress(). On windows, this prevents firewall warnings. It's also good for security in general
            port = sock.getLocalPort();

            LOG.log(Level.FINE, "Listening on port {0}", port);

            Runtime.getRuntime().addShutdownHook(new Thread() {

                @Override
                public void run() {
                    LOG.info("FTP server shutting down...");
                }
            });

            new Thread(new Runnable() {

                private Socket data;
                private ServerSocket pasv;

                public void run() {
                    while (!shutdown) {
//                        LOG.info("Waiting for client...");
                        final Socket client;
                        try {
                            client = sock.accept();
                        } catch (IOException ex) {
                            Logger.getLogger(FTPWatcher.class.getName()).log(Level.SEVERE, null, ex);
                            continue;
                        }
                        new Thread(new Runnable() {
                            public void run() {
                                try {
                                    BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));
                                    PrintWriter pw = new PrintWriter(client.getOutputStream(), true);
                                    out(pw, "220 Welcome");
                                    while (!client.isClosed()) {
                                        try {
                                            String cmd = in(br);
                                            if (cmd == null) {
                                                break;
                                            }
                                            
                                            LOG.log(Level.INFO, "{0}", cmd);

                                            if (cmd.startsWith("USER")) {
                                                out(pw, "230 Logged in.");
                                            } else if (cmd.startsWith("SYST")) {
                                                //<editor-fold defaultstate="collapsed" desc="System type">
                                                out(pw, "215 UNIX Type: L8"); // XXX
                                                //</editor-fold>
                                            } else if (cmd.startsWith("QUIT")) {
                                                //<editor-fold defaultstate="collapsed" desc="Log out">
                                                out(pw, "221 Bye");
                                                client.close();
                                                //</editor-fold>
                                            } else if (cmd.startsWith("PORT")) {
                                                String[] args = cmd.substring(5).split(",");
                                                String sep = ".";
                                                String dataAddress = args[0] + sep + args[1] + sep + args[2] + sep + args[3];
                                                int dataPort = (Integer.parseInt(args[4]) * 256) + Integer.parseInt(args[5]);
                                                data = new Socket(InetAddress.getByName(dataAddress), dataPort);
                                                LOG.log(Level.FINE, "=== Data receiver: {0}", data);
                                                out(pw, "200 PORT command successful.");
                                            } else if (cmd.startsWith("LIST")) {
                                                out(pw, "150 Gathering /bin/ls -l output");
                                                if (pasv != null) {
                                                    data = pasv.accept();
                                                }
                                                PrintWriter out = new PrintWriter(data.getOutputStream(), true);
                                                out(out, "-rw-rw-rw- 1 timepath users 0 Jan 1 00:00 out.log");
                                                data.close();
                                                out(pw, "226 Listing completed");
                                            } else if (cmd.startsWith("RETR")) {
                                                out(pw, "150 Opening file");
                                                if (pasv != null) {
                                                    data = pasv.accept();
                                                }
                                                PrintWriter out = new PrintWriter(data.getOutputStream(), true);
        //                                        out(out, "");
                                                data.close();
                                                out(pw, "226 File sent");
                                            } else if (cmd.startsWith("CWD")) {
                                                String dir = cmd.substring(4);
                                                boolean dirAccessible = true;
                                                if (dirAccessible) {
                                                    out(pw, "250 Okay");
                                                } else {
                                                    out(pw, "550 " + dir + ": No such file or directory.");
                                                }
                                            } else if (cmd.startsWith("PWD")) {
                                                String dir = "/";
                                                boolean dirKnowable = true;
                                                if (dirKnowable) {
                                                    out(pw, "257 " + dir);
                                                } else {
                                                    out(pw, "550 Error");
                                                }
                                            } else if (cmd.startsWith("TYPE")) {
                                                out(pw, "200 Ok");
                                            } else if (cmd.startsWith("PASV")) {
                                                pasv = new ServerSocket(0);
                                                byte[] h = pasv.getInetAddress().getAddress();
                                                int[] p = {pasv.getLocalPort() / 256, pasv.getLocalPort() % 256};
                                                out(pw, "227 =" + h[0] + "," + h[1] + "," + h[2] + "," + h[3] + "," + p[0] + "," + p[1]);
                                            } else if (cmd.startsWith("SIZE")) {
                                                out(pw, "200 1024");
                                            } else if (cmd.startsWith("MDTM")) {
                                                out(pw, "200 " + new SimpleDateFormat("yyyyMMddhhmmss").format(new Date(System.currentTimeMillis() / 1000)));
                                            } else if (cmd.startsWith("FEAT")) {
                                                //<editor-fold defaultstate="collapsed" desc="Supported features">
                                                out(pw, "211-Extensions supported");
                                                //                                        out(pw, " SIZE");
                                                //                                        out(pw, " MDTM");
                                                //                                        out(pw, " MLST size*;type*;perm*;create*;modify*");
                                                //                                        out(pw, " LANG EN*");
                                                //                                        out(pw, " REST STREAM");
                                                //                                        out(pw, " TVFS");
                                                //                                        out(pw, " UTF8");
                                                out(pw, "211 end");
                                                //</editor-fold>
                                            } else if (cmd.startsWith("HELP")) {
                                                //<editor-fold defaultstate="collapsed" desc="comment">
                                                out(pw, "214-Commands supported:");
                                                out(pw, "STOR APPE PASV");
                                                out(pw, "214 End");
                                                //</editor-fold>out(pw, "214-Commands supported:");
                                                out(pw, "STOR APPE PASV");
                                                out(pw, "214 End");
                                            } else if (cmd.startsWith("SITE")) {
                                                out(pw, "200 Nothing to see here");
                                            } else if (cmd.startsWith("RNFR")) {
                                                //<editor-fold defaultstate="collapsed" desc="Rename file">
                                                String from = cmd.substring(5);
                                                out(pw, "350 Okay");
                                                String to = in(br).substring(5);
                                                out(pw, "250 Renamed");
                                                //</editor-fold>
                                            } else if (cmd.startsWith("STOR")) {
                                                //<editor-fold defaultstate="collapsed" desc="Upload file">
                                                String file = cmd.substring(5);
                                                String text = "";
                                                out(pw, "150 Entering Transfer Mode");
                                                if (pasv != null) {
                                                    data = pasv.accept();
                                                }
                                                BufferedReader in = new BufferedReader(new InputStreamReader(data.getInputStream()));
                                                PrintWriter out = new PrintWriter(data.getOutputStream(), true);
                                                String line;
                                                while ((line = in.readLine()) != null) {
                                                    LOG.log(Level.FINE, "=== {0}", line);
                                                    if(text.length() == 0) {
                                                        text = line;
                                                    } else {
                                                        text += "\r\n" + line;
                                                    }
                                                }
                                                data.close();
                                                for(int i = 0; i < listeners.size(); i++) {
                                                    listeners.get(i).fileChanged(text);
                                                }
                                                //                                                text = text.substring(0, text.length());
                                                LOG.log(Level.INFO, "***\r\n{0}", text);
                                                out(pw, "226 File uploaded successfully");
                                                //</editor-fold>
                                            } else {
                                                LOG.log(Level.WARNING, "Unsupported operation {0}", cmd);
                                            }
                                        } catch (Exception ex) {
                                            LOG.log(Level.SEVERE, null, ex);
                                        }
                                    }
                                    LOG.info("Socket closed");
                                } catch (IOException ex) {
                                    Logger.getLogger(FTPWatcher.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                        }).start();
                    }
                }

                private String in(BufferedReader in) throws IOException {
                    String s = in.readLine();
                    LOG.log(Level.FINE, "<<< {0}", s);
                    return s;
                }

                private void out(PrintWriter out, String cmd) {
                    out.print(cmd + "\r\n");
                    out.flush();
                    LOG.log(Level.FINE, ">>> {0}", cmd);
                }
            }, "FTP Server").start();
        } catch (IOException ex) {
            Logger.getLogger(FTPWatcher.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}