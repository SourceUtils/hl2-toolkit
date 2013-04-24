package com.timepath.steam.io.util;

import com.timepath.hl2.io.util.Property;
import java.io.File;
import java.util.ArrayList;
import java.util.logging.Logger;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 *
 * @author timepath
 */
public class VDFNode extends DefaultMutableTreeNode {

    public VDFNode() {
    }

    public VDFNode(String key) {
        this.key = key;
    }

    public VDFNode(String key, Object value) {
        this.key = key;
        this.userObject = value;
    }

    private String key;

    public void setKey(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

    public VDFNode get(int index) {
        return (VDFNode) this.getChildAt(index);
    }

    public VDFNode get(String key) {
        VDFNode node;
        for(Object o : this.children) {
            if(!(o instanceof VDFNode)) {
                continue;
            }
            node = (VDFNode) o;
            if(node.getKey().equals(key)) {
                return node;
            }
        }
        return null;
    }

    public void setValue(Object o) {
        this.setUserObject(o);
    }

    public String getValue() {
        return (String) this.getUserObject();
    }

    public int intValue() {
        return Integer.parseInt(getValue());
    }

    @Override
    public String toString() {
        if(this.isLeaf()) {
            return getKey() + " == " + getValue();
        } else {
            return this.getKey();
        }
    }

    private static final Logger LOG = Logger.getLogger(VDFNode.class.getName());

    public ArrayList<Property> getProperties() {
        ArrayList<Property> props = new ArrayList<Property>();
        for(int i = 0; i < this.getChildCount(); i++) {
            TreeNode tn = this.getChildAt(i);
            if(!(tn instanceof VDFNode)) {
                continue;
            }
            VDFNode v = (VDFNode) tn;
            props.add(new Property(v.getKey(), v.getValue(), ""));
        }
        return props;
    }

    protected String fileName;

    public void setFile(File file) {
        setFile(file.getName());
    }

    public void setFile(String name) { // todo: case insensitivity
        if(name.contains(".")) {
            this.fileName = name.split("\\.")[0];
        } else {
            this.fileName = name;
        }
    }

    public String getFile() {
        return fileName;
    }
}