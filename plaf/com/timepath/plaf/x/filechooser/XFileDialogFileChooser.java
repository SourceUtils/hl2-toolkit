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
        String[] selection;
        XFileDialog fd = new XFileDialog(parent);
        fd.setTitle(title);
        if(directory != null) {
            fd.setDirectory(directory);
        }
        boolean multi = false;
        if(directoryMode) {
            if(multi) {
                selection = fd.getFolders();
            } else {
                selection = new String[]{fd.getFolder()};
            }
        } else {
            if(saveDialog) {
                selection = new String[]{fd.getSaveFile()};
            } else {
                if(multi) {
                    selection = fd.getFiles();
                } else {
                    selection = new String[]{fd.getFile()};
                }
            }
        }
        fd.dispose();
        if(selection == null) {
            return null;
        } else {
            if(directoryMode) { 
                return new File(selection[0]);
            } else {
                return new File(fd.getDirectory(), selection[0]);
            }
        }
    }

    private static final Logger LOG = Logger.getLogger(XFileDialogFileChooser.class.getName());

}
