package com.timepath.plaf.x.filechooser;

import com.timepath.plaf.OS;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class NativeFileChooser extends BaseFileChooser {

    private static final Logger LOG = Logger.getLogger(NativeFileChooser.class.getName());

    public NativeFileChooser(Frame parent, String title, File directory) {
        super(parent, title, directory);
    }
    
    @Override
    public File choose(boolean directoryMode, boolean saveDialog) {
        File selection;
        if(OS.isWindows()) {
            selection = new XFileDialogFileChooser(parent, title, directory).choose(directoryMode, saveDialog);
        } else if(OS.isMac()) {
            selection = new AWTFileChooser(parent, title, directory).choose(directoryMode, saveDialog);
        } else if(OS.isLinux()) {
            try {
                selection = new ZenityFileChooser(parent, title, directory).choose(directoryMode, saveDialog);
            } catch(IOException ex) {
                selection = new SwingFileChooser(parent, title, directory).choose(directoryMode, saveDialog);
            }
        } else {
            selection = new SwingFileChooser(parent, title, directory).choose(directoryMode, saveDialog);
        }
        return selection;
    }
    
    public static File choose(Frame parent, String title, File directory, boolean directoryMode, boolean saveDialog) {
        return new NativeFileChooser(parent, title, directory).choose(directoryMode, saveDialog);
    }

}
