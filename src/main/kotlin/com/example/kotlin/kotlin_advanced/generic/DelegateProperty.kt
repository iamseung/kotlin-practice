package com.example.kotlin.kotlin_advanced.generic

import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

class Person5 {
    val name: String by LazyInitPropertyV2 {
        Thread.sleep(2_000)
        "김김ㄱ믹미"
    }
}

// ReadOnlyProperty<위임 객체 클래스, property 타입>
// LazyInitPropertyV2 는 T 타입을 들고 있으니 T로 지정
class LazyInitPropertyV2<T>(val init: () -> T): ReadOnlyProperty<Any, T> {
    private var _value: T? = null
    val value: T
    get() {
        if (_value == null) {
            this._value = init()
        }
        return _value!!
    }

    override fun getValue(thisRef: Any, property: KProperty<*>): T {
        return value
    }
}

fun main() {
    Person6()
}

class Person6 {
    val name by DelegateProvider("이승석") // 정상동작 O
    val country by DelegateProvider("한국") // 정상동작 X
}

class DelegateProvider(
    private val initValue: String
) {
    operator fun provideDelegate(thisRef: Any, property: KProperty<*>): DelegateProperty {
        // 위임 프로퍼티의 진짜 이름 (name, country 등)
        val propertyName = property.name
        if (propertyName != "name") {
            throw IllegalArgumentException("name만 연결 가능, current propertyName : $propertyName")
        }

        return DelegateProperty(initValue)
    }
}

// PropertyDelegateProvider<Any, {우리가 반환할 위임 객체 타입}>
class DelegateProviderV2(
    private val initValue: String
) : PropertyDelegateProvider<Any, DelegateProperty> {
    override fun provideDelegate(
        thisRef: Any,
        property: KProperty<*>
    ): DelegateProperty {
        // 위임 프로퍼티의 진짜 이름 (name, country 등)
        val propertyName = property.name
        if (propertyName != "name") {
            throw IllegalArgumentException("name만 연결 가능, current propertyName : $propertyName")
        }

        return DelegateProperty(initValue)
    }
}

class DelegateProperty(
    private val initValue: String,
) : ReadOnlyProperty<Any, String> {
    override fun getValue(thisRef: Any, property: KProperty<*>): String {
        return initValue
    }
}
