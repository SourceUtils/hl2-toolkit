package com.timepath.steam.io;

import com.timepath.steam.io.util.VDFNode;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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

    public static boolean isBinary(File f) {
        try {
            RandomAccessFile rf = new RandomAccessFile(f, "r");
            rf.seek(2);
            return (rf.read() == 0x56);
        } catch(FileNotFoundException ex) {
            Logger.getLogger(VDF.class.getName()).log(Level.SEVERE, null, ex);
        } catch(IOException ex) {
            Logger.getLogger(VDF.class.getName()).log(Level.SEVERE, null, ex);
        }
        return false;
    }

    public static void analyze(final File file, final DefaultMutableTreeNode top) {
        if(file.isDirectory()) {
            return;
        }

        Scanner s = null;
        try {
            RandomAccessFile rf = new RandomAccessFile(file.getPath(), "r");
            s = new Scanner(rf.getChannel());
            processAnalyze(s, top, file);
        } catch(StackOverflowError ex) {
            LOG.log(Level.WARNING, "{0} is too deep (Stack overflowed)", file);
        } catch(FileNotFoundException ex) {
            LOG.log(Level.SEVERE, null, ex);
        } finally {
            if(s != null) {
                s.close();
            }
        }
    }
    
    public static VDFNode load(File f) {
        VDFNode vn = new VDFNode();
        analyze(f, vn);
        return vn;
    }

    private static final Pattern quoteRegex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"");

    private static final Pattern platformRegex = Pattern.compile("(?:\\[(!)?\\$)(.*)(?:\\])");

    protected static void processAnalyze(Scanner scanner, DefaultMutableTreeNode parent, File file) {
        while(scanner.hasNext()) {
            String line = scanner.nextLine().trim();
            if(line.length() == 0) {
                continue;
            }
            String comment = null;
            int cIndex = line.indexOf("//");
            if(cIndex != -1) {
                comment = line.substring(cIndex);
                line = line.substring(0, cIndex);
            }

            List<String> matchList = new ArrayList<String>();
            Matcher regexMatcher = quoteRegex.matcher(line);
            while(regexMatcher.find()) {
                if(regexMatcher.group(1) != null) {
                    // Add double-quoted string without the quotes
                    matchList.add(regexMatcher.group(1));
                } else {
                    // Add unquoted word
                    matchList.add(regexMatcher.group());
                }
            }
            if(matchList.isEmpty()) {
                if(comment != null && comment.trim().length() > 0) {
                    LOG.log(Level.FINE, "Carrying extra: [{0}]", comment);
                    parent.add(new DefaultMutableTreeNode(comment));
                }
                continue;
            }
            String[] args = matchList.toArray(new String[0]);
            LOG.log(Level.FINE, "{0}:{1}", new Object[]{args.length, Arrays.toString(args)});

            String val = null;
            if(args.length >= 2) {
                val = args[1];
                Matcher plafMatcher = platformRegex.matcher(args[args.length - 1]);
                if(plafMatcher.find()) {
                    boolean bool = plafMatcher.group(1) == null; // true if "!" is present
                    String platform = plafMatcher.group(2);
                    if(args[args.length - 1] == val) { // yes, this is supposed to be a direct check
                        val = null;
                    }
                } else if(args.length > 2) {
                    LOG.log(Level.WARNING, "More than 2 args on {0}: {1}", new Object[]{line, Arrays.toString(args)});
                }
            }

            if(args[0].equals("{")) { // just a { on its own line
                continue;
            }

            if(args[0].equals("}")) { // for returning out of recursion: analyze: processAnalyze > processAnalyze < break < break
                Object obj = parent.getUserObject();
//                if(obj instanceof Element) {
//                    ((Element)e).validate(); // TODO: Thread safety. oops
//                }
                LOG.log(Level.FINE, "Leaving {0}", obj);
                break; // TODO: /tf/scripts/HudAnimations_tf.txt
            } else if(val == null) { // very good assumption
                val = "{";
            }

            VDFNode p = new VDFNode(args[0], val);
            p.setFile(file);
            parent.add(p);
            if(val.equals("{")) { // make new sub
                LOG.log(Level.FINE, "Stepping into {0}", p);
                processAnalyze(scanner, p, file);
            }
        }
    }
}
