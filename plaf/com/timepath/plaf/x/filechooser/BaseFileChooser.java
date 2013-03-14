package com.timepath.plaf.x.filechooser;

import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public abstract class BaseFileChooser {
    
    private static final Logger LOG = Logger.getLogger(BaseFileChooser.class.getName());
    
    Frame parent;

    String title;

    File directory;
    
    public BaseFileChooser(Frame parent, String title, File directory) {
        this.parent = parent;
        this.title = title;
        this.directory = directory;
    }
    
    public abstract File choose(boolean directoryMode, boolean saveDialog) throws IOException;
    
}
