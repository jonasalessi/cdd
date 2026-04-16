package com.example

class SafeCallTest {
    fun testSafeCall(fieldErrors: List<String>?) {
        val size = fieldErrors?.size
        fieldErrors?.let { println(it) }
        val x = fieldErrors ?: emptyList<String>()
        listOf(1, 2, 3).forEach { println(it) }
    }
}
