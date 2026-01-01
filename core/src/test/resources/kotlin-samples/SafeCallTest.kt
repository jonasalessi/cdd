package com.example

class SafeCallTest {
    fun testSafeCall(fieldErrors: List<String>?) {
        // Safe call only: 0 ICP
        val size = fieldErrors?.size
        
        // Safe call + let (lambda): 1 Branch
        fieldErrors?.let { println(it) } 
        
        // Elvis: 1 Condition
        val x = fieldErrors ?: emptyList<String>()
        
        // Regular Lambda: 1 Branch
        listOf(1, 2, 3).forEach { println(it) }
    }
}
