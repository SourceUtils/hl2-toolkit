package com.timepath.hl2.io;

import java.io.InputStream;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author TimePath
 */
public class CFG {

    private static final Logger LOG = Logger.getLogger(CFG.class.getName());
    List<Alias> aliases = new LinkedList<>();

    public CFG() {}

    private static List<Token> lex(CharSequence input) {
        List<Token> tokens = new LinkedList<>();
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

    public static void main(String... args) {
        readFromString("alias b; alias c alias c alias d alias v \"taunt; nope\"");
    }

    private static CFG readFromString(String s) {
        return parse(new Scanner(s));
    }

    public static void readFromStream(InputStream in) {
        readFromStream(in, "UTF-8");
    }

    private static void readFromStream(InputStream in, String encoding) {
        Scanner scanner = null;
        try {
            scanner = new Scanner(in, encoding);
            parse(scanner);
        } catch(Exception ex) {
            LOG.log(Level.SEVERE, null, ex);
        } finally {
            if(scanner != null) {
                scanner.close();
            }
        }
    }

    @SuppressWarnings("fallthrough")
    private static CFG parse(Scanner scanner) {
        CFG cfg = new CFG();
        int lineNum = 0;
        while(scanner.hasNext()) {
            String line = scanner.nextLine().trim();
            lineNum++;
            List<Token> q = lex(line);
            LOG.info(String.valueOf(q));
            Deque<Token> cmd = new LinkedList<>();
            boolean quoted = false;
            for(Token t : q) {
                switch(t.type) {
                    case QUOTE:
                        quoted = !quoted;
                    case SEPARATOR:
                        if(!quoted) {
                            LOG.info(eval(cmd));
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
            LOG.info(eval(cmd));
        }
        return cfg;
    }

    private static String eval(Deque<Token> deque) {
        StringBuilder sb = new StringBuilder();
        while(!deque.isEmpty()) {
            Token t = deque.pop();
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
                    if("alias".equals(t.data)) {
                        Alias a = new Alias();
                        a.name = t.data;
                        if(!deque.isEmpty() && ( deque.peek().type == TokenType.SPACE )) {
                            deque.pop();
                        }
                        a.cmd = eval(deque);
                        sb.append(a);
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
        public String    data;

        public Token(TokenType type, String data) {
            this.type = type;
            this.data = data;
        }

        @Override
        public String toString() {
            return String.format("(%s) %s", type.name(), data);
        }
    }

    public static class Alias {

        /**
         * 32 characters
         */
        String name;
        String cmd;

        public Alias() {}

        @Override
        public String toString() {
            return "alias " + name + ' ' + cmd;
        }
    }
}
