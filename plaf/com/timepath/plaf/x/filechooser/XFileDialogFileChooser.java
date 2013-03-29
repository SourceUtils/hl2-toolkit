package com.timepath.plaf.x.filechooser;

import com.timepath.plaf.OS;
import java.awt.Frame;
import java.io.File;
import java.util.logging.Logger;
import net.tomahawk.XFileDialog;

/**
 *
 * @author timepath
 */
public class XFileDialogFileChooser extends BaseFileChooser {

    public static void setTraceLevel(int level) {
        if(!OS.isWindows()) {
            return;
        }
        XFileDialog.setTraceLevel(0);
    }
    
    public XFileDialogFileChooser(Frame parent, String title, String directory) {
        super(parent, title, directory);
    }
    
    public File choose(boolean directoryMode, boolean saveDialog) {
        String selection;
        XFileDialog fd = new XFileDialog(parent);
        fd.setTitle(title);
        if(directory != null) {
            fd.setDirectory(directory);
        }
        if(directoryMode) {
            selection = fd.getFolder();
        } else {
            if(saveDialog) {
                selection = fd.getSaveFile();
            } else {
                selection = fd.getFile();
            }
        }
        fd.dispose();
        if(selection == null) {
            return null;
        } else {
            return new File(selection);
        }
    }
    private static final Logger LOG = Logger.getLogger(XFileDialogFileChooser.class.getName());
    
}
