package com.example

class OuterClass {
    fun outerMethod(flag: Boolean) {
        if (flag) {
            println("outer")
        }
    }

    class InnerClass {
        fun innerMethod(value: Int) {
            if (value > 0) {
                println("inner")
            }
        }
    }
}
