package com.example

import com.example.domain.*

class SampleCoupling {
    fun testCoupling() {
        val user = User("John") // +1 Internal Coupling
        InternalClass().hello() // +1 Internal Coupling
        House().hello() // +1 Internal Coupling
        println(user.name)
    }
}


internal class InternalClass {
    fun hello() {

    }
}