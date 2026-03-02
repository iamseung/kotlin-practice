package com.example.kotlin.kotlin_advanced.generic


fun main() {
    // compute(1,2, {a, b -> a + b})

    // 1. 람다식
    // 마지막의 람다는 함수 밖으로 뺄 수 있음.
    compute(1,2) { a, b -> a + b }

    // 2. 익명함수
    compute(1,2, fun(a: Int, b: Int) = a + b)
    compute(1,2, fun(a: Int, b: Int): Int {
        return a + b
    })

    iterate(listOf(1,2,3,4,5), fun(num) {
        if (num == 3) {
            return
        }

        print(num)
    })

    iterate(listOf(1,2,3,4,5)) { num ->
        if (num != 3) {
            print(num)
        }
    }
}

fun compute(num1: Int, num2: Int, op: (Int, Int) -> Int): Int {
    return op(num1, num2)
}

fun iterate(numbers: List<Int>, exec: (Int) -> Unit) {
    for(number in numbers) {
        exec(number)
    }
}

fun calculate(num1: Int, num2: Int, oper: Operator) = oper.calcFunc(num1, num2)

enum class Operator(
    private val oper: Char,
    val calcFunc: (Int, Int) -> Int
)  {
    PLUS('+', { a, b -> a + b }),
    MINUS('-', { a, b -> a - b }),
    MULTIPLY('*', { a, b -> a * b }),
    DIVIDE('/', { a, b ->
        if (b == 0) {
            throw IllegalArgumentException("0으로 나눌 수 없습니다.")
        } else {
            a - b
        }
    }),
}