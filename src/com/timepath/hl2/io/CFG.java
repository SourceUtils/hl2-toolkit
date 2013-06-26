package com.timepath.hl2.io;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class CFG {

    private static final Logger LOG = Logger.getLogger(CFG.class.getName());

    ArrayList<Alias> aliases = new ArrayList<Alias>();

    public class Alias {

        /**
         * 32 characters
         */
        String name;

        String cmd;

        public Alias() {
        }

    }

    private static void parse(Scanner s) {
        int lineNum = 0;
        while(s.hasNext()) {
            String line = s.nextLine().trim();
            char[] pText = line.toCharArray();
            lineNum++;
            if(pText.length == 0) {
                continue;
            }

            int offset = 0;
            while(offset < pText.length) {
                int commandLength = getCommand(pText, offset);
                if(commandLength == 0) {
                    offset++;
                    continue;
                }
                System.out.println(
                        new String(pText).substring(offset, offset + commandLength).trim());
                offset += commandLength;
            }

        }
    }

    static int getCommand(char[] pText, int offset) {
        int maxLen = pText.length - offset;
        boolean quoted = false;
        boolean commented = false;
        int i;
        for(i = 0; i < maxLen; i++) {
            char c = pText[offset + i];
            if(commented) {
                continue;
            } else if(c == '"') {
                quoted = !quoted;
                continue;
            }
            if(quoted) {
                continue;
            } else if(c == '/') {
                commented = (i + 1 < maxLen) && pText[offset + i + 1] == '/';
                if(commented) {
                    break;
                }
            }
            if(c == ';') {
                break;
            }
        }

        return i;
    }

    public static void main(String[] args) {
        readFromString(
                "alias one \"alias three four\"; alias two // yay\nalias b; alias c alias c alias d alias v taunt");
    }

    public static void readFromString(String s) {
        parse(new Scanner(s));
    }

    public static void readFromStream(InputStream in) {
        readFromStream(in, "UTF-8");
    }

    public static void readFromStream(InputStream in, String encoding) {
        Scanner s = null;
        try {
            s = new Scanner(in, encoding);
            parse(s);
        } catch(Exception ex) {
            ex.printStackTrace();
        } finally {
            if(s != null) {
                s.close();
            }
        }
    }

}
