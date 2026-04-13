package com.example

class SampleConstructs {
    fun testWhen(x: Int) {
        when (x) {
            1 -> println("One")
            2 -> println("Two")
            else -> println("Other")
        }
    }

    fun testElvis(s: String?) {
        val length = s?.length ?: 0
        println(length)
    }

    fun testConditions(x: Int, y: Int) {
        if (x > 0 && y > 0 || x < -10) {
            println("Complex")
        }
    }
}
