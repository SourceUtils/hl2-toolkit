package com.timepath.hl2.io;

import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author TimePath
 */
public class CFG {

    private static final Logger LOG = Logger.getLogger(CFG.class.getName());

    public static enum TokenType {

        COMMENT("//(.*)$"),
        SPACE("(\\s+)"),
        SEPARATOR("(;)"),
        QUOTE("(\\\")"),
        TOKEN("([^\\s;\\\"]+)");

        public final String pattern;

        private TokenType(String pattern) {
            this.pattern = pattern;
        }

    }

    public static class Token {

        public TokenType type;

        public String data;

        public Token(TokenType type, String data) {
            this.type = type;
            this.data = data;
        }

        @Override
        public String toString() {
            return String.format("(%s) %s", type.name(), data);
        }

    }

    private static List<Token> lex(String input) {
        List<Token> tokens = new LinkedList<Token>();

        StringBuilder tokenPatternsBuffer = new StringBuilder();
        TokenType[] values = TokenType.values();
        for(TokenType tokenType : values) {
            tokenPatternsBuffer.append(String.format("|%s", tokenType.pattern));
        }
        Pattern tokenPatterns = Pattern.compile(tokenPatternsBuffer.substring(1));
        Matcher matcher = tokenPatterns.matcher(input);
        while(matcher.find()) {
            for(int i = 0; i < values.length; i++) {
                TokenType type = values[i];
                String group = matcher.group(i + 1);
                if(group != null) {
                    tokens.add(new Token(type, group));
                    break;
                }
            }
        }
        return tokens;
    }

    public static void main(String[] args) {
        readFromString("alias b; alias c alias c alias d alias v \"taunt; nope\"");
    }

    public static CFG readFromString(String s) {
        return CFG.parse(new Scanner(s));
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
            LOG.log(Level.SEVERE, null, ex);
        } finally {
            if(s != null) {
                s.close();
            }
        }
    }

    @SuppressWarnings("fallthrough")
    private static CFG parse(Scanner s) {
        CFG c = new CFG();
        int lineNum = 0;
        while(s.hasNext()) {
            String line = s.nextLine().trim();
            lineNum++;
            List<Token> q = lex(line);
            System.out.println(q);
            Deque<Token> cmd = new LinkedList<Token>();
            boolean quoted = false;
            for(Token t : q) {
                switch(t.type) {
                    case QUOTE:
                        quoted = !quoted;
                    case SEPARATOR:
                        if(!quoted) {
                            System.out.println(c.eval(cmd));
                            cmd.clear();
                            break;
                        }
                    case SPACE:
                    case TOKEN:
                        cmd.add(t);
                        break;
                    case COMMENT:
                        break;
                    default:
                        throw new AssertionError(t.type.name());
                }
            }
            System.out.println(c.eval(cmd));
        }
        return c;
    }

    private String eval(Deque<Token> l) {
        StringBuilder sb = new StringBuilder();
        while(!l.isEmpty()) {
            Token t = l.pop();
            switch(t.type) {
                case COMMENT:
                    break;
                case SPACE:
                    break;
                case SEPARATOR:
                    break;
                case QUOTE:
                    break;
                case TOKEN:
                    if(t.data.equals("alias")) {
                        Alias a = new Alias();
                        a.name = t.data;
                        if(!l.isEmpty() && l.peek().type == TokenType.SPACE) {
                            l.pop();
                        }
                        a.cmd = eval(l);
                        sb.append(a.toString());
                    } else {
                        sb.append(t.data);
                    }
                    break;
                default:
                    sb.append(t.data);
                    throw new AssertionError(t.type.name());
            }
        }
        return sb.toString();
    }

    List<Alias> aliases = new LinkedList<Alias>();

    public class Alias {

        /**
         * 32 characters
         */
        String name;

        String cmd;

        public Alias() {
        }

        @Override
        public String toString() {
            return "alias " + name + " " + cmd;
        }

    }

}
