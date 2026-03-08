package com.example.kotlin.kotlin_advanced.reflection

import org.reflections.Reflections
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.cast

object ContainerV1 {
    // 등록한 클래스 보관, KClass를 보관
    private val registeredClasses = mutableSetOf<KClass<*>>()

    // KClass > Class에 대한 메타데이터
    fun register(clazz: KClass<*>) {
        registeredClasses.add(clazz)
    }

    // Any, null 방지
    fun <T : Any> getInstance(type: KClass<T>): T {
        return registeredClasses.firstOrNull { clazz -> clazz == type }
            ?.let { clazz -> clazz.constructors.first().call() as T }
            ?: throw IllegalArgumentException("해당 인스턴스 타입을 찾을 수 없습니다")
    }
}

fun start(clazz: KClass<*>) {

}
object ContainerV2 {
    // 등록한 클래스 보관, KClass를 보관
    private val registeredClasses = mutableSetOf<KClass<*>>()
    private val cachedInstance = mutableMapOf<KClass<*>, Any>()

    // KClass > Class에 대한 메타데이터
    fun register(clazz: KClass<*>) {
        registeredClasses.add(clazz)
    }

    // Any, null 방지
    fun <T : Any> getInstance(type: KClass<T>): T {
        if (type in cachedInstance) {
            return type.cast(cachedInstance[type])
        }

        val instance = (registeredClasses.firstOrNull { clazz -> clazz == type }
            ?.let { clazz -> instantiate(clazz) as T }
            ?: throw IllegalArgumentException("해당 인스턴스 타입을 찾을 수 없습니다"))
        cachedInstance[type] = instance
        return instance
    }

    private fun <T : Any> instantiate(clazz: KClass<T>): T {
        // 사용 가능한 생성자
        val constructor = findUsableConstructor(clazz)
        // 파라미터를 하나하나 인스턴스화
        val params = constructor.parameters
            .map { parameter -> getInstance(parameter.type.classifier as KClass<*>) }
            .toTypedArray()

        return constructor.call(*params)
    }
    // clazz의 constructor들 중, 사용할 수 있는 constructor
    // constructor에 넣어야 하는 타입들이 모두 등록된 경우 (컨테이너에서 관리하고 있는 경우)
    private fun <T : Any> findUsableConstructor(clazz: KClass<T>): KFunction<T> {
        return clazz.constructors
            .firstOrNull{ constructor -> constructor.parameters.isAllRegistered }
            ?: throw IllegalArgumentException("")
    }

    private val List<KParameter>.isAllRegistered: Boolean
        get() = this.all { it.type.classifier in registeredClasses }
}

fun main() {
//    ContainerV2.register(AService::class)
//    ContainerV2.register(BService::class)
//
//    val bService = ContainerV2.getInstance(BService::class)
//    bService.print()

    val reflections = Reflections("com.example.kotlin.kotlin_advanced.reflection")
    val jClasses = reflections.getTypesAnnotatedWith(MyClass::class.java)
    println(jClasses)
}

annotation class MyClass

@MyClass
class AService {
    fun print() {
        println("A Service")
    }
}

@MyClass
class BService(
    private val aService: AService
) {
    fun print() {
        this.aService.print()
    }
}