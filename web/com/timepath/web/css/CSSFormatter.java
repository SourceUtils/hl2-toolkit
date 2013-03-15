package com.timepath.web.css;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.logging.Logger;

/**
 *
 * @author timepath
 */
public class CSSFormatter {
    
    public static void main(String... args) throws FileNotFoundException, IOException {
        RandomAccessFile rf = new RandomAccessFile("css.css", "r");
        String input = rf.readLine();
        String text = "";
        for(int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
                
            if(c == '{') {
                text += " " + c + " ";
            } else if(c == '}') {
                text += " " + c;
            } else if(c == ',' || c == ';') {
                text += c + " ";
            } else {
                text += c;
            }
                
            if(c == '}') {
                System.out.println(text);
                text = "";
            }
        }
    }
    private static final Logger LOG = Logger.getLogger(CSSFormatter.class.getName());
    
}
