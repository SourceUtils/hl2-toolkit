package com.timepath.plaf.x.filechooser;

import com.timepath.plaf.OS;
import java.io.File;
import java.util.logging.Logger;
import javax.swing.JFileChooser;

/**
 *
 * @author timepath
 */
public class SwingFileChooser extends BaseFileChooser {

    @Override
    public File[] choose() {
        if(OS.isLinux()) {
//            UIManager.put("FileChooserUI", "eu.kostia.gtkjfilechooser.ui.GtkFileChooserUI");
        }
        JFileChooser fd = new JFileChooser(directory);
        fd.setDialogTitle(dialogTitle);
        fd.setDialogType(this.isSaveDialog() ? JFileChooser.SAVE_DIALOG : JFileChooser.OPEN_DIALOG);
        fd.setSelectedFile(file);
        fd.setFileSelectionMode(this.isDirectoryMode() ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_AND_DIRECTORIES);
        if(!this.isSaveDialog()) {
            fd.setMultiSelectionEnabled(multiSelectionEnabled);
        }
        File[] selection = null;
        if(fd.showDialog(parent, approveButtonText) == JFileChooser.APPROVE_OPTION) {
            if(this.multiSelectionEnabled) {
                selection = fd.getSelectedFiles();
            } else {
                selection = new File[]{fd.getSelectedFile()};
            }
        }
        return selection;
    }

    private static final Logger LOG = Logger.getLogger(SwingFileChooser.class.getName());

}
