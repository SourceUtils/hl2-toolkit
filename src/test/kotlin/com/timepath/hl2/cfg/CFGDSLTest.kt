package com.timepath.hl2.cfg

object CFGDSLTest {
    public kotlin.platform.platformStatic fun main(args: Array<String>) {
        CFGDSL {
            val list = cyclicList("li", 3) { i ->
                echo("${i}")
            }
            val pressedA = latch()
            bind("a", press = {
                pressedA(true)
                eval(list.exec)
                eval(list.next)
                echo("pressed ${it}")
            }, release = {
                pressedA(false)
            })
            eval("")
            echo("hello world")
            eval("")
            val btn1 = alias("bpr") {
                pressedA(true) {
                    echo("A is pressed")
                }
                pressedA(false) {
                    echo("A is not pressed")
                }
                echo("pressed")
                echo("again")
            }
            eval("")
            eval(alias {
                eval(btn1)
            })
        }.let {
            println(it.print())
        }
    }
}
