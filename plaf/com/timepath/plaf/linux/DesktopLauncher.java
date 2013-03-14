package com.timepath.plaf.linux;

import com.timepath.Utils;
import com.timepath.FileUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class DesktopLauncher {

    private static final Logger LOG = Logger.getLogger(DesktopLauncher.class.getName());

    public static void create(String desktop, String root, String[] icons, String[] iconFiles) {
        createLauncher(desktop, iconFiles[0]);
        createIcons(root, icons, iconFiles);
    }

    private static void createLauncher(String desktop, String icon) {
        File destFile = new File(FileUtils.getLinuxStore()+ "applications/" + desktop + ".desktop");
        LOG.log(Level.INFO, "Linux .desktop file: {0}", destFile);

        StringBuilder sb = new StringBuilder();
        sb.append("[Desktop Entry]").append("\n");
        sb.append("Version=1.0").append("\n");
        sb.append("StartupWMClass=").append(desktop).append("\n");
        sb.append("Exec=java -jar Dropbox/Public/tf/Hud\\ Editor/TF2\\ HUD\\ Editor.jar %U").append("\n"); // TODO: fixme. Get a dedicated install directory.
        if(icon != null) {
            sb.append("Icon=").append(icon).append("\n");
        }
        sb.append("Type=Application").append("\n");
        sb.append("StartupNotify=true").append("\n");
        sb.append("Terminal=false").append("\n");
        sb.append("Keywords=TF2;HUD;Editor;WYSIWYG;").append("\n");
        sb.append("Categories=Game;").append("\n");
        sb.append("Name=TF2 HUD Editor").append("\n");
        sb.append("GenericName=TF2 HUD Editor").append("\n");
        sb.append("Comment=Edit TF2 HUDs").append("\n");
        sb.append("Actions=New;").append("\n");
        sb.append("[Desktop Action New]").append("\n");
        sb.append("Name=New HUD").append("\n");
        sb.append("Exec=java -jar Dropbox/Public/tf/Hud\\ Editor/TF2\\ HUD\\ Editor.jar"); // TODO: fixme

        boolean flag = false;
        String md51, md52;
        try {
            md51 = Utils.takeMD5(Utils.loadFile(destFile));
            md52 = Utils.takeMD5(sb.toString().getBytes());
            if(!md51.equals(md52)) { // TODO: Check date to allow for user customisation
                LOG.log(Level.INFO, "{0} vs {1}", new Object[]{md51, md52});
                flag = true;
            }
        } catch(Exception e) {
            flag = true;
        }

        PrintWriter out = null;
        if(flag) {
            try {
                if(!destFile.exists()) {
                    destFile.getParentFile().mkdirs();
                    destFile.createNewFile();
                }
                FileUtils.chmod777(destFile);
                out = new PrintWriter(new FileOutputStream(destFile));
                out.print(sb.toString());
            } catch(FileNotFoundException ex) {
                Logger.getLogger(DesktopLauncher.class.getName()).log(Level.SEVERE, null, ex);
            } catch(IOException ex) {
                Logger.getLogger(DesktopLauncher.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                if(out != null) {
                    out.close();
                }
            }
        }
    }

    private static void createIcons(String root, String[] icons, String[] iconFiles) {
        if(!root.endsWith("/")) {
            root += "/";
        }
        for(int i = 0; i < icons.length; i++) {
            try {
                File destFile = new File(FileUtils.getLinuxStore() + "icons/" + iconFiles[i] + icons[i].substring(icons[i].lastIndexOf('.') > 0 ? icons[i].lastIndexOf('.') : icons[i].length()));

                if(!destFile.getParentFile().exists()) {
                    destFile.getParentFile().mkdirs();
                }
                if(!destFile.exists()) {
                    destFile.createNewFile();
                }

                InputStream in = DesktopLauncher.class.getResourceAsStream(root + icons[i]);
                if(in == null) {
                    continue;
                }
                FileOutputStream out = new FileOutputStream(destFile);

                try {
                    byte[] buf = new byte[1024];
                    int len;
                    while((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                } finally {
                    if(in != null) {
                        in.close();
                    }
                    if(out != null) {
                        out.close();
                    }
                    destFile.setExecutable(false);
                }
            } catch(IOException ex) {
                LOG.log(Level.WARNING, null, ex);
                continue;
            }
        }
    }

    private DesktopLauncher() {
    }
}