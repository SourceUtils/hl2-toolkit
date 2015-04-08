package com.timepath.hl2.cfg

private object NameGen {
    private val m = linkedMapOf<String, Int>()
    fun get(prefix: String) = prefix + m.let {
        val r = it.getOrPut(prefix, { 0 })
        it[prefix] = it[prefix]!! + 1
        r
    }
}

abstract class CFGContext(val children: MutableList<CFGContext> = arrayListOf()) {
    abstract fun print(): String
}

class CFGDSL(configure: CFGDSL.() -> Unit) : CFGContext() {
    override fun print() = children.map { it.print() }.join("\n")

    init {
        configure()
    }
}

class Command(val cmd: String, vararg val args: String) : CFGContext() {
    override fun print() = when {
        args.isEmpty() -> "${cmd}"
        else -> "${cmd} ${args.joinToString(" ")}"
    }
}

fun CFGContext.cmd(cmd: String, vararg args: String) = children.add(Command(cmd, *args))

fun CFGContext.echo(text: String) = cmd("echo", text)

class Alias(val name: String, children: MutableList<CFGContext> = arrayListOf()) : CFGContext(children) {
    override fun print(): String = when {
        children.isEmpty() -> "alias ${name}"
        children.size() == 1 -> "alias ${name} ${children.single().print()}"
        else -> {
            val cmds = children.map { Alias(NameGen["_t"], arrayListOf(it)) }
            StringBuilder {
                append("alias ${name} \"${cmds.map { it.name }.join("; ")}\"")
                cmds.forEach { append("\n${it.print()}") }
            }.toString()
        }
    }
}

fun CFGContext.alias(name: String = NameGen["_a"], configure: Alias.() -> Unit) = Alias(name).let {
    it.configure()
    children.add(it)
    it
}

fun CFGContext.cmd(alias: Alias) = children.add(Command(alias.name))

data class Bind(val key: String)

fun CFGContext.bind(key: String, press: Alias.(Bind) -> Unit) = bind(key, press, {})
fun CFGContext.bind(key: String, press: Alias.(Bind) -> Unit, release: Alias.(Bind) -> Unit) = NameGen["_b"].let {
    val bind = Bind(key)
    cmd("bind", key, "+$it")
    alias("+$it", { press(bind) })
    alias("-$it", { release(bind) })
    bind
}

data class CyclicList(val exec: String, val next: String, val prev: String)

fun CFGContext.cyclicList(name: String, size: Int, begin: Int = 0, configure: Alias.(Int) -> Unit): CyclicList {
    val exec = "*${name}"
    val next = "${name}++"
    val prev = "${name}--"
    val list = CyclicList(exec, next, prev)
    fun idx(i: Int) = (size + i) % size
    fun action(i: Int) = "${name}[${idx(i)}]"
    fun iter(i: Int) = "${name}.i[${idx(i)}]"
    // Loop twice for nicer output
    for (i in size.indices) {
        alias(action(i)) { configure(i) }
    }
    for (i in size.indices) {
        alias(iter(i)) {
            alias(exec) { cmd(action(i)) }
            alias(next) { cmd(iter(i + 1)) }
            alias(prev) { cmd(iter(i - 1)) }
        }
    }
    cmd(iter(begin)) // Initialize
    return list
}
