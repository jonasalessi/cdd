package com.example

class SampleControlFlow {
    fun testLoops(n: Int) {
        for (i in 0 until n) {
            println(i)
        }

        var j = 0
        while (j < 10) {
            println(j)
            j++
        }
    }

    fun testIfElse(x: Int) {
        if (x > 10) {
            println("Large")
        } else if (x > 5) {
            println("Medium")
        } else {
            println("Small")
        }
    }
}
