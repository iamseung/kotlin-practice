package com.example.kotlin.kotlin_advanced.generic


fun main() {
    var num = 5
    num += 1

    // Java에서는 변경 가능한 값을 람다에 넣기 불가능
    // Kotlin에서는 이러한 코드가 가능한데,
    // 여기서 클로저란 개념은 밖에 있는 변수에 접근하기 위해 일시적으로 밖에 정보들을 포획해 두는 그러한 개념
    val plusOne = { num += 1 }
}
