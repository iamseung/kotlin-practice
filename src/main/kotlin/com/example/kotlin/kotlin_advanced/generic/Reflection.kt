package com.example.kotlin.kotlin_advanced.generic

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType
import kotlin.reflect.full.cast
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.createType
import kotlin.reflect.full.hasAnnotation

@Executable
class Reflection {

    fun a() {
        println("A입니다.")
    }

    fun b(n: Int) {
        println("B입니다.")
    }
}

@Target(AnnotationTarget.CLASS)
annotation class Executable

fun executeAll(obj: Any) {
    val kClass = obj::class
    if (!kClass.hasAnnotation<Executable>()) {
        return
    }

    val callableFunctions = kClass.members.filterIsInstance<KFunction<*>>()
        .filter { it.returnType == Unit::class.createType() }
        .filter { it.parameters.size == 1 && it.parameters[0].type == kClass.createType() }

    callableFunctions.forEach { function ->
//        function.call(kClass.createInstance())
        function.call(obj)
    }
}

fun add(a: Int, b: Int) = a + b
fun main() {
    // kClass를 얻는 3가지 방법
    val kClass: KClass<Reflection> = Reflection::class

    // 함수의 변수화, KCallble (KFunction)
    val callable = ::add

    val ref = Reflection()
    val kClass2: KClass<out Reflection> = ref::class // 공변을 만들어줘야 해서 out 키워드 사용

    // 문자열이 진짜 Reflection인지 모르기 때문에 Any의 하위타입으로 가져왔다가 변형해야 함.
    val kClass3: KClass<out Any> = Class.forName("com.example.kotlin.kotlin_advanced.generic.Reflection").kotlin

    kClass.java // class<Reflection>
    kClass.java.kotlin // kClass<Reflection>

    // kType, 타입을 표현
    val kType: KType = Hyundai::class.createType()

    val hyundai = Hyundai(5000)
    // print 라는 함수가 멤버함수 이기 때문에 Hyundai.print()와 같이 표기할 수 있음.
    // 그렇기에 call 에 hyundai를 전달
    hyundai::class.members.first{ it.name == "print" }.call(hyundai)

    executeAll(Reflection())
}

class Hyundai(val price: Int) {
    fun print() {
        println("현대 가면 제 연봉은 ${price}입니다.")
    }
}

fun castToHyundai(obj: Any): Hyundai {
    // return obj as Hyundai
    return Hyundai::class.cast(obj) // 클래스::class -> kClass
}

