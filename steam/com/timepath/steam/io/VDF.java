package com.timepath.steam.io;

import com.timepath.hl2.io.util.Element;
import com.timepath.hl2.io.util.Property;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.tree.DefaultMutableTreeNode;

/**
 *
 * http://hpmod.googlecode.com/svn/trunk/tier1/KeyValues.cpp
 * http://hlssmod.net/he_code/public/tier1/KeyValues.h
 *
 * Standard KeyValues format loader
 *
 * @author timepath
 */
public class VDF {

    private static final Logger LOG = Logger.getLogger(VDF.class.getName());
    
    public VDF() {
        
    }

    public VDF(String file) {
    }

    public static void analyze(final File file, final DefaultMutableTreeNode top) {
        if(file.isDirectory()) {
            return;
        }

        Scanner s = null;
        try {
            RandomAccessFile rf = new RandomAccessFile(file.getPath(), "r");
            s = new Scanner(rf.getChannel());
            processAnalyze(s, top, new ArrayList<Property>(), file);
        } catch(StackOverflowError ex) {
            LOG.log(Level.WARNING, "Taking too long on {0}", file);
        } catch(FileNotFoundException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } finally {
            if(s != null) {
                s.close();
            }
        }
    }

    private static void processAnalyze(Scanner scanner, DefaultMutableTreeNode parent, ArrayList<Property> carried, File file) {
        while(scanner.hasNext()) {
            // Read values
            String line = scanner.nextLine().trim();

            List<String> matchList = new ArrayList<String>();
            
            // http://gskinner.com/RegExr/
            // Regex: Quotes with escapes, single line comments, braces, unquoted words
            // "(\\[\S]|[^"])*+"|(//.*[\S]*+)|(\{|\}[\S]*+)|([a-zA-Z\d\.]+)
            Pattern regex = Pattern.compile("\"(\\\\[\\S]|[^\"])*+\"|(//.*[\\S]*+)|(\\{|\\}[\\S]*+)|([a-zA-Z\\d\\.]+)");
            Matcher regexMatcher = regex.matcher(line);
            while(regexMatcher.find()) {
//                if(regexMatcher.group(1) != null) { // Add double-quoted string without the quotes
//                    matchList.add(regexMatcher.group(1));
//                } else { // Add unquoted word
                    matchList.add(regexMatcher.group());
//                }
            }
            
            String[] args = matchList.toArray(new String[0]);
            
            System.out.println(Arrays.toString(args));
            
            if(args.length > 3) {
                LOG.log(Level.WARNING, "More than 3 args on {0}: {1}", new Object[]{line, Arrays.toString(args)});
            } else {
                LOG.log(Level.FINE, "{0}:{1}", new Object[]{args.length, Arrays.toString(args)});
            }
            
            if(args.length == 0) {
                LOG.log(Level.FINE, "Carrying new line");
                carried.add(new Property("\\n", "", ""));
                continue;
            }
            
            String key = args[0];
            String val = "";
            String info = null;
            if(args.length > 1) {
                val = args[1];
            }
            if(args.length > 2) {
                info = args[2];
            }
            if(args.length > 3) {
                info = line;
            }
            
            if(line.equals("{")) { // just a { on its own line
                continue;
            }
            
            if(val.length() == 0 && !key.equals("}")) { // very good assumption
                val = "{";
            }
            
            if(line.equals("}")) { // for returning out of recursion: analyze: processAnalyze > processAnalyze < break < break
                Object obj = parent.getUserObject();
                if(obj instanceof Element) {
                    Element e = (Element) obj;
                    e.addProps(carried);
//                    e.validate(); // TODO: Thread safety. oops
                }
                LOG.log(Level.FINE, "Leaving {0}", obj);
                break;
            }

            // Process values

            Property p = new Property(key, val, info);

            if(key.startsWith("#") && key.split("[ \t]+").length == 2) { // #include, #base
                String rest = line.substring(line.indexOf('#') + 1);
                p.setKey("#" + rest);
                int idx2 = rest.indexOf(' ');
                if(idx2 == -1) {
                    idx2 = 0;
                }
                p.setValue(rest.substring(idx2));
                p.setInfo("");
                LOG.log(Level.FINE, "Carrying: {0}", line);
                carried.add(p);
                continue;
            } else if(line.startsWith("//")) {
                p.setKey("//");
                p.setValue(line.substring(line.indexOf("//") + 2)); // display this with .trim()
                p.setInfo("");
                LOG.log(Level.FINE, "Carrying: {0}", line);
                carried.add(p);
                continue;
            }

            if(p.getValue().equals("{")) { // make new sub
                Element childElement = new Element(p.getKey(), p.getInfo());
                childElement.setParentFile(file);
                LOG.log(Level.FINE, "Subbing: {0}", childElement);
                // If setting the properties of a section, put put the value in the info spot
                for(int i = 0; i < carried.size(); i++) {
                    Property prop = carried.get(i);
                    prop.setInfo(prop.getValue());
                    prop.setValue("");
                }
                childElement.addProps(carried);

                Object obj = parent.getUserObject();
                if(obj instanceof Element) {
                    Element e = (Element) obj;
                    e.addChild(childElement);
                }

                DefaultMutableTreeNode child = new DefaultMutableTreeNode(childElement);
//                child.setUserObject(childElement);
                parent.add(child);

                processAnalyze(scanner, child, carried, file);
            } else { // properties
                Object obj = parent.getUserObject();
                LOG.log(Level.FINE, "{2} >> {0},{1}", new Object[]{key, val, obj});
                if(obj instanceof Element) {
                    Element e = (Element) obj;
                    e.addProps(carried);
                    e.addProp(p);
                }
            }
        }
    }
}