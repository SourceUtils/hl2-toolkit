package com.timepath.steam.io;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * @author timepath
 */
public interface Archive {

    public Archive load(File f);

    public InputStream get(int index);
    
    public ArrayList<DirectoryEntry> find(String search);
    
    public DirectoryEntry getRoot();
    
    public interface DirectoryEntry {

        public int getItemSize();

        public Object getAttributes();

        public boolean isDirectory();

        public Object getPath();

        public Object getGCF();

        public boolean isComplete();
        
    }

    public void analyze(DefaultMutableTreeNode top, boolean leaves);

}
