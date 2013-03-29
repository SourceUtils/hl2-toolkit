package com.timepath.plaf.x.filechooser;

import com.timepath.plaf.OS;
import java.awt.FileDialog;
import java.awt.Frame;
import java.io.File;
import java.io.FilenameFilter;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class AWTFileChooser extends BaseFileChooser {
    
    public AWTFileChooser(Frame parent, String title, String directory) {
        super(parent, title, directory);
    }

    @Override
    public File choose(boolean directoryMode, boolean saveDialog) {
        if(OS.isMac()) {
            System.setProperty("apple.awt.fileDialogForDirectories", Boolean.toString(directoryMode));
        }
        FileDialog fd = new FileDialog(parent, title);
        if(directoryMode) {
            fd.setFilenameFilter(new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return new File(dir, name).isDirectory();
                }
            });
        }
        if(directory != null) {
            fd.setDirectory(directory);
        }
        fd.setMode(saveDialog ? FileDialog.SAVE : FileDialog.LOAD);
        fd.setVisible(true);
        if(fd.getDirectory() == null || fd.getFile() == null) {
            return null;
        }
        String selection = fd.getDirectory() + fd.getFile();
        if(selection == null) {
            return null;
        } else {
            return new File(selection);
        }
    }
    private static final Logger LOG = Logger.getLogger(AWTFileChooser.class.getName());
    
}
