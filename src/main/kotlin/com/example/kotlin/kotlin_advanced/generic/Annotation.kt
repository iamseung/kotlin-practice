package com.example.kotlin.kotlin_advanced.generic

import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
annotation class Shape(
    val texts: Array<String>
)

@Shape(texts = ["A", "B"])
class Annotation {

}

fun main() {
    val clazz: KClass<Annotation> = Annotation::class
}