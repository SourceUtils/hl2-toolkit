package com.timepath.ftp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * With reference to:
 * http://cr.yp.to/ftp.html
 * http://www.nsftools.com/tips/RawFTP.htm
 * http://www.ipswitch.com/support/ws_ftp-server/guide/v5/a_ftpref3.html
 * http://graham.main.nc.us/~bhammel/graham/ftp.html
 * 
 * @author timepath
 */
public class FTPWatcher {
	
	private static final Logger LOG = Logger.getLogger(FTPWatcher.class.getName());
	
	public static void main(String... args) {
		new FTPWatcher("test.txt").start();
	}
	
	private int port = 8000;
	
	private String file;
    
    private boolean shutdown;
	
	public FTPWatcher(String file) {
		this.file = file;
	}
	
	public void start() {
        try {
            final ServerSocket sock = new ServerSocket(port, 0, InetAddress.getByName(null)); // cannot use java7 InetAddress.getLoopbackAddress(). On windows, this prevents firewall warnings. It's also good for security in general
            port = sock.getLocalPort();

            LOG.log(Level.INFO, "Listening on port {0}", port);

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
                    while(!shutdown) {
                        try {
                            LOG.info("Waiting for client...");
                            Socket client = sock.accept();
                            BufferedReader br = new BufferedReader(new InputStreamReader(client.getInputStream()));
                            PrintWriter pw = new PrintWriter(client.getOutputStream(), true);
                            out(pw, "220 Welcome");
                            while(!client.isClosed()) {
                                try {
                                    String cmd = in(br);
                                    if(cmd == null) {
                                        break;
                                    }

                                    if(cmd.startsWith("USER")) {
                                        out(pw, "230 Logged in.");
                                    } else if(cmd.startsWith("SYST")) {
                                        out(pw, "215 UNIX Type: L8"); // XXX
                                    } else if(cmd.startsWith("QUIT")) {
                                        out(pw, "221 Bye");
                                        client.close();
                                    } else if(cmd.startsWith("PORT")) {
                                        String[] args = cmd.substring(5).split(",");
                                        String sep = ".";
                                        String dataAddress = args[0] + sep + args[1] + sep + args[2] + sep + args[3];
                                        int dataPort = (Integer.parseInt(args[4]) * 256) + Integer.parseInt(args[5]);
                                        data = new Socket(InetAddress.getByName(dataAddress), dataPort);
                                        LOG.log(Level.INFO, "=== Data receiver: {0}", data);
                                        out(pw, "200 PORT command successful.");
                                    } else if(cmd.startsWith("LIST") || cmd.startsWith("RETR")) {
                                        out(pw, "150 Opening ASCII mode data connection for /bin/ls");
                                        if(pasv != null) {
                                            data = pasv.accept();
                                        }
                                        PrintWriter out = new PrintWriter(data.getOutputStream(), true);
                                        out(out, "-rw-r--r--   1 ftpuser  ftpusers     14886 Dec  3 15:22 Acmemail.TXT");
                                        data.close();
                                        out(pw, "226 Listing completed.");
                                    } else if(cmd.startsWith("CWD")) {
                                        String dir = cmd.substring(4);
                                        boolean dirAccessible = true;
                                        if(dirAccessible) {
                                            out(pw, "250 Okay");
                                        } else {
                                            out(pw, "550 " + dir + ": No such file or directory.");
                                        }
                                    } else if(cmd.startsWith("PWD")) {
                                        String dir = "/";
                                        boolean dirKnowable = true;
                                        if(dirKnowable) {
                                            out(pw, "257 " + dir);
                                        } else {
                                            out(pw, "550 Error");
                                        }
                                    } else if(cmd.startsWith("TYPE")) {
                                        out(pw, "200 Ok");
                                    } else if(cmd.startsWith("PASV")) {
                                        pasv = new ServerSocket(0);
                                        byte[] h = pasv.getInetAddress().getAddress();
                                        int[] p = {pasv.getLocalPort() / 256, pasv.getLocalPort() % 256};
                                        out(pw, "227 =" + h[0] + "," + h[1] + "," + h[2] + "," + h[3] + "," + p[0] + "," + p[1]);
                                    } else if(cmd.startsWith("SIZE")) {
                                        out(pw, "200 1024");
                                    } else if(cmd.startsWith("MDTM")) {
                                        out(pw, "200 " + new SimpleDateFormat("yyyyMMddhhmmss").format(new Date(System.currentTimeMillis() / 1000)));
                                    } else if(cmd.startsWith("FEAT")) {
                                        String features = ""
                                                + "211-Extensions supported\r\n"
//                                                + " SIZE\r\n"
//                                                + " MDTM\r\n"
//                                                + " MLST size*;type*;perm*;create*;modify*\r\n"
//                                                + " LANG EN*\r\n"
//                                                + " REST STREAM\r\n"
//                                                + " TVFS\r\n"
//                                                + " UTF8\r\n"
                                                + "211 end";
                                        out(pw, features);
                                    } else if(cmd.startsWith("SITE")) {
                                        out(pw, "200 Nothing to see here");
                                    } else if(cmd.startsWith("RNFR")) {
                                        String from = cmd.substring(5);
                                        out(pw, "350 Okay");
                                        String to = in(br).substring(5);
                                        out(pw, "250 Renamed");
                                    } else if(cmd.startsWith("STOR")) {
                                        String file = cmd.substring(5);
//                                        out(pw, "250 Okay");
                                    }
                                } catch(Exception ex) {
                                    LOG.log(Level.SEVERE, null, ex);
                                }
                            }
                            LOG.info("Socket closed");
                        } catch(IOException ex) {
                            Logger.getLogger(FTPWatcher.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
                
                private String in(BufferedReader in) throws IOException {
                    String s = in.readLine();
                    LOG.log(Level.INFO, "<<< {0}", s);
                    return s;
                }
                
                private void out(PrintWriter out, String cmd) {
                    out.print(cmd + "\r\n");
                    out.flush();
                    LOG.log(Level.INFO, ">>> {0}", cmd);
                }
                
            }, "FTP Server").start();
        } catch(IOException ex) {
            Logger.getLogger(FTPWatcher.class.getName()).log(Level.SEVERE, null, ex);
        }
	}
}