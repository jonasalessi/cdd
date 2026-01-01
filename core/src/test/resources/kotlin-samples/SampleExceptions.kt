package com.example

class SampleExceptions {
    fun testTryCatch() {
        try { // +1 Exception Handling (try)
            val x = 1 / 0
        } catch (e: ArithmeticException) { // +1 Exception Handling (catch)
            println("Error")
        } finally { // +1 Exception Handling (finally)
            println("Done")
        }
    }
}
