package com.timepath.hl2.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author timepath
 */
public class CVarList {

    public static Map<String, CVar> analyze(File f) {
        Map<String, CVar> map = new HashMap<String, CVar>();
        try {
            RandomAccessFile rf = new RandomAccessFile(f.getPath(), "r");
            Scanner scanner = new Scanner(rf.getChannel());
            while(scanner.hasNext()) {
                String line = scanner.nextLine();
                String[] components = line.split(":");
                if(components.length < 3) {
                    continue;
                }
                CVar c = new CVar();
                String component;
                for(int i = 0; i < components.length; i++) {
                    component = components[i].trim();
                    switch(i) {
                        case 0:
                            c.name = component;
                            break;
                        case 1:
                            c.value = component;
                            break;
                        case 2:
                            ArrayList<String> tags = new ArrayList<String>();
                            Pattern tagComponentRegex = Pattern.compile("[^, \\\":]+");
                            Matcher tagComponentRegexMatcher = tagComponentRegex.matcher(component);
                            while(tagComponentRegexMatcher.find()) {
                                tags.add(tagComponentRegexMatcher.group());
                            }
                            c.tags = tags;
                            break;
                        case 3:
                            c.desc = component;
                            break;
                    }
                }
                if(map.containsKey(c.name)) {
                    CVar other = map.get(c.name);
                    if(!c.equals(other)) {
                        LOG.log(Level.WARNING, "Duplicate entries:\n{0}\n{1}", new Object[]{other, c});
                    } else {
                        LOG.log(Level.FINE, "Duplicate entries, blame valve:\n{0}\n{1}", new Object[]{other, c});
                    }
                }
                map.put(c.name, c);
            }
        } catch(FileNotFoundException ex) {
            Logger.getLogger(CVarList.class.getName()).log(Level.SEVERE, null, ex);
        }
        return map;
    }
    
    public static class CVar {
        
        private String name;

        public void setName(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }
        /**
         *  // null if cmd
         */
        private Object value;

        public void setValue(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }
        private String desc;

        public void setDesc(String desc) {
            this.desc = desc;
        }

        public String getDesc() {
            return desc;
        }
        private ArrayList<String> tags;

        public ArrayList<String> getTags() {
            return tags;
        }

        @Override
        public boolean equals(Object obj) {
            return toString().equals(obj.toString());
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(name).append(" : ").append(value).append(" : ");
            for(String tag : tags) {
                sb.append(", ").append("\"").append(tag).append("\"");
            }
            sb.append(" : ").append(desc);
            return sb.toString();
        }
        
    }

    private static final Logger LOG = Logger.getLogger(CVarList.class.getName());

    private CVarList() {
    }
    
}
