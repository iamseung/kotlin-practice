package com.example.kotlin.kotlin_advanced.generic

import java.time.LocalDate

class OperatorOverLoading {

}

data class Point(
    val x: Int,
    val y: Int
) {

    fun zeroPointSymmetry(): Point = Point(-x, -y)

    operator fun unaryMinus(): Point {
        return Point(-x, -y)
    }

    operator fun inc(): Point {
        return Point(x + 1, y)
    }
}

fun main() {
    var point = Point(20, -10)
    println(point.zeroPointSymmetry())
    println(-point)
    println(++point)

    // 2026-03-02
    LocalDate.of(2026,3,2).plusDays(3)
    LocalDate.of(2026,3,2) + Days(3)
    LocalDate.of(2026,3,2) + 3.d

    val mutableListOf = mutableListOf("a", "b", "c")
    mutableListOf += "d"
}

data class Days(val day: Long)

val Int.d: Days
    get() = Days(this.toLong())

operator fun LocalDate.plus(days: Days): LocalDate {
    return this.plusDays(days.day)
}