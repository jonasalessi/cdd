package com.example

class SampleExceptions {
    fun testTryCatch() {
        try {
            val x = 1 / 0
        } catch (e: ArithmeticException) {
            println("Error")
        } finally {
            println("Done")
        }
    }
}
