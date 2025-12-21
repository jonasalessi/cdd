package com.example

class SampleControlFlow {
    fun testLoops(n: Int) {
        for (i in 0 until n) { // +1 Branch (for)
            println(i)
        }

        var j = 0
        while (j < 10) { // +1 Branch (while), +1 Condition
            println(j)
            j++
        }
    }

    fun testIfElse(x: Int) {
        if (x > 10) { // +1 Branch (if), +1 Condition
            println("Large")
        } else if (x > 5) { // +1 Branch (else if), +1 Condition
            println("Medium")
        } else { // +1 Branch (else)
            println("Small")
        }
    }
}
