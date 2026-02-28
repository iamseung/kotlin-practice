package com.example.kotlin.kotlin_advanced.generic

class Person{
    lateinit var name: String

    val isKim: Boolean
        get() = name.startsWith("김")

    val maskingName: String
        get() = name[0] + (1 until name.length).joinToString("") { "*" }
}

class Person2 {
    // backing property
    private var _name: String? = null
    val name: String
        get() {
            if (_name == null) {
                Thread.sleep(2_000L)
                this._name = "김김"
            }

            return this._name!!
        }

    val age: Int by lazy {
        Thread.sleep(2_000L)
        19
    }
}