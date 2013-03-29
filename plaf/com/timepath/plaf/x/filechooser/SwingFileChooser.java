package com.timepath.plaf.x.filechooser;

import com.timepath.plaf.OS;
import java.awt.Frame;
import java.io.File;
import java.util.logging.Logger;
import javax.swing.JFileChooser;

/**
 *
 * @author timepath
 */
public class SwingFileChooser extends BaseFileChooser {
    
    public SwingFileChooser(Frame parent, String title, String directory) {
        super(parent, title, directory);
    }

    @Override
    public File choose(boolean directoryMode, boolean saveDialog) {
        if(OS.isLinux()) {
//            UIManager.put("FileChooserUI", "eu.kostia.gtkjfilechooser.ui.GtkFileChooserUI");
        }
        JFileChooser fd = new JFileChooser(directory);
        fd.setFileSelectionMode(directoryMode ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_AND_DIRECTORIES);
        fd.setDialogType(saveDialog ? JFileChooser.SAVE_DIALOG : JFileChooser.OPEN_DIALOG);
        String selection = null;
        if(fd.showDialog(parent, null) == JFileChooser.APPROVE_OPTION) {
            selection = fd.getSelectedFile().getPath();
        }
        if(selection == null) {
            return null;
        } else {
            return new File(selection);
        }
    }
    private static final Logger LOG = Logger.getLogger(SwingFileChooser.class.getName());
    
}
