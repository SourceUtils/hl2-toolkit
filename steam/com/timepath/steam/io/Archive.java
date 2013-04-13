package com.timepath.steam.io;

import java.io.File;
import java.io.IOException;
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

        public String getPath();
        
        public String getName();
        
        public String getAbsoluteName();

        public Archive getArchive();

        public boolean isComplete();

        public DirectoryEntry[] getImmediateChildren();

        public int getIndex();

        public void extract(File out) throws IOException;
        
    }

    public void analyze(DefaultMutableTreeNode top, boolean leaves);

}
