package com.timepath.plaf.x.filechooser;

import com.timepath.plaf.OS.WindowToolkit;
import com.timepath.plaf.linux.LinuxUtils;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class ZenityFileChooser extends BaseFileChooser {
    
    private static final Logger LOG = Logger.getLogger(ZenityFileChooser.class.getName());

    @Override
    public File[] choose() throws IOException {
        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add("zenity");
        cmd.add("--file-selection");
        if(this.isDirectoryMode()) {
            cmd.add("--directory");
        }
        if(this.isSaveDialog()) {
            cmd.add("--save");
            cmd.add("--confirm-overwrite");
        }
        if(this.isMultiSelectionEnabled()) {
            cmd.add("--multiple");
        }
        if(file != null || directory != null) {
            cmd.add("--filename=" + (directory != null ? (directory.getPath() + "/") : "") + (file != null ? file : ""));
        }
        String windowClass = WindowToolkit.getWindowClass();
        try {
            Toolkit xToolkit = Toolkit.getDefaultToolkit();
            Field awtAppClassNameField = xToolkit.getClass().getDeclaredField("awtAppClassName");
            boolean accessible = awtAppClassNameField.isAccessible();
            awtAppClassNameField.setAccessible(true);
            windowClass = (String) awtAppClassNameField.get(xToolkit);
            awtAppClassNameField.setAccessible(accessible);
        } catch(Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
        cmd.add("--class=" + windowClass);
//        cmd.add("--name=" + Main.projectName + " ");
        if(WindowToolkit.getWindowClass() != null) {
            cmd.add("--window-icon=" + LinuxUtils.getLinuxStore() + "icons/" + WindowToolkit.getWindowClass() + ".png");
        }
        cmd.add("--title=" + this.getTitle());
        if(this.getApproveButtonText() != null) {
            cmd.add("--ok-label=" + this.getApproveButtonText());
        }
//        cmd.add("--cancel-label=TEXT ");

        String[] exec = new String[cmd.size()];
        cmd.toArray(exec);
        LOG.log(Level.FINE, "zenity: {0}", Arrays.toString(exec));
        final Process proc = Runtime.getRuntime().exec(exec);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                proc.destroy();
            }
        });
        BufferedReader br = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        String selection = br.readLine();
//        String selection;
//        while((selection = br.readLine()) != null) {
            LOG.log(Level.INFO, "Zenity selection: {0}", selection);
//        }
        if(selection == null) {
            return null;
        } else {
            String[] selected = selection.split("\\|");
            File[] f = new File[selected.length];
            for(int i = 0; i < f.length; i++) {
                f[i] = new File(selected[i]);
            }
            return f;
        }
    }
    
}
