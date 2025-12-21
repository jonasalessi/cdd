package com.example

class SampleConstructs {
    fun testWhen(x: Int) {
        when (x) { // +1 Branch
            1 -> println("One")
            2 -> println("Two")
            else -> println("Other") // +1 Branch (else)
        }
    }

    fun testElvis(s: String?) {
        val length = s?.length ?: 0 // +1 Branch (Safe call), +1 Branch (Elvis)
        println(length)
    }

    fun testConditions(x: Int, y: Int) {
        if (x > 0 && y > 0 || x < -10) { // +1 Branch (if), +3 Conditions (&&, ||, < -10)
            println("Complex")
        }
    }
}
