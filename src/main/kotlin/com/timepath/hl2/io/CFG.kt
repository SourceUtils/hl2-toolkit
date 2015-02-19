package com.timepath.hl2.io


import java.io.InputStream
import java.util.Deque
import java.util.LinkedList
import java.util.Scanner
import java.util.logging.Level
import java.util.logging.Logger
import java.util.regex.Pattern

/**
 * @author TimePath
 */
public class CFG {
    var aliases: List<Alias> = LinkedList()

    public enum class TokenType private(public val pattern: String) {
        COMMENT : TokenType("//(.*)$")
        SPACE : TokenType("(\\s+)")
        SEPARATOR : TokenType("(;)")
        QUOTE : TokenType("(\\\")")
        TOKEN : TokenType("([^\\s;\\\"]+)")
    }

    public class Token(public var type: TokenType, public var data: String) {

        override fun toString(): String {
            return java.lang.String.format("(%s) %s", type.name(), data)
        }
    }

    public class Alias (
            /**
             * 32 characters
             */
            var name: String,
            var cmd: String
    ) {
        override fun toString(): String {
            return "alias " + name + ' ' + cmd
        }
    }

    class object {

        private val LOG = Logger.getLogger(javaClass<CFG>().getName())

        private fun lex(input: CharSequence): List<Token> {
            val tokens = LinkedList<Token>()
            val tokenPatternsBuffer = StringBuilder()
            val values = TokenType.values()
            for (tokenType in values) {
                tokenPatternsBuffer.append(java.lang.String.format("|%s", tokenType.pattern))
            }
            val tokenPatterns = Pattern.compile(tokenPatternsBuffer.substring(1))
            val matcher = tokenPatterns.matcher(input)
            while (matcher.find()) {
                for (i in values.size().indices) {
                    val type = values[i]
                    val group = matcher.group(i + 1)
                    if (group != null) {
                        tokens.add(Token(type, group))
                        break
                    }
                }
            }
            return tokens
        }

        public fun main(args: Array<String>) {
            readFromString("alias b; alias c alias c alias d alias v \"taunt; nope\"")
        }

        private fun readFromString(s: String): CFG {
            return parse(Scanner(s))
        }

        public fun readFromStream(`in`: InputStream) {
            readFromStream(`in`, "UTF-8")
        }

        private fun readFromStream(`in`: InputStream, encoding: String) {
            var scanner: Scanner? = null
            try {
                scanner = Scanner(`in`, encoding)
                parse(scanner!!)
            } catch (ex: Exception) {
                LOG.log(Level.SEVERE, null, ex)
            } finally {
                if (scanner != null) {
                    scanner!!.close()
                }
            }
        }

        SuppressWarnings("fallthrough")
        private fun parse(scanner: Scanner): CFG {
            val cfg = CFG()
            var lineNum = 0
            while (scanner.hasNext()) {
                val line = scanner.nextLine().trim()
                lineNum++
                val q = lex(line)
                LOG.info("$q")
                val cmd = LinkedList<Token>()
                var quoted = false
                for (t in q) {
                    when (t.type) {
                        CFG.TokenType.QUOTE -> {
                            quoted = !quoted
                            if (!quoted) {
                                LOG.info(eval(cmd))
                                cmd.clear()
                                break
                            }
                            cmd.add(t)
                        }
                        CFG.TokenType.SEPARATOR -> {
                            if (!quoted) {
                                LOG.info(eval(cmd))
                                cmd.clear()
                                break
                            }
                            cmd.add(t)
                        }
                        CFG.TokenType.SPACE, CFG.TokenType.TOKEN -> cmd.add(t)
                        CFG.TokenType.COMMENT -> {
                        }
                        else -> throw AssertionError(t.type.name())
                    }
                }
                LOG.info(eval(cmd))
            }
            return cfg
        }

        private fun eval(deque: Deque<Token>): String {
            val sb = StringBuilder()
            while (!deque.isEmpty()) {
                val t = deque.pop()
                when (t.type) {
                    CFG.TokenType.COMMENT -> {
                    }
                    CFG.TokenType.SPACE -> {
                    }
                    CFG.TokenType.SEPARATOR -> {
                    }
                    CFG.TokenType.QUOTE -> {
                    }
                    CFG.TokenType.TOKEN -> if ("alias" == t.data) {
                        if (!deque.isEmpty() && (deque.peek().type == TokenType.SPACE)) {
                            deque.pop()
                        }
                        val a = Alias(t.data, eval(deque))
                        sb.append(a)
                    } else {
                        sb.append(t.data)
                    }
                    else -> {
                        sb.append(t.data)
                        throw AssertionError(t.type.name())
                    }
                }
            }
            return sb.toString()
        }
    }
}

fun main(args: Array<String>) = CFG.main(args)
