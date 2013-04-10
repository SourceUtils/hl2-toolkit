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

    protected Frame parent;

    public Frame getParent() {
        return parent;
    }

    public BaseFileChooser setParent(Frame parent) {
        this.parent = parent;
        return this;
    }

    protected String dialogTitle;

    public String getTitle() {
        if(dialogTitle == null) {
            return this.isSaveDialog() ? "Save" : "Open";
        }
        return dialogTitle;
    }

    public BaseFileChooser setTitle(String title) {
        this.dialogTitle = title;
        return this;
    }

    protected File directory;

    public File getDirectory() {
        return directory;
    }

    public BaseFileChooser setDirectory(File directory) {
        this.directory = directory;
        return this;
    }
    
    public BaseFileChooser setDirectory(String directoryPath) {
        return setDirectory(new File(directoryPath));
    }
    
    protected File file;

    public File getFile() {
        return file;
    }

    public BaseFileChooser setFile(File file) {
        this.file = file;
        if(file != null) {
            setDirectory(file.getParentFile());
        }
        return this;
    }
    
    public BaseFileChooser setFile(String file) {
        return setFile(new File(file));
    }
    
    protected String approveButtonText;

    public String getApproveButtonText() {
        return approveButtonText;
    }

    public BaseFileChooser setApproveButtonText(String approveButtonText) {
        this.approveButtonText = approveButtonText;
        return this;
    }
    
    public static enum DialogType {
        SAVE_DIALOG, OPEN_DIALOG
    }
    
    protected DialogType dialogType = DialogType.OPEN_DIALOG;

    public DialogType getDialogType() {
        return dialogType;
    }

    public BaseFileChooser setDialogType(DialogType dialogType) {
        this.dialogType = dialogType;
        return this;
    }
    
    public boolean isSaveDialog() {
        return dialogType == DialogType.SAVE_DIALOG;
    }
    
    public static enum FileMode {
        DIRECTORIES_ONLY, FILES_ONLY, FILES_AND_DIRECTORIES
    }
    
    protected FileMode fileMode;

    public FileMode getFileMode() {
        return fileMode = FileMode.FILES_ONLY;
    }

    public BaseFileChooser setFileMode(FileMode fileMode) {
        this.fileMode = fileMode;
        return this;
    }
    
    public boolean isDirectoryMode() {
        return this.getFileMode() == FileMode.DIRECTORIES_ONLY;
    }
    
    protected boolean multiSelectionEnabled;
    
    public boolean isMultiSelectionEnabled() {
        return multiSelectionEnabled;
    }
    
    public BaseFileChooser setMultiSelectionEnabled(boolean multiSelectionEnabled) {
        this.multiSelectionEnabled = multiSelectionEnabled;
        return this;
    }
    
    public BaseFileChooser() {
        
    }
    
    public BaseFileChooser(File currentDirectory) {
        setDirectory(currentDirectory);
    }
    
    public BaseFileChooser(String currentDirectoryPath) {
        setDirectory(currentDirectoryPath);
    }

    public abstract File choose() throws IOException;
}
