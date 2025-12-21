package com.example

import com.example.domain.User
import com.thirdparty.Lib

class SampleCoupling {
    fun testCoupling() {
        val user = User("John") // +1 Internal Coupling
        val lib = Lib() // +1 External Coupling
        println(user.name)
    }
}
