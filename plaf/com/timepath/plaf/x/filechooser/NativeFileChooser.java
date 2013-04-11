package com.timepath.plaf.x.filechooser;

import com.timepath.plaf.OS;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class NativeFileChooser extends BaseFileChooser {

    private static final Logger LOG = Logger.getLogger(NativeFileChooser.class.getName());
    
    private BaseFileChooser getChooser() {
        BaseFileChooser chooser;
        if(OS.isWindows()) {
            chooser = new XFileDialogFileChooser();
        } else if(OS.isMac()) {
            chooser = new AWTFileChooser();
        } else if(OS.isLinux()) {
//            try {
                chooser = new ZenityFileChooser();
//            } catch(IOException ex) {
//                chooser = new SwingFileChooser();  
//            }
        } else {
            chooser = new SwingFileChooser();
        }
        chooser
                .setApproveButtonText(approveButtonText)
                .setTitle(dialogTitle)
                .setDialogType(dialogType)
                .setDirectory(directory)
                .setFile(file)
                .setFileMode(fileMode)
                .setMultiSelectionEnabled(multiSelectionEnabled)
                .setParent(parent)
                ;
        
        return chooser;
    }

    @Override
    public File[] choose() throws IOException {
        return getChooser().choose();
    }
}
