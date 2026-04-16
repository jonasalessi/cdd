package com.example

import com.example.domain.*

class SampleCoupling {
    fun testCoupling() {
        val user = User("John")
        InternalClass().hello()
        House().hello()
        println(user.name)
    }
}

internal class InternalClass {
    fun hello() {
    }
}
