package com.timepath.plaf.x.filechooser;

import com.timepath.plaf.OS;
import java.awt.FileDialog;
import java.io.File;
import java.io.FilenameFilter;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class AWTFileChooser extends BaseFileChooser {

    @Override
    public File[] choose() {
        if(OS.isMac()) {
            System.setProperty("apple.awt.fileDialogForDirectories", Boolean.toString(this.isDirectoryMode()));
        }
        FileDialog fd = new FileDialog(parent, dialogTitle);
        if(directory != null) {
            fd.setDirectory(directory.getPath());
        }
        if(file != null) {
            fd.setFile(file.getPath());
        }
        if(this.isDirectoryMode()) {
            if(!OS.isMac()) {
                LOG.warning("Using AWT for directory selection on non mac system - not ideal");
            }
            fd.setFilenameFilter(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return new File(dir, name).isDirectory();
                }
            });
        }
        fd.setMode(this.isSaveDialog() ? FileDialog.SAVE : FileDialog.LOAD);
        fd.setVisible(true);
        if(fd.getDirectory() == null || fd.getFile() == null) { // cancelled
            return null;
        }
        return new File[]{new File(fd.getDirectory() + File.pathSeparator + fd.getFile())};
    }

    private static final Logger LOG = Logger.getLogger(AWTFileChooser.class.getName());

}
