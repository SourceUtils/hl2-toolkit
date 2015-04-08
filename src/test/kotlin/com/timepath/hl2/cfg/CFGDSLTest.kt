package com.timepath.hl2.cfg

object CFGDSLTest {
    public kotlin.platform.platformStatic fun main(args: Array<String>) {
        CFGDSL {
            val list = cyclicList("li", 3) { i ->
                cmd("echo ${i}")
            }
            bind("a") {
                cmd(list.exec)
                cmd(list.next)
                echo("pressed ${it}")
            }
            cmd("")
            echo("hello world")
            cmd("")
            val btn1 = alias("bpr") {
                echo("pressed")
                echo("again")
            }
            cmd("")
            cmd(alias {
                cmd(btn1)
            })
        }.let {
            println(it.print())
        }
    }
}
