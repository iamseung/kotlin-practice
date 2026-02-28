package com.example.kotlin.kotlin_advanced.generic

import kotlin.reflect.KProperty

class Person{
    lateinit var name: String

    val isKim: Boolean
        get() = name.startsWith("김")

    val maskingName: String
        get() = name[0] + (1 until name.length).joinToString("") { "*" }
}

class Person2 {
    // name과 대응되는, 외부로 드러나지 않는 프로퍼티 > backing property
    private var _name: String? = null
    val name: String
        get() {
            if (_name == null) {
                Thread.sleep(2_000L)
                this._name = "김김"
            }

            return this._name!!
        }

    // by lazy
    val age: Int by lazy {
        Thread.sleep(2_000L)
        19
    }

    private val delegateProperty = LazyInitProperty {
        Thread.sleep(2_000L)
        "김수환무"
    }

    val name2: String
        get() = delegateProperty.getValue(this, ::name2)
}

class LazyInitProperty<T>(val init: () -> T) {
    private var _value: T? = null
    val value: T
        get() {
            if (_value == null) {
                this._value = init()
            }
            return _value!!
        }

    operator fun getValue(thisRef: Any, property: KProperty<*>): T {
        return value
    }
}